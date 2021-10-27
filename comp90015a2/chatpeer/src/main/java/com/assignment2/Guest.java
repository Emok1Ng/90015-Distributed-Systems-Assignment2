package com.assignment2;

public class Guest {

    private String identity;
    private String currentRoom;
    private Integer pPort;
    private Integer iPort;

    public Guest() {
    }

    public String getIp(){
        return identity + ":" + pPort;
    }

    public String getIdentity() {
        if(identity.equals("localhost")){
            return identity + ":" + pPort;
        }
        return identity + ":" + iPort;
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

    public Integer getiPort() {
        return iPort;
    }

    public void setiPort(Integer iPort) {
        this.iPort = iPort;
    }
}
