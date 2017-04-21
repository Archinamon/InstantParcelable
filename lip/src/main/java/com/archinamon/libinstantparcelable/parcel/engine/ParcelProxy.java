package com.archinamon.libinstantparcelable.parcel.engine;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.archinamon.libinstantparcelable.parcel.adapter.IParcelTypeAdapter;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.archinamon.libinstantparcelable.parcel.engine.AjcIParcelableBridge.CREATOR;

/**
 * Created by archinamon on 26/02/16.
 */
final class ParcelProxy {

    private synchronized static ParcelProxy instance(Parcel parcel, Object owner) {
        ParcelProxy instance = new ParcelProxy();
        instance.mParcel = parcel;
        instance.mOwner = owner;

        return instance;
    }

    private volatile Parcel mParcel;
    private volatile Object mOwner;

    static Map<Class<?>, IParcelTypeAdapter> typeAdapters    = new HashMap<>();
    static Map<Class<?>, Class<?>>           typeTargetLinks = new HashMap<>();

    private ParcelProxy() {}

    @NonNull
    static ParcelProxy with(Parcel p, Object o) {
        return instance(p, o);
    }

    @Nullable
    Parcel getTargetParcel() {
        return mParcel;
    }

    @SuppressWarnings("unchecked")
    private ParcelProxy write(Field field, Object value) {
        if (value != null && typeAdapters.containsKey(value.getClass())) {
            IParcelTypeAdapter adapter = typeAdapters.get(value.getClass());
            value = adapter.lookupLink(mOwner.getClass()) ? adapter.adapt(value) : value;
        }

        if (value instanceof List) {
            if (isListOfParcelables(field)) {
                mParcel.writeTypedList((List<Parcelable>) value);
            } else {
                mParcel.writeList((List) value);
            }
        } else if (value instanceof Map) {
            mParcel.writeMap((Map) value);
        } else if (value instanceof Parcelable[]) {
            mParcel.writeParcelableArray((Parcelable[]) value, 0);
        } else {
            mParcel.writeValue(value);
        }

        return this;
    }

    private Object read(ClassLoader loader) {
        return mParcel.readValue(loader);
    }

    synchronized void extractAndWrite(final Field field) {
        unseal(field);

        try {
            write(field, field.get(mOwner));
        } catch (Exception e) {
            e.printStackTrace();
            write(field, null);
        }

        finally {
            seal(field);
        }
    }

    @SuppressWarnings("unchecked")
    synchronized void readAndDefine(final Field field) {
        try {
            Class<?> fieldType = field.getType();
            ClassLoader loader = fieldType.getClassLoader();

            if (typeAdapters.containsKey(fieldType)) {
                IParcelTypeAdapter adapter = typeAdapters.get(fieldType);
                if (adapter.lookupLink(mOwner.getClass())) {
                    loader = typeTargetLinks.get(fieldType).getClassLoader();

                    unseal(field);
                    field.set(mOwner, typeAdapters.get(fieldType).wiseVersa(read(loader)));
                    return; // if we have found adapted value, no need to pass it below
                }
            }

            Object value = null;
            if (List.class.isAssignableFrom(fieldType)) {
                if (isListOfParcelables(field)) {
                    value = mParcel.createTypedArrayList(CREATOR);
                } else {
                    value = mParcel.readArrayList(loader);
                }
            } else if (Map.class.isAssignableFrom(fieldType)) {
                value = mParcel.readHashMap(getMapValueObjClassLoader((Class<? extends Map>) fieldType));
            } else if (Parcelable[].class.isAssignableFrom(fieldType)) {
                final Parcelable[] values = mParcel.readParcelableArray(loader);
                Object newArr = fieldType.cast(Array.newInstance(fieldType.getComponentType(), values.length));

                //noinspection SuspiciousSystemArraycopy
                System.arraycopy(values, 0, newArr, 0, values.length);
            } else {
                value = read(loader);
            }

            unseal(field);
            field.set(mOwner, value);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                unseal(field);
                field.set(mOwner, null);
            } catch (IllegalAccessException ignore) {}
        }

        finally {
            seal(field);
        }
    }

    private static volatile transient boolean wasUnlocked = false;
    private static void unseal(Field field) {
        synchronized (ParcelProxy.class) {
            if (!field.isAccessible()) {
                field.setAccessible(wasUnlocked = true);
            }
        }
    }

    private static void seal(Field field) {
        synchronized (ParcelProxy.class) {
            if (wasUnlocked) {
                field.setAccessible(wasUnlocked = false);
            }
        }
    }

    private boolean isListOfParcelables(Field field) {
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        Type[] genericTypes = genericType.getActualTypeArguments();
        if (genericTypes.length == 0) { throw new IllegalArgumentException("List passing to Parcel should be properly parametrized"); }

        Type listParamType = genericTypes[0];//for List we always have only one param type or less
        return Parcelable.class.isAssignableFrom((Class) listParamType);
    }

    private ClassLoader getMapValueObjClassLoader(Class<? extends Map> mapClass) {
        ParameterizedType generics = (ParameterizedType) mapClass.getGenericSuperclass();
        Type[] genericTypes = generics.getActualTypeArguments();
        if (genericTypes.length < 2) { throw new IllegalArgumentException("Map writing to parcel should be properly parametrized"); }

        return ((Class) genericTypes[1]).getClassLoader();
    }
}