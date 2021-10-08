package com.assignment2.base.Enum;

public enum MessageType {

    NEWIDENTITY("newidentity"),ROOMCHANGE("roomchange"),
    ROOMCONTENTS("roomcontents"),ROOMLIST("roomlist"),
    MESSAGE("message");

    private String type;

    MessageType(String type) {
        this.type = type;
    }

    public String getType(){
        return this.type;
    }

}
