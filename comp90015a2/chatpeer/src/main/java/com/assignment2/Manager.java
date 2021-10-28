package com.assignment2;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.assignment2.base.Enum.Command;
import com.assignment2.base.Enum.MessageType;
import com.assignment2.base.Message.C2S.List;
import com.assignment2.base.Message.C2S.ListNeighbors;
import com.assignment2.base.Message.C2S.Quit;
import com.assignment2.base.Message.S2C.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Manager {

    private volatile ArrayList<ChatRoom> roomList;
    private volatile ArrayList<String> identityList;
    private volatile HashMap<ChatPeer.ChatConnection, Guest> guestHashMap;
    private volatile HashMap<Guest, ChatPeer.ChatConnection> connectionHashMap;
    private volatile HashMap<String, ChatRoom> roomHashMap;
    private volatile Socket myPeer;
    private volatile PrintWriter writer;
    private volatile Integer pPort;
    private volatile Integer iPort;
    private volatile ArrayList<String> banList;

    public Manager() {
        this.identityList = new ArrayList<>();
        this.guestHashMap = new HashMap<>();
        this.connectionHashMap = new HashMap<>();
        this.roomList = new ArrayList<>();
        ChatRoom emptyRoom = new ChatRoom("", null);
        this.roomList.add(emptyRoom);
        this.roomHashMap = new HashMap<>();
        this.roomHashMap.put("", emptyRoom);
        this.banList = new ArrayList<>();
    }

    public void setPort(Integer iPort, Integer pPort){
        this.iPort = iPort;
        this.pPort = pPort;
        this.resetSocket();
    }

    public void resetSocket(){
        this.myPeer = null;
        this.writer = null;
        Guest g = new Guest();
        g.setCurrentRoom("");
        g.setIdentity("localhost");
        g.setpPort(this.pPort);
        g.setiPort(this.iPort);
        this.identityList.add("localhost");
        this.guestHashMap.put(null, g);
        this.connectionHashMap.put(g,null);
        ChatRoom emptyRoom = this.roomHashMap.get("");
        emptyRoom.addMember(g);
    }

    public void sendMessage(String message) {
        writer.print(message+"\n");
        writer.flush();
    }

    public boolean ban(String ip){
        return this.banList.contains(ip);
    }

    public ArrayList<BroadcastInfo> Analyze(String s, ChatPeer.ChatConnection connection, ChatPeer.Receiver receiver){
        ArrayList<BroadcastInfo> infoList;
        //local input
        if(connection == null){
            JSONObject json = JSON.parseObject(s);
            String command = json.get("type").toString();
            //have a peer
            if(this.myPeer != null){
                if(command.equals(Command.CREATEROOM.getCommand()) ||
                        command.equals(Command.KICK.getCommand()) ||
                        command.equals(Command.DELETEROOM.getCommand()) ||
                        command.equals(Command.CONNECT.getCommand())){
                    System.out.println("This command can not be used when connecting to a remote peer.");
                }
                else if(command.equals(Command.HELP.getCommand())){
                    this.Help();
                }
                else if(command.equals(Command.SEARCHNETWORK.getCommand())){
                    this.SearchNetwork();
                }
                else{
                    sendMessage(s);
                }
                return null;
            }
            //not have a peer
            Guest g = this.guestHashMap.get(null);
            if(command.equals(Command.JOIN.getCommand())){
                String roomid = json.get("roomid").toString();
                infoList = this.Join(roomid, g);
            }
            else if(command.equals(Command.LIST.getCommand())){
                infoList = this.List(g);
            }
            else if(command.equals(Command.WHO.getCommand())){
                String roomid = json.get("roomid").toString();
                infoList = this.Who(roomid, g);
            }
            else if(command.equals(Command.LISTNEIGHBORS.getCommand())){
                infoList = this.ListNeighbors(g);
            }
            else if(command.equals(Command.SEARCHNETWORK.getCommand())){
                infoList = this.SearchNetwork();
            }
            else if(command.equals(Command.QUIT.getCommand())){
                System.out.println("You are not connecting to a remote peer.");
                infoList = null;
            }
            else if(command.equals(Command.KICK.getCommand())){
                String identity = json.get("identity").toString();
                infoList = this.Kick(identity);
            }
            else if(command.equals(Command.HELP.getCommand())){
                infoList = this.Help();
            }
            else if(command.equals(Command.CREATEROOM.getCommand())){
                String roomid = json.get("roomid").toString();
                infoList = this.CreateRoom(roomid, g);
            }
            else if(command.equals(Command.DELETEROOM.getCommand())){
                String roomid = json.get("roomid").toString();
                infoList = this.DeleteRoom(roomid, g);
            }
            else if(command.equals(Command.CONNECT.getCommand())){
                String ip = json.get("ip").toString();
                String port = json.get("port").toString();
                infoList = this.Connect(ip,port,g,receiver);
            }
            else if(command.equals(Command.SHOUT.getCommand())){
                String content = json.get("content").toString();
                infoList = this.Shout(content,g);
            }
            else{
                if(this.guestHashMap.get(connection).getCurrentRoom().equals("")){
                    return null;
                }
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
        //remote input
        else{
            if(!this.guestHashMap.containsKey(connection)){
                Guest g = new Guest();
                infoList = NewIdentity(g, connection);
            }
            else{
                JSONObject json = JSON.parseObject(s);
                String command = json.get("type").toString();
                Guest g = this.guestHashMap.get(connection);
                if(command.equals(MessageType.HOSTCHANGE.getType())){
                    Integer pPort = Integer.valueOf(json.get("host").toString());
                    infoList = this.SendBackIdentity(pPort, g);
                }
                else if(command.equals(Command.JOIN.getCommand())){
                    String roomid = json.get("roomid").toString();
                    infoList = this.Join(roomid, g);
                }
                else if(command.equals(Command.LIST.getCommand())){
                    infoList = this.List(g);
                }
                else if(command.equals(Command.WHO.getCommand())){
                    String roomid = json.get("roomid").toString();
                    infoList = this.Who(roomid, g);
                }
                else if(command.equals(Command.LISTNEIGHBORS.getCommand())){
                    infoList = this.ListNeighbors(g);
                }
                else if(command.equals(Command.QUIT.getCommand())){
                    infoList = this.Quit(g);
                }
                else if(command.equals(Command.SHOUT.getCommand())){
                    String content = json.get("content").toString();
                    infoList = this.Shout(content,g);
                }
                else if(command.equals(MessageType.SHOUTMSG.getType())){
                    String speaker = json.get("identity").toString();
                    String content = json.get("content").toString();
                    System.out.printf("%s shouted %s\n",speaker,content);
                    return null;
                }
                else{
                    Message message = new Message();
                    message.setIdentity(this.guestHashMap.get(connection).getIdentity());
                    message.setContent(json.get("content").toString());
                    message.setType(MessageType.MESSAGE.getType());
                    infoList = new ArrayList<>();
                    BroadcastInfo info = new BroadcastInfo();
                    info.setContent(JSON.toJSONString(message));
                    ChatRoom room = this.roomHashMap.get(this.guestHashMap.get(connection).getCurrentRoom());
                    if(room.getRoomid().equals("")){
                        return null;
                    }
                    if(this.myPeer == null && this.guestHashMap.get(null).getCurrentRoom().equals(room.getRoomid())){
                        System.out.printf("[%s] %s>%s\n",g.getCurrentRoom(),g.getIdentity(),json.get("content"));
                    }
                    for(int i=0;i<room.getMembers().size();i++){
                        info.addConnection(this.connectionHashMap.get(room.getMembers().get(i)));
                    }
                    infoList.add(info);
                }
            }
        }
        return infoList;
    }

    private synchronized ArrayList<BroadcastInfo> NewIdentity(Guest g, ChatPeer.ChatConnection connection){
        String ip = connection.getSocket().getInetAddress().toString().split("/")[1];
        Integer iPort = connection.getSocket().getPort();
        if(this.myPeer == null && !this.ban(ip)){
            //System.out.printf("Connected by a new peer from %s:%s\n", ip, iPort);
        }
        g.setIdentity(ip);
        g.setiPort(iPort);
        g.setCurrentRoom("");
        this.connectionHashMap.put(g, connection);
        this.guestHashMap.put(connection, g);
        return null;
    }

    private synchronized ArrayList<BroadcastInfo> SendBackIdentity(Integer pPort, Guest g){
        ArrayList<BroadcastInfo> infoList = new ArrayList<>();
        String ip = this.connectionHashMap.get(g).getSocket().getInetAddress().toString().split("/")[1];
        g.setpPort(pPort);
        if(this.ban(ip)){
            BroadcastInfo info = new BroadcastInfo();
            RoomChange rc = new RoomChange();
            rc.setType(MessageType.ROOMCHANGE.getType());
            rc.setFormer("-");
            rc.setIdentity(g.getIdentity());
            rc.setRoomid("--");
            info.setContent(JSON.toJSONString(rc));
            info.addConnection(this.connectionHashMap.get(g));
            infoList.add(info);
            this.guestHashMap.remove(this.connectionHashMap.get(g));
            this.connectionHashMap.remove(g);
            return infoList;
        }
        this.identityList.add(g.getIdentity());
        RoomChange rc = new RoomChange();
        rc.setType(MessageType.ROOMCHANGE.getType());
        rc.setIdentity(g.getIdentity());
        rc.setRoomid("");
        rc.setFormer("-");
        this.roomHashMap.get("").addMember(g);
        BroadcastInfo info = new BroadcastInfo();
        info.setContent(JSON.toJSONString(rc));
        ChatPeer.ChatConnection connection = this.connectionHashMap.get(g);
        info.addConnection(connection);
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
            if(this.connectionHashMap.get(g)==null){
                System.out.println("The requested room is invalid now or non existent");
                return null;
            }
            info.addConnection(this.connectionHashMap.get(g));
            info.setContent(JSON.toJSONString(rc));
            infoList.add(info);
        }
        else{
            ArrayList<Guest> guestsToSendFormer = new ArrayList<>();
            ArrayList<Guest> guestsToSendCurrent = new ArrayList<>();
            guestsToSendCurrent = this.roomHashMap.get(roomid).getMembers();
            guestsToSendFormer = this.roomHashMap.get(g.getCurrentRoom()).getMembers();
            if(this.myPeer == null && (this.guestHashMap.get(null).getCurrentRoom().equals(g.getCurrentRoom()) || this.guestHashMap.get(null).getCurrentRoom().equals(roomid))){
                if(g.getCurrentRoom().equals("")){
                    System.out.printf("%s move to %s.\n",g.getIdentity(),roomid);
                }
                else if(roomid.equals("")){
                    System.out.printf("%s move out from %s.\n",g.getIdentity(),g.getCurrentRoom());
                }
                else{
                    System.out.printf("%s move from %s to %s.\n",g.getIdentity(),g.getCurrentRoom(),roomid);
                }
            }
            this.roomHashMap.get(g.getCurrentRoom()).deleteMember(g);
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
        return rc;
    }

    private ArrayList<HashMap> getRooms(){
        ArrayList<HashMap> rooms = new ArrayList<>();
        for(int i=0;i<this.roomList.size();i++){
            if(this.roomList.get(i).getRoomid().equals("")){
                continue;
            }
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
        ArrayList<HashMap> rooms = this.getRooms();
        //local part
        if(this.connectionHashMap.get(g) == null){
            String toPrint = "";
            for(int i=0;i<rooms.size();i++){
                HashMap each = rooms.get(i);
                toPrint += each.get("roomid").toString() + ": " + each.get("count").toString() + " guests\n";
            }
            System.out.print(toPrint);
            return null;
        }
        info.addConnection(this.connectionHashMap.get(g));
        rl.setRooms(rooms);
        info.setContent(JSON.toJSONString(rl));
        infoList.add(info);
        return infoList;
    }

    private synchronized ArrayList<BroadcastInfo> CreateRoom(String roomid, Guest g){
        if(!this.roomHashMap.containsKey(roomid)){
            ChatRoom newRoom = new ChatRoom(roomid, g);
            this.roomList.add(newRoom);
            this.roomHashMap.put(roomid, newRoom);
            System.out.printf("Room %s created.\n", roomid);
        }
        else{
            System.out.printf("Room %s is invalid or already in use.\n", roomid);
        }
        return null;
    }

    private synchronized ArrayList<BroadcastInfo> DeleteRoom(String roomid, Guest g){
        if(this.roomHashMap.containsKey(roomid) && !roomid.equals("")){
            ArrayList<BroadcastInfo> infoList = new ArrayList<>();
            ChatRoom room = this.roomHashMap.get(roomid);
            ArrayList<Guest> guests = room.getMembers();
            for(int i=0;i<guests.size();i++){
                BroadcastInfo roomChangeInfo = new BroadcastInfo();
                RoomChange rc = new RoomChange();
                rc.setType(MessageType.ROOMCHANGE.getType());
                rc.setIdentity(guests.get(i).getIdentity());
                rc.setFormer(roomid);
                rc.setRoomid("");
                roomChangeInfo.setContent(JSON.toJSONString(rc));
                Guest guest = guests.get(i);
                for(int j=0;j<guests.size();j++){
                    roomChangeInfo.addConnection(this.connectionHashMap.get(guests.get(j)));
                }
                if(this.myPeer==null && (this.guestHashMap.get(null).getCurrentRoom().equals(roomid) || this.guestHashMap.get(null).getCurrentRoom().equals(""))){
                    System.out.printf("%s moved out from %s\n",guest.getIdentity(),roomid);
                }
                guest.setCurrentRoom("");
                this.roomHashMap.get("").addMember(guest);
                infoList.add(roomChangeInfo);
            }
            this.roomList.remove(room);
            this.roomHashMap.remove(roomid);
            System.out.printf("Room %s is deleted.\n", roomid);
            return infoList;
        }
        else{
            System.out.printf("Room %s is not exist or cannot be deleted.\n", roomid);
            return null;
        }
    }

    private synchronized ArrayList<BroadcastInfo> Who(String roomid, Guest g){
        ArrayList<BroadcastInfo> infoList = new ArrayList<>();
        BroadcastInfo info = new BroadcastInfo();
        if(!this.roomHashMap.containsKey(roomid)){
            System.out.println("The requested room is invalid now or non existent");
            return null;
        }
        RoomContents rc = getRoomContents(roomid);
        if(this.connectionHashMap.get(g) == null){
            String toPrint = "";
            ArrayList<String> identities = rc.getIdentities();
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
            return null;
        }
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
        rc.setRoomid("-");
        info.setContent(JSON.toJSONString(rc));
        //System.out.printf("%s left the server\n",g.getIdentity());
        ArrayList<Guest> guestsToSend = new ArrayList<>();
        if(!g.getCurrentRoom().equals("")){
            guestsToSend = this.roomHashMap.get(g.getCurrentRoom()).getMembers();
        }
        else{
            guestsToSend.add(g);
        }
        for(int i=0;i<guestsToSend.size();i++){
            info.addConnection(this.connectionHashMap.get(guestsToSend.get(i)));
        }
        this.roomHashMap.get(g.getCurrentRoom()).deleteMember(g);
        this.identityList.remove(g.getIdentity());
        this.guestHashMap.remove(this.connectionHashMap.get(g));
        this.connectionHashMap.remove(g);
        infoList.add(info);
        return infoList;
    }

    private synchronized ArrayList<BroadcastInfo> SearchNetwork(){
        ArrayList<String> ips = new ArrayList<>();
        ArrayList<String> visited = new ArrayList<>();
        ListNeighbors ln = new ListNeighbors();
        ln.setType(Command.LISTNEIGHBORS.getCommand());
        List l = new List();
        l.setType(Command.LIST.getCommand());
        Quit q = new Quit();
        q.setType(Command.QUIT.getCommand());
        HostChange hc = new HostChange();
        hc.setType(MessageType.HOSTCHANGE.getType());
        hc.setHost(this.pPort.toString());
        PrintWriter writer;
        BufferedReader reader;
        String response;
        String ip;
        Integer port;
        Socket socket;
        String myIp;
        String toPrint = "";
        JSONObject json;
        if(myPeer!=null){
            ip = this.myPeer.getInetAddress().toString().split("/")[1];
            port = this.myPeer.getPort();
            ips.add(ip + ":" + port);
        }
        Iterator iter = this.guestHashMap.entrySet().iterator();
        while(iter.hasNext()){
            Map.Entry entry = (Map.Entry) iter.next();
            Guest value = (Guest) entry.getValue();
            if(value.getIp().equals("localhost:"+this.pPort)){
                continue;
            }
            ips.add(value.getIp());
        }
        while(ips.size()!=0){
            try{
                ip = ips.get(0).split(":")[0];
                port = Integer.parseInt(ips.get(0).split(":")[1]);
                toPrint += ip + ":" + port + '\n';
                visited.add(ip + ":" + port);
                socket = new Socket(ip, port);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);
                writer.print(JSON.toJSONString(hc)+"\n");
                writer.flush();
                response = reader.readLine();
                json = JSON.parseObject(response);
                myIp = json.get("identity").toString().split(":")[0] + ":" + this.pPort;
                writer.print(JSON.toJSONString(l)+"\n");
                writer.flush();
                response = reader.readLine();
                json = JSON.parseObject(response);
                JSONArray rooms = (JSONArray) json.get("rooms");
                for(int i=0;i<rooms.size();i++){
                    JSONObject room = (JSONObject) rooms.get(i);
                    toPrint += room.get("roomid") + " " + room.get("count") + " users.\n";
                }
                writer.print(JSON.toJSONString(ln)+"\n");
                writer.flush();
                response = reader.readLine();
                json = JSON.parseObject(response);
                JSONArray neighbors = (JSONArray) json.get("neighbors");
                for(int i=0;i<neighbors.size();i++){
                    if(!neighbors.get(i).equals(myIp) && !visited.contains(neighbors.get(i).toString())){
                        ips.add(neighbors.get(i).toString());
                    }
                }
                writer.print(JSON.toJSONString(q)+"\n");
                writer.flush();
                ips.remove(0);
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        System.out.print(toPrint);
        return null;
    }

    private synchronized ArrayList<BroadcastInfo> ListNeighbors(Guest g){
        ArrayList<BroadcastInfo> infoList = new ArrayList<>();
        BroadcastInfo info = new BroadcastInfo();
        Neighbors n = new Neighbors();
        n.setType(MessageType.NEIGHBORS.getType());
        ArrayList<String> neighbors = new ArrayList<>();
        Iterator iter = this.guestHashMap.entrySet().iterator();
        while(iter.hasNext()){
            Map.Entry entry = (Map.Entry) iter.next();
            Guest value = (Guest) entry.getValue();
            if(value.getIp().equals("localhost:"+this.pPort) || value.getIp().equals(g.getIp())){
                continue;
            }
            neighbors.add(value.getIp());
        }
        if(this.myPeer != null){
            String ip = this.myPeer.getInetAddress().toString().split("/")[1];
            Integer port = this.myPeer.getPort();
            neighbors.add(ip + ":" + port);
        }
        n.setNeighbors(neighbors);
        if(this.connectionHashMap.get(g) == null) {
            System.out.println(neighbors);
            return null;
        }
        info.setContent(JSON.toJSONString(n));
        info.addConnection((this.connectionHashMap.get(g)));
        infoList.add(info);
        return infoList;
    }

    private synchronized ArrayList<BroadcastInfo> Kick(String identity){
        if(!this.identityList.contains(identity)){
            System.out.println("Peer invalid or not exist.");
            return null;
        }
        Iterator iter = this.guestHashMap.entrySet().iterator();
        Guest kicked = null;
        while(iter.hasNext()){
            Map.Entry entry = (Map.Entry) iter.next();
            Guest value = (Guest) entry.getValue();
            if(value.getIdentity().equals(identity)){
                kicked = value;
                break;
            }
        }
        ArrayList<BroadcastInfo> infoList = new ArrayList<>();
        BroadcastInfo info1 = new BroadcastInfo();
        BroadcastInfo info2 = new BroadcastInfo();
        RoomChange rc1 = new RoomChange();
        RoomChange rc2 = new RoomChange();
        rc1.setType(MessageType.ROOMCHANGE.getType());
        rc1.setFormer(kicked.getCurrentRoom());
        rc1.setIdentity(identity);
        rc1.setRoomid("--");
        rc2.setType(MessageType.ROOMCHANGE.getType());
        rc2.setFormer(kicked.getCurrentRoom());
        rc2.setIdentity(identity);
        rc2.setRoomid("-");
        info1.setContent(JSON.toJSONString(rc1));
        info1.setContent(JSON.toJSONString(rc1));
        info1.addConnection(this.connectionHashMap.get(kicked));
        this.guestHashMap.remove(this.connectionHashMap.get(kicked));
        infoList.add(info1);
        info2.setContent(JSON.toJSONString(rc2));
        info2.setContent(JSON.toJSONString(rc2));
        this.roomHashMap.get(kicked.getCurrentRoom()).deleteMember(kicked);
        System.out.printf("%s is kicked.\n",identity);
        this.banList.add(identity.split(":")[0]);
        ArrayList<Guest> guestsToSend = this.roomHashMap.get(kicked.getCurrentRoom()).getMembers();
        for(int i=0;i<guestsToSend.size();i++){
            info2.addConnection(this.connectionHashMap.get(guestsToSend.get(i)));
        }
        infoList.add(info2);
        this.identityList.remove(identity);
        this.connectionHashMap.remove(kicked);
        return infoList;
    }

    private synchronized ArrayList<BroadcastInfo> Help(){
        System.out.println(
                "#connect IP[:port] [local port] - connect to another peer\n" +
                "#createroom RoomId - create a room\n" +
                "#delete RoomId - delete a room\n" +
                "#help - list this information\n" +
                "#join RoomId - join a room\n" +
                "#kick Identity - kick and block a user\n" +
                "#list - list all available rooms\n" +
                "#listneighbors - list peers' network address that connected to the peer\n" +
                "#quit - disconnect from a peer\n" +
                "#searchnetwork - search all available peers\n" +
                "#who RoomId - get information of a room"
        );
        return null;
    }

    private synchronized ArrayList<BroadcastInfo> Connect(String ip, String port, Guest g, ChatPeer.Receiver receiver){
        try{
            String[] parts = ip.split(":");
            String hostName = parts[0];
            Integer hostPort;
            if(parts.length == 1){
                hostPort = 4444;
            }
            else{
                hostPort = Integer.valueOf(parts[1]);
            }
            Socket socket;
            if(port.equals("")){
                if(this.iPort == -1){
                    socket = new Socket(hostName, hostPort);
                }
                else{
                    socket = new Socket();
                    socket.bind(new InetSocketAddress(iPort));
                    socket.connect(new InetSocketAddress(hostName, hostPort));
                }
            }
            else{
                socket = new Socket();
                socket.bind(new InetSocketAddress(Integer.parseInt(port)));
                socket.connect(new InetSocketAddress(hostName, hostPort));
            }
            this.myPeer = socket;
            this.writer = new PrintWriter(this.myPeer.getOutputStream(), true);
            receiver.setReader(this.myPeer);
        }catch (Exception e){
            System.out.println("Fail to connect.");
            this.myPeer = null;
            this.writer = null;
            return null;
        }
        HostChange hc = new HostChange();
        hc.setType(MessageType.HOSTCHANGE.getType());
        hc.setHost(this.pPort.toString());
        sendMessage(JSON.toJSONString(hc));
        this.roomHashMap.get(g.getCurrentRoom()).deleteMember(g);
        this.guestHashMap.remove(null);
        this.identityList.remove(g.getIdentity());
        this.connectionHashMap.remove(g);
        return null;
    }

    private synchronized ArrayList<BroadcastInfo> Shout(String content, Guest g){
        if(g.getCurrentRoom().equals("")){
            return null;
        }
        String identity;
        ArrayList<String> ips = new ArrayList<>();
        ArrayList<String> visited = new ArrayList<>();
        if(this.connectionHashMap.get(g) == null){
            identity = "127.0.0.1" + ":" + this.pPort;
        }
        else{
            identity = g.getIdentity();
        }
        System.out.printf("%s shouted %s\n",identity,content);
        ShoutMsg shout = new ShoutMsg();
        shout.setType(MessageType.SHOUTMSG.getType());
        shout.setIdentity(identity);
        shout.setContent(content);
        ListNeighbors ln = new ListNeighbors();
        ln.setType(Command.LISTNEIGHBORS.getCommand());
        Quit q = new Quit();
        q.setType(Command.QUIT.getCommand());
        HostChange hc = new HostChange();
        hc.setType(MessageType.HOSTCHANGE.getType());
        hc.setHost(this.pPort.toString());
        PrintWriter writer;
        BufferedReader reader;
        String response;
        String ip;
        Integer port;
        Socket socket;
        String myIp;
        JSONObject json;
        if(myPeer!=null){
            ip = this.myPeer.getInetAddress().toString().split("/")[1];
            port = this.myPeer.getPort();
            ips.add(ip + ":" + port);
        }
        Iterator iter = this.guestHashMap.entrySet().iterator();
        while(iter.hasNext()){
            Map.Entry entry = (Map.Entry) iter.next();
            Guest value = (Guest) entry.getValue();
            if(value.getIp().equals("localhost:"+this.pPort)){
                continue;
            }
            ips.add(value.getIp());
        }
        while(ips.size()!=0){
            try{
                ip = ips.get(0).split(":")[0];
                port = Integer.parseInt(ips.get(0).split(":")[1]);
                visited.add(ip + ":" + port);
                socket = new Socket(ip, port);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);
                writer.print(JSON.toJSONString(hc)+"\n");
                writer.flush();
                response = reader.readLine();
                json = JSON.parseObject(response);
                myIp = json.get("identity").toString().split(":")[0] + ":" + this.pPort;
                writer.print(JSON.toJSONString(ln)+"\n");
                writer.flush();
                response = reader.readLine();
                json = JSON.parseObject(response);
                JSONArray neighbors = (JSONArray) json.get("neighbors");
                for(int i=0;i<neighbors.size();i++){
                    if(!neighbors.get(i).equals(myIp) && !visited.contains(neighbors.get(i).toString())){
                        ips.add(neighbors.get(i).toString());
                    }
                }
                writer.print(JSON.toJSONString(shout)+"\n");
                writer.flush();
                writer.print(JSON.toJSONString(q)+"\n");
                writer.flush();
                ips.remove(0);
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        return null;
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
