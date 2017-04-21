package com.archinamon.libinstantparcelable.parcel.engine;

import android.os.Parcel;
import com.annimon.stream.function.Consumer;
import com.archinamon.libinstantparcelable.parcel.Parcelable;
import com.archinamon.libinstantparcelable.parcel.adapter.IParcelTypeAdapter;
import com.archinamon.libinstantparcelable.parcel.errors.ClassClashException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;

import static com.archinamon.libinstantparcelable.parcel.ParcelableUtils.isEmpty;

/**
 * Created by archinamon on 26/02/16.
 */
privileged aspect ParcelableProcessor pertypewithin(@Parcelable *) {

    declare parents: @Parcelable * implements AjcIParcelableBridge;

    pointcut initFields(Parcelable parcelable):
        @within(parcelable) &&
        staticinitialization(*);

    public void AjcIParcelableBridge.readFromParcel(Parcel in) {
        final ParcelProxy proxy = ParcelProxy.with(in, this);
        proceedObjectWithConsumer(this, proxy, ExtPropertyMode.READ, Utils.getReadMethod(proxy));
    }

    public void AjcIParcelableBridge.writeToParcel(Parcel dest, int flags) {
        dest.writeString(aspectFor(this).thisClassName);

        final ParcelProxy proxy = ParcelProxy.with(dest, this);
        proceedObjectWithConsumer(this, proxy, ExtPropertyMode.WRITE, Utils.getWriteMethod(proxy));
    }

    public int AjcIParcelableBridge.describeContents() {
        return aspectFor(this).describeContents;
    }

    /*
     * The meta-programming stage is above
     * Below a direct type-injector and private API
     */

    enum ExtPropertyMode {

        READ,
        WRITE
    }

    private transient Class<?> thisClass;
    private transient String thisClassName;
    private volatile Field[] thisClassFields;

    private int describeContents = 0;
    private HashMap<String, Method> readMethods = new HashMap<>();
    private HashMap<String, Method> writeMethods = new HashMap<>();

    before(Parcelable parcelable): initFields(parcelable) {
        thisClass = thisJoinPointStaticPart.getSourceLocation().getWithinType();

        if (!isEmpty(thisClassName)) throw new ClassClashException();

        if (!hasFields()) { // well, we shall never access this jp twice
            thisClassName = thisClass.getName();
            thisClassFields = Utils.extractFields(thisClass);
            describeContents = parcelable.describeContents();

            Utils.extractReadMethods(readMethods, thisClass);
            Utils.extractWriteMethods(writeMethods, thisClass);

            if (!isEmpty(parcelable.adapters())) {
                Set<IParcelTypeAdapter<?,?>> adapters = Utils.registerParcelableTypeAdapters(parcelable.adapters());
                for (IParcelTypeAdapter<?,?> adapter : adapters) {
                    //noinspection unchecked
                    adapter.thisLinkedWith = (Class<? extends AjcIParcelableBridge>) thisClass;
                }
            }

            Constructor<?> thisClassConstructor = Utils.defaultConstructor(thisClass);
            Utils.registerType(thisClassName, thisClassConstructor);
        }
    }

    private boolean hasFields() {
        return !isEmpty(thisClassFields);
    }

    private static <T> void proceedObjectWithConsumer(T impl, final ParcelProxy proxy, final ExtPropertyMode mode, final Consumer<Field> consumer) {
        final ParcelableProcessor processor = aspectFor(impl);
        if (processor.hasFields()) {
            for (Field f : processor.thisClassFields) {
                switch (mode) {
                    case READ:
                        if (processor.readMethods.containsKey(f.getName())) {
                            Method          m    = processor.readMethods.get(f.getName());
                            Parcelable.Read prop = m.getAnnotation(Parcelable.Read.class);
                            proceedFieldPropertyParse(m, f, prop.mode(), consumer, impl, proxy.getTargetParcel());
                            continue;
                        }
                    case WRITE:
                        if (processor.writeMethods.containsKey(f.getName())) {
                            Method           m    = processor.writeMethods.get(f.getName());
                            Parcelable.Write prop = m.getAnnotation(Parcelable.Write.class);
                            proceedFieldPropertyParse(m, f, prop.mode(), consumer, impl, proxy.getTargetParcel());
                            continue;
                        }
                }

                consumer.accept(f);
            }
        }
    }

    private static void proceedFieldPropertyParse(Method m, Field f, Parcelable.Mode propMode, Consumer<Field> consumer, Object target, Object... args) {
        switch (propMode) {
            case AFTER:
                consumer.accept(f);
                invoke(m, target, args);
                break;
            case BEFORE:
                invoke(m, target, args);
                consumer.accept(f);
                break;
            case AROUND:
                invoke(m, target, args);
                break;
        }
    }

    @SuppressWarnings("ThrowFromFinallyBlock")
    private static void invoke(Method m, Object target, Object... args) {
        boolean accessUnsealed = false;
        if (!m.isAccessible()) {
            m.setAccessible(true);
            accessUnsealed = true;
        }

        try {
            m.invoke(target, args);
        } catch (Exception e) { e.printStackTrace(); }

        finally {
            if (accessUnsealed) {
                m.setAccessible(false);
            }
        }
    }

    //this error is fine, do not try to fix it!
    //AS just don't see the additional method of aspect class with 'pertypewithin' clause :)
    private static <T> ParcelableProcessor aspectFor(T obj) {
        return ParcelableProcessor.aspectOf(obj.getClass());
    }
}