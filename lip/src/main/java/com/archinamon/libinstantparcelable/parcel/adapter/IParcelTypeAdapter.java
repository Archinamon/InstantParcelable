package com.archinamon.libinstantparcelable.parcel.adapter;

import android.annotation.SuppressLint;

/**
 * TODO: Add destription
 *
 * @author archinamon on 06/12/16.
 */
@SuppressLint("NewApi")
public interface IParcelTypeAdapter<T, R> {

    R adapt(T obj);

    T wiseVersa(R adapted);

    default boolean lookupLink(Class<?> in) {
        return false;
    }
}
