package com.archinamon.libinstantparcelable.parcel.errors;

/**
 * TODO: Add destription
 *
 * @author archinamon on 11/05/16.
 */
public final class NoDefaultConstructorException
    extends RuntimeException {

    private static final String EXCEPTION_TEXT = "No valid default constructor provided for @Parcelable extension";

    public NoDefaultConstructorException() {
        super(EXCEPTION_TEXT);
    }
}