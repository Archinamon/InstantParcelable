package com.archinamon.libinstantparcelable.parcel;

import com.archinamon.libinstantparcelable.parcel.errors.ClassErroneousException;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

/**
 * TODO: Add destription
 *
 * @author archinamon on 30/11/16.
 */

public final class ParcelableUtils {

    @SuppressWarnings("unchecked")
    public static <T> T unparcel(android.os.Parcelable parcelable) {
        if (parcelable.getClass().getAnnotation(Parcelable.class) != null) {
            return (T) parcelable;
        }

        throw new ClassErroneousException();
    }

    public static <T> android.os.Parcelable parcelable(T parcelable) {
        if (parcelable.getClass().getAnnotation(Parcelable.class) != null) {
            return ((android.os.Parcelable) parcelable);
        }

        if (parcelable instanceof android.os.Parcelable) {
            return (android.os.Parcelable) parcelable;
        }

        throw new ClassErroneousException();
    }

    public static <T> android.os.Parcelable[] parcelables(T[] parcelables) {
        if (!isEmpty(parcelables) && parcelables[0].getClass().getAnnotation(Parcelable.class) != null) {
            return (android.os.Parcelable[]) parcelables;
        }

        if (parcelables instanceof android.os.Parcelable[]) {
            return (android.os.Parcelable[]) parcelables;
        }

        throw new ClassErroneousException();
    }

    public static <T> boolean isEmpty(T val) throws IllegalArgumentException {
        if (val == null) {
            return true;
        } else if (val instanceof CharSequence) {
            return ((CharSequence) val).length() == 0;
        } else if (val instanceof Collection) {
            return ((Collection) val).isEmpty();
        } else if (val instanceof Map) {
            return ((Map) val).isEmpty();
        } else if (val.getClass().isArray()) {
            return Array.getLength(val) == 0;
        } else {
            throw new IllegalArgumentException("Class has incompatible type: " + val.getClass());
        }
    }

    public static boolean isEmpty(CharSequence val) {
        return val == null || val.length() == 0;
    }

    public static boolean isEmpty(Collection val) {
        return val == null || val.isEmpty();
    }

    public static boolean isEmpty(Map val) {
        return val == null || val.isEmpty();
    }

    public static <T> boolean isEmpty(T[] values) {
        return values == null || values.length == 0;
    }
}
