package com.assignment2;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.assignment2.base.Enum.Command;
import com.assignment2.base.Enum.MessageType;
import com.assignment2.base.Message.S2C.*;


import java.util.ArrayList;
import java.util.HashMap;

public class Manager {

    private volatile ArrayList<ChatRoom> roomList;
    private volatile ArrayList<String> identityList;
    private volatile HashMap<ChatPeer.ChatConnection, Guest> guestHashMap;
    private volatile HashMap<Guest, ChatPeer.ChatConnection> connectionHashMap;
    private volatile HashMap<String, ChatRoom> roomHashMap;
    private volatile String nextIdentity;

    public Manager() {
        this.roomList = new ArrayList<>();
        ChatRoom hall = new ChatRoom("MainHall", null);
        this.roomList.add(hall);
        this.identityList = new ArrayList<>();
        this.guestHashMap = new HashMap<>();
        this.connectionHashMap = new HashMap<>();
        this.roomHashMap = new HashMap<>();
        this.roomHashMap.put("MainHall", hall);
        this.nextIdentity = "guest1";
    }

    public ArrayList<BroadcastInfo> Analyze(String s, ChatPeer.ChatConnection connection){
        ArrayList<BroadcastInfo> infoList;
        if(!this.guestHashMap.containsKey(connection)){
            Guest g = new Guest();
            infoList = NewIdentity(g, connection);
            System.out.printf("%s join the server\n",g.getIdentity());
        }
        else{
            JSONObject json = JSON.parseObject(s);
            String command = json.get("type").toString();
            Guest g = this.guestHashMap.get(connection);
            if(command.equals(Command.JOIN.getCommand())){
                String roomid = json.get("roomid").toString();
                if(!g.getCurrentRoom().equals("")){
                    System.out.printf("[%s] %s>#%s %s\n",g.getCurrentRoom(),g.getIdentity(),Command.JOIN.getCommand(),roomid);
                }
                infoList = this.Join(roomid, g);
            }
            else if(command.equals(Command.LIST.getCommand())){
                System.out.printf("[%s] %s>#%s\n",g.getCurrentRoom(),g.getIdentity(),Command.LIST.getCommand());
                infoList = this.List(g);
            }
            else if(command.equals(Command.CREATEROOM.getCommand())){
                String roomid = json.get("roomid").toString();
                System.out.printf("[%s] %s>#%s %s\n",g.getCurrentRoom(),g.getIdentity(),Command.CREATEROOM.getCommand(),roomid);
                infoList = this.CreateRoom(roomid, g);
            }
            else if(command.equals(Command.DELETEROOM.getCommand())){
                String roomid = json.get("roomid").toString();
                System.out.printf("[%s] %s>#%s %s\n",g.getCurrentRoom(),g.getIdentity(),Command.DELETEROOM.getCommand(),roomid);
                infoList = this.DeleteRoom(roomid, g);
            }
            else if(command.equals(Command.WHO.getCommand())){
                String roomid = json.get("roomid").toString();
                System.out.printf("[%s] %s>#%s %s\n",g.getCurrentRoom(),g.getIdentity(),Command.WHO.getCommand(),roomid);
                infoList = this.Who(roomid, g);
            }
            else if(command.equals(Command.QUIT.getCommand())){
                System.out.printf("[%s] %s>#%s\n",g.getCurrentRoom(),g.getIdentity(),Command.QUIT.getCommand());
                infoList = this.Quit(g);
            }
            else{
                Message message = new Message();
                message.setIdentity(this.guestHashMap.get(connection).getIdentity());
                message.setContent(json.get("content").toString());
                message.setType(MessageType.MESSAGE.getType());
                System.out.printf("[%s] %s>%s\n",g.getCurrentRoom(),g.getIdentity(),json.get("content"));
                infoList = new ArrayList<>();
                BroadcastInfo info = new BroadcastInfo();
                info.setContent(JSON.toJSONString(message));
                ChatRoom room = this.roomHashMap.get(this.guestHashMap.get(connection).getCurrentRoom());
                for(int i=0;i<room.getMembers().size();i++){
                    info.addConnection(this.connectionHashMap.get(room.getMembers().get(i)));
                }
                infoList.add(info);
            }
        }
        return infoList;
    }

    public void CheckIdentity(){
        int count = 1;
        while(true){
            String identity = "guest" + count;
            boolean flag = true;
            for(int i=0;i<this.identityList.size();i++){
                if(this.identityList.get(i).equals(identity)){
                    flag = false;
                    break;
                }
            }
            if(flag){
                this.nextIdentity = identity;
                break;
            }
            count += 1;
        }
    }

    private synchronized ArrayList<BroadcastInfo> NewIdentity(Guest g, ChatPeer.ChatConnection connection){
        ArrayList<BroadcastInfo> infoList = new ArrayList<>();
        g.setIdentity(this.nextIdentity);
        g.setCurrentRoom("");
        this.connectionHashMap.put(g, connection);
        this.guestHashMap.put(connection, g);
        this.identityList.add(g.getIdentity());
        this.CheckIdentity();
        NewIdentity ni = new NewIdentity();
        ni.setType(MessageType.NEWIDENTITY.getType());
        ni.setFormer("");
        ni.setIdentity(g.getIdentity());
        BroadcastInfo info = new BroadcastInfo();
        info.setContent(JSON.toJSONString(ni));
        for(ChatPeer.ChatConnection c:this.guestHashMap.keySet()){
            info.addConnection(c);
        }
        infoList.add(info);
        return infoList;
    }

    private synchronized ArrayList<BroadcastInfo> Join(String roomid, Guest g){
        ArrayList<BroadcastInfo> infoList = new ArrayList<>();
        RoomChange rc = new RoomChange();
        rc.setType(MessageType.ROOMCHANGE.getType());
        rc.setIdentity(g.getIdentity());
        rc.setFormer(g.getCurrentRoom());
        BroadcastInfo info = new BroadcastInfo();
        if(g.getCurrentRoom().equals(roomid) || !this.roomHashMap.containsKey(roomid)){
            rc.setRoomid(g.getCurrentRoom());
            info.addConnection(this.connectionHashMap.get(g));
            info.setContent(JSON.toJSONString(rc));
            infoList.add(info);
        }
        else{
            ArrayList<Guest> guestsToSendFormer = new ArrayList<>();
            ArrayList<Guest> guestsToSendCurrent = this.roomHashMap.get(roomid).getMembers();
            if(!g.getCurrentRoom().equals("")){
                this.roomHashMap.get(g.getCurrentRoom()).deleteMember(g);
                if(!g.getCurrentRoom().equals("MainHall")){
                    ChatRoom checkout = this.roomHashMap.get(g.getCurrentRoom());
                    if(checkout.getMembers().size()==0 && checkout.getOwner()==null){
                        this.roomList.remove(checkout);
                        this.roomHashMap.remove(checkout.getRoomid());
                    }
                }
                if(this.roomHashMap.get(g.getCurrentRoom())!=null){
                    guestsToSendFormer = this.roomHashMap.get(g.getCurrentRoom()).getMembers();
                }
                System.out.printf("%s move from %s to %s\n",g.getIdentity(),g.getCurrentRoom(),roomid);
            }
            else{
                System.out.printf("%s join the %s\n",g.getIdentity(),roomid);
            }
            this.roomHashMap.get(roomid).addMember(g);
            g.setCurrentRoom(roomid);
            rc.setRoomid(roomid);
            for(int i=0;i<guestsToSendFormer.size();i++){
                info.addConnection(this.connectionHashMap.get(guestsToSendFormer.get(i)));
            }
            for(int i=0;i<guestsToSendCurrent.size();i++){
                info.addConnection(this.connectionHashMap.get(guestsToSendCurrent.get(i)));
            }
            info.setContent(JSON.toJSONString(rc));
            infoList.add(info);
            if(roomid.equals("MainHall")){
                BroadcastInfo roomContentsInfo = new BroadcastInfo();
                RoomContents roomContents = getRoomContents(roomid);
                roomContentsInfo.setContent(JSON.toJSONString(roomContents));
                roomContentsInfo.addConnection(this.connectionHashMap.get(g));
                infoList.add(roomContentsInfo);
                BroadcastInfo roomListInfo = new BroadcastInfo();
                RoomList rl = new RoomList();
                rl.setType(MessageType.ROOMLIST.getType());
                ArrayList<HashMap> rooms = getRooms();
                rl.setRooms(rooms);
                roomListInfo.setContent(JSON.toJSONString(rl));
                roomListInfo.addConnection(this.connectionHashMap.get(g));
                infoList.add(roomListInfo);
            }
        }
        return infoList;
    }

    private RoomContents getRoomContents(String roomid){
        RoomContents rc = new RoomContents();
        rc.setType(MessageType.ROOMCONTENTS.getType());
        rc.setRoomid(roomid);
        ArrayList<Guest> guests = this.roomHashMap.get(roomid).getMembers();
        ArrayList<String> identities = new ArrayList<>();
        for(int i =0;i<guests.size();i++){
            identities.add(guests.get(i).getIdentity());
        }
        rc.setIdentities(identities);
        Guest owner = this.roomHashMap.get(roomid).getOwner();
        rc.setOwner(owner!=null ? owner.getIdentity() : "");
        return rc;
    }

    private ArrayList<HashMap> getRooms(){
        ArrayList<HashMap> rooms = new ArrayList<>();
        for(int i=0;i<this.roomList.size();i++){
            HashMap each = new HashMap();
            each.put("roomid", this.roomList.get(i).getRoomid());
            each.put("count", this.roomList.get(i).getMembers().size());
            rooms.add(each);
        }
        return rooms;
    }

    private ArrayList<BroadcastInfo> List(Guest g){
        ArrayList<BroadcastInfo> infoList = new ArrayList<>();
        RoomList rl = new RoomList();
        rl.setType(MessageType.ROOMLIST.getType());
        BroadcastInfo info = new BroadcastInfo();
        info.addConnection(this.connectionHashMap.get(g));
        ArrayList<HashMap> rooms = this.getRooms();
        rl.setRooms(rooms);
        info.setContent(JSON.toJSONString(rl));
        infoList.add(info);
        return infoList;
    }

    private synchronized ArrayList<BroadcastInfo> CreateRoom(String roomid, Guest g){
        ArrayList<BroadcastInfo> infoList = new ArrayList<>();
        RoomList rl = new RoomList();
        rl.setType(MessageType.ROOMLIST.getType());
        BroadcastInfo info = new BroadcastInfo();
        ArrayList<HashMap> rooms = this.getRooms();
        if(!this.roomHashMap.containsKey(roomid)){
            ChatRoom newRoom = new ChatRoom(roomid, g);
            this.roomList.add(newRoom);
            this.roomHashMap.put(roomid, newRoom);
            g.addOwnership(roomid);
            HashMap add = new HashMap();
            add.put("roomid", roomid);
            add.put("count", 0);
            rooms.add(add);
        }
        else{
            for(int i=0;i<rooms.size();i++){
                HashMap iter = rooms.get(i);
                if(iter.get("roomid").equals(roomid)){
                    rooms.remove(iter);
                    break;
                }
            }
        }
        rl.setRooms(rooms);
        info.setContent(JSON.toJSONString(rl));
        info.addConnection(this.connectionHashMap.get(g));
        infoList.add(info);
        return infoList;
    }

    private synchronized ArrayList<BroadcastInfo> DeleteRoom(String roomid, Guest g){
        ArrayList<BroadcastInfo> infoList = new ArrayList<>();
        RoomList rl = new RoomList();
        rl.setType(MessageType.ROOMLIST.getType());
        BroadcastInfo info = new BroadcastInfo();
        if(this.roomHashMap.containsKey(roomid) && !roomid.equals("MainHall") && this.roomHashMap.get(roomid).getOwner().equals(g)){
            ChatRoom room = this.roomHashMap.get(roomid);
            ArrayList<Guest> guests = room.getMembers();
            ArrayList<Guest> mainHallGuests = this.roomHashMap.get("MainHall").getMembers();
            for(int i=0;i<guests.size();i++){
                BroadcastInfo roomChangeInfo = new BroadcastInfo();
                RoomChange rc = new RoomChange();
                rc.setType(MessageType.ROOMCHANGE.getType());
                rc.setIdentity(guests.get(i).getIdentity());
                rc.setFormer(roomid);
                rc.setRoomid("MainHall");
                roomChangeInfo.setContent(JSON.toJSONString(rc));
                Guest guest = guests.get(i);
                for(int j=0;j<guests.size();j++){
                    roomChangeInfo.addConnection(this.connectionHashMap.get(guest));
                }
                for(int j=0;j<mainHallGuests.size();j++){
                    roomChangeInfo.addConnection(this.connectionHashMap.get(mainHallGuests.get(j)));
                }
                System.out.printf("%s move from %s to MainHall\n",guest.getIdentity(),roomid);
                guest.setCurrentRoom("MainHall");
                this.roomHashMap.get("MainHall").addMember(guest);
                infoList.add(roomChangeInfo);
            }

            g.deleteOwnership(roomid);
            this.roomList.remove(room);
            this.roomHashMap.remove(roomid);

            BroadcastInfo roomContentsInfo = new BroadcastInfo();
            roomContentsInfo.setContent(JSON.toJSONString(getRoomContents("MainHall")));
            for(int i=0;i<guests.size();i++){
                roomContentsInfo.addConnection(this.connectionHashMap.get(guests.get(i)));
            }
            infoList.add(roomContentsInfo);

            ArrayList<HashMap> rooms = this.getRooms();
            rl.setRooms(rooms);
            info.setContent(JSON.toJSONString(rl));
            for(int i=0;i<guests.size();i++){
                info.addConnection(this.connectionHashMap.get(guests.get(i)));
            }
            if(!guests.contains(g)){
                info.addConnection(this.connectionHashMap.get(g));
            }
            infoList.add(info);
        }
        else{
            ArrayList<HashMap> rooms = this.getRooms();
            if(!this.roomHashMap.containsKey(roomid)){
                HashMap vituralRoom = new HashMap();
                vituralRoom.put("roomid", roomid);
                vituralRoom.put("count", 0);
                rooms.add(vituralRoom);
            }
            rl.setRooms(rooms);
            info.setContent(JSON.toJSONString(rl));
            info.addConnection(this.connectionHashMap.get(g));
            infoList.add(info);
        }
        return infoList;
    }

    private synchronized ArrayList<BroadcastInfo> Who(String roomid, Guest g){
        ArrayList<BroadcastInfo> infoList = new ArrayList<>();
        BroadcastInfo info = new BroadcastInfo();
        if(!this.roomHashMap.containsKey(roomid)){
            infoList.add(info);
            return infoList;
        }
        RoomContents rc = getRoomContents(roomid);
        info.setContent(JSON.toJSONString(rc));
        info.addConnection(this.connectionHashMap.get(g));
        infoList.add(info);
        return infoList;
    }

    private synchronized ArrayList<BroadcastInfo> Quit(Guest g){
        ArrayList<BroadcastInfo> infoList = new ArrayList<>();
        BroadcastInfo info = new BroadcastInfo();
        RoomChange rc = new RoomChange();
        rc.setType(MessageType.ROOMCHANGE.getType());
        rc.setFormer(g.getCurrentRoom());
        rc.setIdentity(g.getIdentity());
        rc.setRoomid("");
        info.setContent(JSON.toJSONString(rc));
        System.out.printf("%s left the server\n",g.getIdentity());
        ArrayList<Guest> guestsToSend = this.roomHashMap.get(g.getCurrentRoom()).getMembers();
        for(int i=0;i<guestsToSend.size();i++){
            info.addConnection(this.connectionHashMap.get(guestsToSend.get(i)));
        }
        this.roomHashMap.get(g.getCurrentRoom()).deleteMember(g);
        this.identityList.remove(g.getIdentity());
        this.CheckIdentity();
        this.connectionHashMap.remove(g);
        infoList.add(info);
        return infoList;
    }


    class BroadcastInfo{

        private String content;
        private ArrayList<ChatPeer.ChatConnection> connections;

        public BroadcastInfo(){
            this.connections = new ArrayList<>();
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public ArrayList<ChatPeer.ChatConnection> getConnections() {
            return connections;
        }

        public void addConnection(ChatPeer.ChatConnection connect) {
            this.connections.add(connect);
        }

        public void deleteConnection(ChatPeer.ChatConnection connect){
            this.connections.remove(connect);
        }
    }
}
