package com.archinamon.libinstantparcelable.parcel.engine;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.archinamon.libinstantparcelable.parcel.errors.NoDefaultConstructorException;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.archinamon.libinstantparcelable.parcel.engine.Utils.readArray;

final class InterParcelableCreatorImpl<T extends AjcIParcelableBridge> implements Parcelable.Creator<T> {

    private static HashMap<String, Constructor<?>> constructors = new LinkedHashMap<>();

    public @Override @Nullable T createFromParcel(Parcel source) {
        String className = source.readString();

        final boolean hasStoredConstructor = constructors.containsKey(className);
        if (!hasStoredConstructor) {
            throw new NoDefaultConstructorException();
        }

        Constructor<?> constructor = constructors.get(className);

        boolean accessSealed = false;
        if (!constructor.isAccessible()) {
            constructor.setAccessible(true);
            accessSealed = true;
        }

        try {
            final T object = (T) constructor.newInstance();
            object.readFromParcel(source);
            return object;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        finally {
            if (accessSealed) {
                //noinspection ThrowFromFinallyBlock
                constructor.setAccessible(false);
            }
        }
    }

    public @Override @NonNull T[] newArray(int size) {
        return readArray(size);
    }

    void registerType(final @NonNull String className, final @NonNull Constructor<?> constructor) throws ClassNotFoundException {
        if (constructors.containsKey(className)) return;
        constructors.put(className, constructor);
    }
}