package com.assignment1;

import java.util.ArrayList;

public class Guest {

    private String identity;
    private String currentRoom;
    private ArrayList<String> ownership;

    public Guest() {
        this.ownership = new ArrayList<>();
    }

    public String getIdentity() {
        return identity;
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

    public ArrayList<String> getOwnership() {
        return ownership;
    }

    public void addOwnership(String chatRoom){
        this.ownership.add(chatRoom);
    }

    public void deleteOwnership(String chatRoom){
        this.ownership.remove(chatRoom);
    }

}
