package com.assignment1.base.Message.S2C;

import com.assignment1.base.Message.Room;

public class RoomChange extends Room {

    private String identity;
    private String former;

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getFormer() {
        return former;
    }

    public void setFormer(String former) {
        this.former = former;
    }
}
