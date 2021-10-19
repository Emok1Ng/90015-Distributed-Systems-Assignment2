package com.assignment2.base.Message.S2C;

import com.assignment2.base.Message.Base;

import java.util.ArrayList;

public class ListNeighbors extends Base {

    private ArrayList<String> neighbors;

    public ArrayList<String> getNeighbors() {
        return neighbors;
    }

    public void setNeighbors(ArrayList<String> neighbors) {
        this.neighbors = neighbors;
    }
}
