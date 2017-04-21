package com.archinamon.libinstantparcelable.parcel.engine;

import android.os.Parcel;
import android.support.annotation.NonNull;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Consumer;
import com.annimon.stream.function.Function;
import com.annimon.stream.function.Predicate;
import com.archinamon.libinstantparcelable.parcel.Parcelable;
import com.archinamon.libinstantparcelable.parcel.adapter.IParcelTypeAdapter;
import com.archinamon.libinstantparcelable.parcel.errors.InvalidArgumentException;
import com.archinamon.libinstantparcelable.parcel.errors.NoDefaultConstructorException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.archinamon.libinstantparcelable.parcel.ParcelableUtils.isEmpty;

/**
 * Utility class which supports operations with fields and {@link ParcelProxy} class lambdas
 *
 * @author archinamon on 30/11/16.
 */
final class Utils {

    static Set<IParcelTypeAdapter<?,?>> registerParcelableTypeAdapters(Class<? extends IParcelTypeAdapter<?,?>>[] adapters) {
        Set<IParcelTypeAdapter<?,?>> insts = new HashSet<>();
        for (Class<? extends IParcelTypeAdapter<?,?>> adapter : adapters) {
            try {
                insts.add(adapter.newInstance());
                // instantiation will be caught by aspectj
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return insts;
    }

    @NonNull
    @SafeVarargs
    static <T> T[] readArray(int size, T... objects) {
        return Arrays.copyOf(objects, size);
    }

    @NonNull
    static Consumer<Field> getReadMethod(ParcelProxy proxy) {
        return proxy::readAndDefine;
    }

    @NonNull
    static Consumer<Field> getWriteMethod(ParcelProxy proxy) {
        return proxy::extractAndWrite;
    }

    static Field[] extractFields(Class<?> clazz) {
        return innerExtractFieldsUnsafe(clazz);
    }

    static void extractReadMethods(final Map<String, Method> collection, final Class<?> clazz) {
        extractMethodsWithPredicate(collection, clazz, Utils::isContentReaderMethod, method -> method.getAnnotation(Parcelable.Read.class).field());
    }

    static void extractWriteMethods(final Map<String, Method> collection, final Class<?> clazz) {
        extractMethodsWithPredicate(collection, clazz, Utils::isContentWriterMethod, method -> method.getAnnotation(Parcelable.Write.class).field());
    }

    static Constructor<?> defaultConstructor(Class<?> jpClass) {
        return Stream.of(jpClass.getDeclaredConstructors())
            .filter(Utils::isDefault)
            .findFirst()
            .orElseThrow(NoDefaultConstructorException::new);
    }

    static void registerType(final @NonNull String className, final @NonNull Constructor<?> constructor) {
        try {
            creator().registerType(className, constructor);
        } catch (ClassNotFoundException e) { e.printStackTrace(); }
    }

    private static boolean isStatic(Field f) {
        return Modifier.isStatic(f.getModifiers());
    }

    private static boolean isTransient(Field f) {
        return Modifier.isTransient(f.getModifiers());
    }

    private static Field[] innerExtractFieldsUnsafe(Class<?> clazz) {
        Set<Field> fields = new HashSet<>(Arrays.asList(clazz.getFields()));
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));

        while (clazz.getSuperclass() != Object.class) {
            clazz = clazz.getSuperclass();
            fields.addAll(Arrays.asList(clazz.getFields()));
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        }

        return Stream.of(fields)
            .filterNot(Utils::isStatic)
            .filterNot(Utils::isTransient)
            .toArray(Field[]::new);
    }

    @Deprecated
    private static Field[] innerExtractFieldsSafe(Class<?> clazz) {
        return Stream.of(clazz.getFields())
            .filterNot(Utils::isStatic)
            .filterNot(Utils::isTransient)
            .toArray(Field[]::new);
    }

    private static boolean isContentReaderMethod(Method m) {
        return m.getAnnotation(Parcelable.Read.class) != null;
    }

    private static boolean isContentWriterMethod(Method m) {
        return m.getAnnotation(Parcelable.Write.class) != null;
    }

    private static boolean isParcelMethodValid(Method m) {
        Class<?>[] argTypes = m.getParameterTypes();
        if (!isEmpty(argTypes) && argTypes.length == 1 && argTypes[0] == Parcel.class)
            return true;

        throw new InvalidArgumentException();
    }

    private static void extractMethodsWithPredicate(final Map<String, Method> collection, Class<?> clazz, final Predicate<Method> predicate, final Function<Method, String> func) {
        Set<Method> methods = new HashSet<>(Arrays.asList(clazz.getMethods()));
        methods.addAll(Arrays.asList(clazz.getDeclaredMethods()));

        while (clazz.getSuperclass() != Object.class) {
            clazz = clazz.getSuperclass();
            methods.addAll(Arrays.asList(clazz.getMethods()));
            methods.addAll(Arrays.asList(clazz.getDeclaredMethods()));
        }

        Stream.of(methods)
            .filter(predicate)
            .filter(Utils::isParcelMethodValid)
            .collect(() -> collection, (map, method) -> {
                final String fieldLink = func.apply(method);
                map.put(fieldLink, method);
            });
    }

    private static InterParcelableCreatorImpl<?> creator() {
        return (InterParcelableCreatorImpl<?>) AjcIParcelableBridge.CREATOR;
    }

    private static boolean isDefault(Constructor<?> constructor) {
        return isEmpty(constructor.getParameterTypes());
    }
}
