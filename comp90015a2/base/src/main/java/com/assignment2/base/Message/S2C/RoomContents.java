package com.assignment2.base.Message.S2C;

import com.assignment2.base.Message.Room;

import java.util.ArrayList;

public class RoomContents extends Room {

    private ArrayList<String> identities;
    private String owner;

    public ArrayList<String> getIdentities() {
        return identities;
    }

    public void setIdentities(ArrayList<String> identities) {
        this.identities = identities;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
