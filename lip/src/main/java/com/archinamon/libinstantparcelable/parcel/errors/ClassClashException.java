package com.archinamon.libinstantparcelable.parcel.errors;

/**
 * TODO: Add destription
 *
 * @author archinamon on 11/05/16.
 */
public final class ClassClashException
    extends RuntimeException {

    private static final String EXCEPTION_TEXT = "Type was already initialized. Check the parcelable compiler.";

    public ClassClashException() {
        super(EXCEPTION_TEXT);
    }
}