package com.assignment1;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.assignment1.base.Enum.MessageType;

public class InputParser {

    private volatile String identity;
    private volatile String currentRoom;

    public InputParser() {
        this.identity = "";
        this.currentRoom = "MainHall";
    }

    public synchronized boolean print(String s, int status, String opearingRoom){
        JSONObject json = JSON.parseObject(s);
        //System.out.printf(json.toJSONString());
        String type = json.get("type").toString();
        if(type.equals(MessageType.MESSAGE.getType())){
            String speaker = json.get("identity").toString();
            String content = json.get("content").toString();
            System.out.printf("[%s] %s>%s\n",this.currentRoom,speaker,content);
        }
        else if(type.equals(MessageType.ROOMLIST.getType())){
            if(status == 0){
                String toPrint = "";
                JSONArray rooms = (JSONArray) json.get("rooms");
                for(int i=0;i<rooms.size();i++){
                    JSONObject room = (JSONObject) rooms.get(i);
                    toPrint += room.get("roomid").toString() + ": " + room.get("count").toString() + " guests\n";
                }
                System.out.print(toPrint);
            }
            else if(status == 1){
                JSONArray rooms = (JSONArray) json.get("rooms");
                for(int i=0;i<rooms.size();i++){
                    JSONObject room = (JSONObject) rooms.get(i);
                    if(room.get("roomid").toString().equals(opearingRoom)){
                        System.out.printf("Room %s created\n",opearingRoom);
                        return true;
                    }
                }
                System.out.printf("Room %s is invalid or already in use\n",opearingRoom);
            }
            else if(status == 2){
                JSONArray rooms = (JSONArray) json.get("rooms");
                for(int i=0;i<rooms.size();i++){
                    JSONObject room = (JSONObject) rooms.get(i);
                    if(room.get("roomid").toString().equals(opearingRoom)){
                        System.out.printf("Room %s is invalid or cannot be deleted due to ownership\n",opearingRoom);
                        return true;
                    }
                }
                System.out.printf("Room %s deleted\n",opearingRoom);
            }
        }
        else if(type.equals(MessageType.NEWIDENTITY.getType())){
            String former = json.get("former").toString();
            String identity = json.get("identity").toString();
            if(!former.equals(identity)){
                if(former.equals(this.identity)){
                    this.identity = identity;
                }
                if(!former.equals("")){
                    System.out.printf("%s is now %s\n",former,identity);
                }
            }
            else{
                System.out.println("Requested identity invalid or in use");
            }
        }
        else if(type.equals(MessageType.ROOMCHANGE.getType())){
            String former = json.get("former").toString();
            String roomid = json.get("roomid").toString();
            String identity = json.get("identity").toString();
            if(former.equals(roomid)){
                System.out.println("The requested room is invalid or non existent");
            }
            else{
                if(former.equals("")){
                    System.out.printf("%s join the %s\n",identity,roomid);
                }
                else{
                    if(roomid.equals("")){
                        System.out.printf("%s left the server\n",identity);
                        if(identity.equals(this.identity)){
                            return false;
                        }
                        return true;
                    }
                    System.out.printf("%s move from %s to %s\n",identity,former,roomid);
                }
                if(identity.equals(this.identity)){
                    this.currentRoom = roomid;
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
        return true;
    }
}
