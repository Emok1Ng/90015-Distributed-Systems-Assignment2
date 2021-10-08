package com.assignment2.base.Message.S2C;

import com.assignment2.base.Message.Base;

import java.util.ArrayList;
import java.util.HashMap;

public class RoomList extends Base {

    private ArrayList<HashMap> rooms;

    public ArrayList<HashMap> getRooms() {
        return rooms;
    }

    public void setRooms(ArrayList<HashMap> rooms) {
        this.rooms = rooms;
    }
}
