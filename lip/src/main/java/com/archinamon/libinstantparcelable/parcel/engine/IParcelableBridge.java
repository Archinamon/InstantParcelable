package com.archinamon.libinstantparcelable.parcel.engine;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * TODO: Add destription
 *
 * @author archinamon on 04/12/16.
 */

interface IParcelableBridge
    extends Parcelable {

    void readFromParcel(Parcel in);
}
