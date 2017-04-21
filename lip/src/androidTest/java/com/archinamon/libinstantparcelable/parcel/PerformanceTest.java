package com.archinamon.libinstantparcelable.parcel;

import android.os.Bundle;
import android.os.Parcel;
import android.util.TimingLogger;
import com.archinamon.libinstantparcelable.parcel.adapter.IParcelTypeAdapter;
import junit.framework.TestCase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE;
import static com.archinamon.libinstantparcelable.parcel.ParcelableUtils.parcelable;
import static com.archinamon.libinstantparcelable.parcel.ParcelableUtils.parcelables;

/**
 * TODO: Add destription
 *
 * @author ironbcc on 22.04.16.
 */
public class PerformanceTest
    extends TestCase {

    public static final String TEST_STR = "Written inside JxMimiClass::beforeIdWrite from ParcelableProcessor.aj with BEFORE mode";

    public static class MimiClass
        implements Serializable {

        enum Type {
            FIRST, SECOND, THIRD
        }

        public int id;
        public transient String name;
        public List<String> params;
        public Type type;
        public Date createDate;

        public int[] intArr;

        public boolean isCool;
    }

    @Parcelable(describeContents = PARCELABLE_WRITE_RETURN_VALUE)
    public static class JxMimiClass
        extends MimiClass {

        @Parcelable.Write(field = "id", mode = Parcelable.Mode.BEFORE)
        void beforeIdWrite(Parcel dest) {
            dest.writeString(TEST_STR);
        }

        @Parcelable.Read(field = "id", mode = Parcelable.Mode.BEFORE)
        void beforeIdRead(Parcel in) {
            assertEquals(TEST_STR, in.readString());
        }
    }

    public static final class DateTypeAdapter
        implements IParcelTypeAdapter<Date, Long> {

        @Override
        public Long adapt(Date obj) {
            return obj.getTime();
        }

        @Override
        public Date wiseVersa(Long adapted) {
            return new Date(adapted);
        }
    }

    public static final class EnumTypeAdapter
        implements IParcelTypeAdapter<MimiClass.Type, Integer> {

        @Override
        public Integer adapt(MimiClass.Type obj) {
            return obj.ordinal();
        }

        @Override
        public MimiClass.Type wiseVersa(Integer adapted) {
            return MimiClass.Type.values()[adapted];
        }
    }

    @Parcelable(adapters = {DateTypeAdapter.class, EnumTypeAdapter.class})
    private static class Jx2MimiClass
        extends JxMimiClass {

        float coef;
    }

    private static class SMimiClass
        extends MimiClass
        implements Serializable {}

    private static class CommonMimiClass
        extends MimiClass
        implements android.os.Parcelable {

        @Override
        public int describeContents() { return 0; }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.id);
            dest.writeString(this.name);
            dest.writeStringList(this.params);
            dest.writeLong(createDate != null ? createDate.getTime() : -1);
            dest.writeInt(this.type == null ? -1 : this.type.ordinal());
            dest.writeIntArray(this.intArr);
            dest.writeByte(isCool ? (byte) 1 : (byte) 0);
        }

        CommonMimiClass() {}

        CommonMimiClass(Parcel in) {
            this.id = in.readInt();
            this.name = in.readString();
            this.params = in.createStringArrayList();
            long tmpCreateDate = in.readLong();
            this.createDate = tmpCreateDate == -1 ? null : new Date(tmpCreateDate);
            int tmpType = in.readInt();
            this.type = tmpType == -1 ? null : Type.values()[tmpType];
            this.intArr = in.createIntArray();
            this.isCool = in.readByte() != 0;
        }

        public static final Creator<CommonMimiClass> CREATOR = new Creator<CommonMimiClass>() {
            @Override
            public CommonMimiClass createFromParcel(Parcel source) {return new CommonMimiClass(source);}

            @Override
            public CommonMimiClass[] newArray(int size) {return new CommonMimiClass[size];}
        };
    }

    public void testSingle() {
        final TimingLogger logger = new TimingLogger("PARCELABLE", "single");
        final MimiClass mimiClass = generateJx();
        final MimiClass jx2Class = generateJx2();
        final CommonMimiClass common = generateCommon();
        logger.addSplit("creation");

        Bundle bundle = new Bundle();
        Parcel parcel = Parcel.obtain();
        bundle.putParcelable("a", parcelable(mimiClass));
        bundle.writeToParcel(parcel, PARCELABLE_WRITE_RETURN_VALUE);
        parcel.setDataPosition(0);
        bundle.readFromParcel(parcel);
        bundle.setClassLoader(JxMimiClass.class.getClassLoader());
        JxMimiClass aa = bundle.getParcelable("a");
        assertNotNull(aa);
        assertNull(aa.name);
        assertEquals(MimiClass.Type.FIRST, aa.type);
        assertEquals(2, aa.intArr[1]);
        parcel.recycle();
        logger.addSplit("jx finished");

        bundle = new Bundle();
        parcel = Parcel.obtain();
        bundle.putParcelable("b", parcelable(jx2Class));
        bundle.writeToParcel(parcel, PARCELABLE_WRITE_RETURN_VALUE);
        parcel.setDataPosition(0);
        bundle.readFromParcel(parcel);
        bundle.setClassLoader(Jx2MimiClass.class.getClassLoader());
        Jx2MimiClass ab = bundle.getParcelable("b");
        assertNotNull(ab);
        assertEquals(.3f, ab.coef);
        assertEquals(2, ab.intArr[1]);
        parcel.recycle();
        logger.addSplit("jx2 finished");

        bundle = new Bundle();
        parcel = Parcel.obtain();
        bundle.putParcelable("c", common);
        bundle.writeToParcel(parcel, PARCELABLE_WRITE_RETURN_VALUE);
        parcel.setDataPosition(0);
        bundle.readFromParcel(parcel);
        bundle.setClassLoader(CommonMimiClass.class.getClassLoader());
        CommonMimiClass ac = bundle.getParcelable("c");
        assertNotNull(ac);
        assertEquals(MimiClass.Type.SECOND, ac.type);
        assertEquals(2, ac.intArr[1]);
        parcel.recycle();
        logger.addSplit("common finished");
        logger.dumpToLog();
    }

    private static final int COUNT = 10000;

    public void testHeavy() {
        Bundle bundle;Parcel parcel;
        final TimingLogger logger = new TimingLogger("PARCELABLE", "heavy");

        final CommonMimiClass[] commonMimiClasses = new CommonMimiClass[COUNT];
        for (int i = 0; i < COUNT; i++) {
            commonMimiClasses[i] = generateCommon();
        }
        logger.addSplit("common creation");

        bundle = new Bundle();
        parcel = Parcel.obtain();
        bundle.putParcelableArray("a", commonMimiClasses);
        bundle.writeToParcel(parcel, PARCELABLE_WRITE_RETURN_VALUE);
        logger.addSplit("common write");
        parcel.setDataPosition(0);

        bundle.readFromParcel(parcel);
        bundle.setClassLoader(CommonMimiClass.class.getClassLoader());
        android.os.Parcelable[] dataCommon = bundle.getParcelableArray("a");
        assert dataCommon != null;
        assertEquals(commonMimiClasses.length, dataCommon.length);
        assertEquals(MimiClass.Type.SECOND, ((MimiClass)dataCommon[0]).type);
        logger.addSplit("common finished");
        parcel.recycle();

        final JxMimiClass[] mimiClasses = new JxMimiClass[COUNT];
        for (int i = 0; i < COUNT; i++) {
            mimiClasses[i] = generateJx();
        }

        logger.addSplit("jx creation");

        bundle = new Bundle();
        parcel = Parcel.obtain();
        bundle.putParcelableArray("a", parcelables(mimiClasses));
        bundle.writeToParcel(parcel, PARCELABLE_WRITE_RETURN_VALUE);

        logger.addSplit("jx write");

        parcel.setDataPosition(0);

        bundle.readFromParcel(parcel);
        bundle.setClassLoader(JxMimiClass.class.getClassLoader());
        android.os.Parcelable[] data = bundle.getParcelableArray("a");
        assert data != null;
        assertEquals(mimiClasses.length, data.length);
        assertEquals(MimiClass.Type.FIRST, ((MimiClass)data[0]).type);

        parcel.recycle();
        logger.addSplit("jx finished");

        final Jx2MimiClass[] jx2MimiClasses = new Jx2MimiClass[COUNT];
        for (int i = 0; i < COUNT; i++) {
            jx2MimiClasses[i] = generateJx2();
        }
        logger.addSplit("jx2 creation");

        bundle = new Bundle();
        parcel = Parcel.obtain();
        bundle.putParcelableArray("a", parcelables(jx2MimiClasses));
        bundle.writeToParcel(parcel, PARCELABLE_WRITE_RETURN_VALUE);

        logger.addSplit("jx2 write");
        parcel.setDataPosition(0);

        bundle.readFromParcel(parcel);
        bundle.setClassLoader(Jx2MimiClass.class.getClassLoader());
        android.os.Parcelable[] data2 = bundle.getParcelableArray("a");
        assert data2 != null;
        assertEquals(jx2MimiClasses.length, data2.length);
        assertEquals(MimiClass.Type.FIRST, ((MimiClass)data2[0]).type);
        parcel.recycle();

        logger.addSplit("jx2 finished");

        final ArrayList<SMimiClass> sMimiClasses = new ArrayList<>(COUNT);
        for (int i = 0; i < COUNT; i++) {
            sMimiClasses.add(generateSerializable());
        }
        logger.addSplit("serializable creation");

        bundle = new Bundle();
        parcel = Parcel.obtain();
        bundle.putSerializable("a", sMimiClasses);
        bundle.writeToParcel(parcel, PARCELABLE_WRITE_RETURN_VALUE);

        logger.addSplit("serializable write");
        parcel.setDataPosition(0);

        bundle.readFromParcel(parcel);
        bundle.setClassLoader(SMimiClass.class.getClassLoader());

        @SuppressWarnings("unchecked")
        ArrayList<SMimiClass> dataSerializable = (ArrayList<SMimiClass>) bundle.getSerializable("a");
        assert dataSerializable != null;
        assertEquals(sMimiClasses.size(), dataSerializable.size());
        assertEquals(MimiClass.Type.THIRD, dataSerializable.get(0).type);
        parcel.recycle();

        logger.addSplit("serializable finished");
        logger.dumpToLog();
    }

    private JxMimiClass generateJx() {
        final JxMimiClass jxMimiClass = new JxMimiClass();
        fill(jxMimiClass);
        jxMimiClass.type = MimiClass.Type.FIRST;
        jxMimiClass.name = "some name";
        return jxMimiClass;
    }

    private Jx2MimiClass generateJx2() {
        final Jx2MimiClass jx2MimiClass = new Jx2MimiClass();
        fill(jx2MimiClass);
        jx2MimiClass.type = MimiClass.Type.FIRST;
        jx2MimiClass.name = "some name";
        jx2MimiClass.coef = .3f;
        return jx2MimiClass;
    }

    private CommonMimiClass generateCommon() {
        final CommonMimiClass commonMimiClass = new CommonMimiClass();
        fill(commonMimiClass);
        commonMimiClass.type = MimiClass.Type.SECOND;
        return commonMimiClass;
    }

    private SMimiClass generateSerializable() {
        final SMimiClass sMimiClass = new SMimiClass();
        fill(sMimiClass);
        sMimiClass.type = MimiClass.Type.THIRD;
        return sMimiClass;
    }

    private MimiClass fill(MimiClass value) {
        value.id = (int) (Math.random() * 1000);
        value.createDate = new Date();
        value.intArr = new int[] {1, 2, 3};
        value.params = new ArrayList<>(2);
        value.params.add("BlaBla");
        value.params.add("BlaBlaBla");
        value.type = MimiClass.Type.FIRST;
        value.isCool = true;
        return value;
    }
}