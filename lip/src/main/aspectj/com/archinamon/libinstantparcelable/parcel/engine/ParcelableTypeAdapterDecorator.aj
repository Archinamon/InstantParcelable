package com.archinamon.libinstantparcelable.parcel.engine;

import com.archinamon.libinstantparcelable.parcel.adapter.IParcelTypeAdapter;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * TODO: Add destription
 *
 * @author archinamon on 06/12/16.
 */
final privileged aspect ParcelableTypeAdapterDecorator issingleton() {

    declare error: illegalInstantiationCall() : "You can not instantiate IParcelableTypeAdapter directly!";
    declare error: unsupportedAdapterType(): "Return types of Map or Iterable (all collections, lists or sets) are not supported.";

    pointcut illegalInstantiationCall(): call(IParcelTypeAdapter+.new(..)) && !within(com.archinamon.libinstantparcelable.parcel.engine.**);
    pointcut unsupportedAdapterType(): withincode(java.util.Map+ IParcelTypeAdapter+.adapt(..)) || withincode(Iterable+ IParcelTypeAdapter+.adapt(..));

    @Target(METHOD)
    @Retention(RUNTIME)
    @interface Locator {}

    declare @method: * IParcelTypeAdapter+.adapt(!Object) : @Locator;

    Class<? extends AjcIParcelableBridge> IParcelTypeAdapter.thisLinkedWith;
    private pointcut lookupLink(Class<?> in): withincode(boolean IParcelTypeAdapter.lookupLink(Class)) && args(in);
    boolean around(Class<?> in): lookupLink(in) {
        return in == ((IParcelTypeAdapter) thisJoinPoint.getThis()).thisLinkedWith;
    }

    pointcut init(IParcelTypeAdapter adapter): !adviceexecution() && initialization(IParcelTypeAdapter.new()) && target(adapter);

    after(IParcelTypeAdapter adapter) returning: init(adapter) {
        for (Method m : adapter.getClass().getDeclaredMethods()) {
            if (m.getAnnotation(Locator.class) != null) {
                Class<?> targetType = m.getParameterTypes()[0];
                Class<?> returnType = m.getReturnType();

                ParcelProxy.typeAdapters.put(targetType, adapter);
                ParcelProxy.typeTargetLinks.put(targetType, returnType);
            }
        }
    }
}
