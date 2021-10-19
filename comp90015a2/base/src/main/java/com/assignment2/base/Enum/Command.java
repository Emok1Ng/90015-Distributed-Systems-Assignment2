package com.assignment2.base.Enum;

public enum Command {
    //local
    CREATEROOM("createroom"),DELETEROOM("delete"),
    KICK("kick"),HELP("help"),
    SEARCHNETWORK("searchnetwork"),CONNECT("connect"),

    //remote
    JOIN("join"),WHO("who"),LIST("list"),QUIT("quit"),
    LISTNEIGHBORS("listneighbors");

    private String command;

    Command(String command) {
        this.command = command;
    }

    public String getCommand(){
        return this.command;
    }
}
