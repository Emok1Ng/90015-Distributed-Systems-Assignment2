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

    private int iPort;
    private int pPort;
    private String hostName;

    private Manager manager = new Manager();

    public ChatPeer(int pPort, int iPort) {
        this.hostName = "127.0.0.1";
        this.pPort = pPort;
        this.iPort = iPort;
        this.manager.setPort(iPort, pPort);
    }

    public static void main(String[] args) {
        ChatPeer chatPeer;
        Integer pPort = 4444;
        Integer iPort = -1;
        try{
            for(int i=0;i<args.length;i++){
                if(args[i].equals("-p")){
                    pPort = Integer.valueOf(args[i+1]);
                    i += 1;
                }
                else if(args[i].equals("-i")){
                    iPort = Integer.valueOf(args[i+1]);
                    i += 1;
                }
            }
            chatPeer = new ChatPeer(pPort,iPort);
        }
        catch (Exception e){
            chatPeer = new ChatPeer(pPort,iPort);
        }
        chatPeer.handle();
    }

    private synchronized void broadCast(ArrayList<Manager.BroadcastInfo> infoList) {
        if(infoList != null){
            for(int i=0;i<infoList.size();i++){
                Manager.BroadcastInfo info = infoList.get(i);
                String content = info.getContent();
                for(int j=0;j<info.getConnections().size();j++){
                    if(info.getConnections().get(j) != null){
                        info.getConnections().get(j).sendMessage(content);
                    }
                }
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
            Receiver receiver = new Receiver();
            threadpool.execute(receiver);
            Reader reader = new Reader(receiver);
            threadpool.execute(reader);
            while(true){
                Socket socket = serverSocket.accept();
                ChatConnection connection = new ChatConnection(socket);
                threadpool.execute(connection);
            }
        }
        catch(Exception e){
            e.printStackTrace();
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
            broadCast(manager.Analyze("",this,null));
            while (connection_alive) {
                try{
                    String input  = reader.readLine();
                    if(input != null){
                        broadCast(manager.Analyze(input,this,null));
                        String type = JSONObject.parseObject(input).get("type").toString();
                        if(type.equals(Command.QUIT.getCommand())){
                            connection_alive = false;
                        }
                    }
                    else{
                        connection_alive = false;
                    }
                }catch (Exception e){
                    broadCast(manager.Analyze("{\"type\":\"quit\"}",this,null));
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

        public Socket getSocket(){
            return this.socket;
        }
    }

    class Reader implements Runnable{
        private BufferedReader keyboard;
        private OutputParser outputParser;
        private Receiver receiver;
        public Reader(Receiver receiver) throws IOException {
            this.receiver = receiver;
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
                        broadCast(manager.Analyze(toSend,null, receiver));
                    } else {
                        System.out.println("[ERROR]Unable to process message due to Invalid command/Lack of arguments/Invalid roomid");
                    }
                } catch (Exception e) {
                    readerAlive = false;
                }
            }
        }
    }

    class Receiver implements Runnable{
        private volatile BufferedReader reader;
        private InputParser inputParser;

        public Receiver() throws IOException {
            this.reader = null;
            this.inputParser = new InputParser(manager);
        }

        public void setReader(Socket socket) {
            try{
                this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            }
            catch(Exception e){
                this.reader = null;
            }
        }

        public void run() {
            while(true){
                if(reader != null){
                    try {
                        String response = reader.readLine();
                        inputParser.print(response);
                    }
                    catch (Exception e){

                    }
                }
            }
        }
    }
}
