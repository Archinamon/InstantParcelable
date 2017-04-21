package com.archinamon.libinstantparcelable.parcel;

import com.archinamon.libinstantparcelable.parcel.adapter.IParcelTypeAdapter;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by archinamon on 26/02/16.
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Parcelable {

    int describeContents() default 0;

    Class<? extends IParcelTypeAdapter<?,?>>[] adapters() default {};

    enum Mode {

        BEFORE,
        AFTER,
        AROUND
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @interface Write {

        Mode mode() default Mode.AROUND;
        String field();
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @interface Read {

        Mode mode() default Mode.AROUND;
        String field();
    }
}