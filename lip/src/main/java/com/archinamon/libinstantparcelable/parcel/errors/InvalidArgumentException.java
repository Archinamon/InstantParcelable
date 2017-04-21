package com.archinamon.libinstantparcelable.parcel.errors;

/**
 * TODO: Add destription
 *
 * @author archinamon on 11/05/16.
 */
public final class InvalidArgumentException
    extends RuntimeException {

    private static final String EXCEPTION_TEXT = "@Read and @Write methods should declare not more nor less then one argument of type android.os.Parcel";

    public InvalidArgumentException() {
        super(EXCEPTION_TEXT);
    }
}