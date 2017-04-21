package com.archinamon.libinstantparcelable.parcel.engine;

interface AjcIParcelableBridge
    extends IParcelableBridge {

    Creator CREATOR = new InterParcelableCreatorImpl();
}