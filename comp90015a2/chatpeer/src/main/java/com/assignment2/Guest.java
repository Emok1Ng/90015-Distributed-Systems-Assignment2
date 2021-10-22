package com.assignment2;

public class Guest {

    private String identity;
    private String currentRoom;
    private Integer pPort;

    public Guest() {
    }

    public String getIdentity() {
        return identity + ":" + pPort;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(String currentRoom) {
        this.currentRoom = currentRoom;
    }

    public Integer getpPort() {
        return pPort;
    }

    public void setpPort(Integer pPort) {
        this.pPort = pPort;
    }
}
