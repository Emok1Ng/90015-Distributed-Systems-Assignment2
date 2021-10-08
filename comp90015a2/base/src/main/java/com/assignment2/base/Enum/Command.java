package com.assignment2.base.Enum;

public enum Command {
    //local
    CREATEROOM("createroom"),DELETEROOM("delete"),
    KICK("kick"),QUIT("quit"),
    SEARCHNETWORK("searchnetwork"),CONNECT("connect"),HELP("help"),

    //remote
    JOIN("join"),WHO("who"),LIST("list"),
    LISTNEIGHBORS("listneighbors");

    private String command;

    Command(String command) {
        this.command = command;
    }

    public String getCommand(){
        return this.command;
    }
}
