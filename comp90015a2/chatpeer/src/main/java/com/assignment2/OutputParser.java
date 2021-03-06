package com.assignment2;

import com.alibaba.fastjson.JSON;
import com.assignment2.base.Enum.Command;
import com.assignment2.base.Enum.MessageType;
import com.assignment2.base.Message.C2S.*;

import java.util.regex.Pattern;

public class OutputParser {

    public String toJSON(String s){
        String toSend;
        if(s.isEmpty()){
            return null;
        }
        else if(s.charAt(0) == '#'){
            toSend = parseToCommand(s.substring(1));
        }
        else{
            toSend = parseToMessage(s);
        }
        return toSend;
    }

    public String parseToCommand(String s){
        String[] parts = s.split(" ");
        String command = parts[0];
        String arg1 = "";
        String arg2 = "";
        String roomPattern = "^[a-zA-Z]{1}[a-zA-Z0-9]{2,31}";
        if(command.equals(Command.JOIN.getCommand())){
            if(parts.length >= 2){
                arg1 = parts[1];
            }
            else{
                arg1 = "";
            }
            if(!Pattern.matches(roomPattern, arg1) && !arg1.equals("")){
                return null;
            }
            Join j = new Join();
            j.setType(Command.JOIN.getCommand());
            j.setRoomid(arg1);
            return JSON.toJSONString(j);
        }
        else if(command.equals(Command.LIST.getCommand())){
            List l = new List();
            l.setType(Command.LIST.getCommand());
            return JSON.toJSONString(l);
        }
        else if(command.equals(Command.WHO.getCommand()) && parts.length >= 2){
            arg1 = parts[1];
            if(!Pattern.matches(roomPattern, arg1)){
                return null;
            }
            Who w = new Who();
            w.setType(Command.WHO.getCommand());
            w.setRoomid(arg1);
            return JSON.toJSONString(w);
        }
        else if(command.equals(Command.LISTNEIGHBORS.getCommand())){
            ListNeighbors l = new ListNeighbors();
            l.setType(Command.LISTNEIGHBORS.getCommand());
            return JSON.toJSONString(l);
        }
        else if(command.equals(Command.QUIT.getCommand())){
            Quit q = new Quit();
            q.setType(Command.QUIT.getCommand());
            return JSON.toJSONString(q);
        }
        else if(command.equals(Command.CREATEROOM.getCommand()) && parts.length >= 2){
            arg1 = parts[1];
            if(!Pattern.matches(roomPattern, arg1)){
                return null;
            }
            CreateRoom cr = new CreateRoom();
            cr.setType(Command.CREATEROOM.getCommand());
            cr.setRoomid(arg1);
            return JSON.toJSONString(cr);
        }
        else if(command.equals(Command.DELETEROOM.getCommand()) && parts.length >= 2){
            arg1 = parts[1];
            if(!Pattern.matches(roomPattern, arg1)){
                return null;
            }
            Delete d = new Delete();
            d.setType(Command.DELETEROOM.getCommand());
            d.setRoomid(arg1);
            return JSON.toJSONString(d);
        }
        else if(command.equals(Command.KICK.getCommand()) && parts.length >= 2){
            arg1 = parts[1];
            Kick k = new Kick();
            k.setType(Command.KICK.getCommand());
            k.setIdentity(arg1);
            return JSON.toJSONString(k);
        }
        else if(command.equals(Command.SEARCHNETWORK.getCommand())){
            SearchNetwork sn = new SearchNetwork();
            sn.setType(Command.SEARCHNETWORK.getCommand());
            return JSON.toJSONString(sn);
        }
        else if(command.equals(Command.CONNECT.getCommand()) && parts.length >= 2){
            arg1 = parts[1];
            if(parts.length == 2){
                arg2 = "";
            }
            else{
                arg2 = parts[2];
            }
            Connect c = new Connect();
            c.setIp(arg1);
            c.setPort(arg2);
            c.setType(Command.CONNECT.getCommand());
            return JSON.toJSONString(c);
        }
        else if(command.equals(Command.HELP.getCommand())){
            Help h = new Help();
            h.setType(Command.HELP.getCommand());
            return JSON.toJSONString(h);
        }
        else if(command.equals(Command.SHOUT.getCommand()) && parts.length >= 2){
            arg1 = parts[1];
            Shout sh = new Shout();
            sh.setType(Command.SHOUT.getCommand());
            sh.setContent(arg1);
            return JSON.toJSONString(sh);
        }
        else{
            return null;
        }
    }

    public String parseToMessage(String s){
        Message message = new Message();
        message.setType(MessageType.MESSAGE.getType());
        message.setContent(s);
        return JSON.toJSONString(message);
    }
}
