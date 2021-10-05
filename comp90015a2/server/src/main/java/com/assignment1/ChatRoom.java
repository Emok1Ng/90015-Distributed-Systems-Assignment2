package com.assignment1;

import java.util.ArrayList;

public class ChatRoom {

    private String roomid;
    private Guest owner;
    private ArrayList<Guest> members;

    public ChatRoom(String roomid, Guest owner) {
        this.roomid = roomid;
        this.owner = owner;
        this.members = new ArrayList<>();
    }

    public String getRoomid() {
        return roomid;
    }

    public void setRoomid(String roomid) {
        this.roomid = roomid;
    }

    public Guest getOwner() {
        return owner;
    }

    public void setOwner(Guest owner) {
        this.owner = owner;
    }

    public ArrayList<Guest> getMembers() {
        return members;
    }

    public void addMember(Guest member) {
        this.members.add(member);
    }

    public void deleteMember(Guest member){
        this.members.remove(member);
    }

}
