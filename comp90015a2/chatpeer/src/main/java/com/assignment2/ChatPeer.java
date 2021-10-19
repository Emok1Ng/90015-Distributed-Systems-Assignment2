package com.assignment2;

import com.alibaba.fastjson.JSONObject;
import com.assignment2.base.Enum.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatPeer {

    private int iPort = 4444;
    private int pPort = 4444;
    private String hostName;

    private final Manager manager = new Manager();

    public ChatPeer(int pPort, int iPort) {
        this.hostName = "127.0.0.1";
        this.pPort = pPort;
        this.iPort = iPort;
    }

    public static void main(String[] args) {
        ChatPeer chatPeer;
        try{
            //todo
            chatPeer = new ChatPeer(4444,4444);
        }
        catch (Exception e){
            chatPeer = new ChatPeer(4444,4444);
        }
        chatPeer.handle();
    }

    private synchronized void broadCast(ArrayList<Manager.BroadcastInfo> infoList) {
        for(int i=0;i<infoList.size();i++){
            Manager.BroadcastInfo info = infoList.get(i);
            String content = info.getContent();
            for(int j=0;j<info.getConnections().size();j++){
                info.getConnections().get(j).sendMessage(content);
            }
        }
    }

    public void handle() {
        ServerSocket serverSocket;
        ExecutorService threadpool = Executors.newFixedThreadPool(50);
        try {
            System.out.println("[LocalChatPeer]:Waiting for connection......");
            serverSocket = new ServerSocket((pPort));
            System.out.printf("[LocalChatPeer]:Listening on port %d\n", pPort);
            Reader reader = new Reader();
            threadpool.execute(reader);
            while(true){
                Socket socket = serverSocket.accept();
                ChatConnection connection = new ChatConnection(socket);
                threadpool.execute(connection);
            }
        }
        catch(Exception e){
            System.out.println("[LocalChatPeer]: Closed......");
        }
    }

    class ChatConnection implements Runnable {
        private Socket socket;
        private PrintWriter writer;
        private BufferedReader reader;
        private boolean connection_alive = false;

        public ChatConnection(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream(), true);
        }

        @Override
        public void run() {
            connection_alive = true;
            broadCast(manager.Analyze("",this));
            broadCast(manager.Analyze("{\"type\":\"join\",\"roomid\":\"\"}",this));
            while (connection_alive) {
                try{
                    String input  = reader.readLine();
                    if(input != null){
                        broadCast(manager.Analyze(input,this));
                        String type = JSONObject.parseObject(input).get("type").toString();
                        if(type.equals(Command.QUIT.getCommand())){
                            connection_alive = false;
                        }
                    }
                    else{
                        connection_alive = false;
                    }
                }catch (Exception e){
                    broadCast(manager.Analyze("{\"type\":\"quit\"}",this));
                    connection_alive  = false;
                }
            }
            close();
            this.close();
        }

        public void close() {
            try {
                reader.close();
                writer.close();
                socket.close();
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

        public void sendMessage(String message) {
            writer.print(message+"\n");
            writer.flush();
        }
    }

    class Reader implements Runnable{
        private BufferedReader keyboard;
        private OutputParser outputParser;
        public Reader() throws IOException {
            this.keyboard = new BufferedReader(new InputStreamReader(System.in));
            this.outputParser = new OutputParser();
        }
        @Override
        public void run() {
            boolean readerAlive = true;
            while (readerAlive) {
                try {
                    String message = keyboard.readLine();
                    String toSend = outputParser.toJSON(message);
                    if (toSend != null) {
                        broadCast(manager.Analyze(toSend,null));
                    } else {
                        System.out.println("[ERROR]Unable to send message due to Invalid command/Lack of arguments/Invalid identity(names begin with 'guest' followed by numbers are preserved) or roomid");
                    }
                } catch (Exception e) {
                    readerAlive = false;
                }
            }
        }
    }
}
