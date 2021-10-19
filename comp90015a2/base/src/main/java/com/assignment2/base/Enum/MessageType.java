package com.assignment2.base.Enum;

public enum MessageType {

    ROOMCHANGE("roomchange"),
    ROOMCONTENTS("roomcontents"),
    ROOMLIST("roomlist"),
    MESSAGE("message"),
    HOSTCHANGE("hostchange"),
    LISTNEIGHBOTS("listneighbors");

    private String type;

    MessageType(String type) {
        this.type = type;
    }

    public String getType(){
        return this.type;
    }

}
