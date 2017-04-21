package com.archinamon.libinstantparcelable.parcel.errors;

/**
 * TODO: Add destription
 *
 * @author archinamon on 11/05/16.
 */
public final class ClassErroneousException
    extends RuntimeException {

    private static final String EXCEPTION_TEXT = "Trying to explicitly cast non-@Parelable class to android.os.Parcelable";

    public ClassErroneousException() {
        super(EXCEPTION_TEXT);
    }
}