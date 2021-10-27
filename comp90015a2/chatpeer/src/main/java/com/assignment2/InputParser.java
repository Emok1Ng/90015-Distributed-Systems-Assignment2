package com.assignment2;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.assignment2.base.Enum.MessageType;

public class InputParser {

    private volatile String identity;
    private volatile String currentRoom;
    private volatile Manager manager;

    public InputParser(Manager manager) {
        this.manager = manager;
        this.currentRoom = "";
    }

    public synchronized boolean print(String s){
        JSONObject json = JSON.parseObject(s);
        String type = json.get("type").toString();
        if(type.equals(MessageType.MESSAGE.getType())){
            String speaker = json.get("identity").toString();
            String content = json.get("content").toString();
            System.out.printf("[%s] %s>%s\n",this.currentRoom,speaker,content);
        }
        else if(type.equals(MessageType.ROOMLIST.getType())){
            String toPrint = "";
            JSONArray rooms = (JSONArray) json.get("rooms");
            for(int i=0;i<rooms.size();i++){
                JSONObject room = (JSONObject) rooms.get(i);
                toPrint += room.get("roomid").toString() + ": " + room.get("count").toString() + " guests\n";
            }
            System.out.print(toPrint);
        }
        else if(type.equals(MessageType.ROOMCHANGE.getType())){
            String former = json.get("former").toString();
            String roomid = json.get("roomid").toString();
            String identity = json.get("identity").toString();
            if(former.equals(roomid)){
                System.out.println("The requested room is invalid now or non existent");
            }
            else{
                if(former.equals("-") && !roomid.equals("--")){
                    System.out.printf("%s join the server.\n",identity);
                    this.identity = identity;
                }
                else if(roomid.equals("-")) {
                    System.out.printf("%s left the server.\n", identity);
                }
                else if(former.equals("") && !roomid.equals("--")) {
                    System.out.printf("%s move to %s.\n",identity,roomid);
                }
                else if(roomid.equals("")){
                    System.out.printf("%s move out from %s.\n",identity,former);
                }
                else if(roomid.equals("--")){
                    System.out.println("You are kicked and the ip is banned.");
                    this.currentRoom = "";
                    this.manager.resetSocket();
                    return false;
                }
                else{
                    System.out.printf("%s move from %s to %s.\n",identity,former,roomid);
                }
                if(identity.equals(this.identity)){
                    if(!roomid.equals("-") && !roomid.equals("--")){
                        this.currentRoom = roomid;
                    }
                    else{
                        this.currentRoom = "";
                        this.manager.resetSocket();
                        return false;
                    }
                }
            }
        }
        else if(type.equals(MessageType.ROOMCONTENTS.getType())){
            String toPrint = "";
            String roomid = json.get("roomid").toString();
            JSONArray identities = (JSONArray) json.get("identities");
            if(identities.size() == 0){
                toPrint = roomid + " is an empty room";
            }
            else{
                toPrint += roomid + " contains";
                for(int i=0;i<identities.size();i++){
                    String identity = identities.get(i).toString();
                    toPrint += " " + identity;
                }
            }
            System.out.println(toPrint);
        }
        else if(type.equals(MessageType.NEIGHBORS.getType())){
            System.out.println(json.get("neighbors").toString());
        }
        return true;
    }
}
