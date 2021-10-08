package com.assignment2.base.Enum;

public enum Command {
    IDENTITYCHANGE("identitychange"),JOIN("join"),
    WHO("who"),LIST("list"),CREATEROOM("createroom"),
    DELETEROOM("delete"),QUIT("quit");

    private String command;

    Command(String command) {
        this.command = command;
    }

    public String getCommand(){
        return this.command;
    }
}
