package com.assignment1;

import com.alibaba.fastjson.JSONObject;
import com.assignment1.base.Enum.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

  private int port = 4444;
  private boolean alive;
  private final Manager manager = new Manager();

  public Server(int port) {
    this.port = port;
  }

  public Server() {
  }

  public static void main(String[] args) {
    Server server_chat;
    try{
      if(args.length==2 && args[0].equals("-p")){
        server_chat = new Server(Integer.parseInt(args[1]));
      }
      else{
        server_chat = new Server();
      }
    }
    catch (Exception e){
      server_chat = new Server();
    }
    server_chat.handle();
  }

  private synchronized void broadCast(ArrayList<Manager.BroadcastInfo> infoList) {
    for(int i=0;i<infoList.size();i++){
      Manager.BroadcastInfo info = infoList.get(i);
      String content = info.getContent();
      //System.out.printf("[Server] %s\n", content);
      for(int j=0;j<info.getConnections().size();j++){
        //System.out.println(info.getConnections().get(j));
        info.getConnections().get(j).sendMessage(content);
      }
    }
  }

  public void handle() {
    ServerSocket serverSocket;
    ExecutorService threadpool = Executors.newFixedThreadPool(50);
    try {
      System.out.println("[Server]:Waiting for connection......");
      serverSocket = new ServerSocket((port));
      System.out.printf("[Server]:Listening on port %d\n", port);
      alive = true;
      while(alive){
        Socket socket = serverSocket.accept();
        ChatConnection connection = new ChatConnection(socket);
        threadpool.execute(connection);
      }
    }
    catch(Exception e){
      System.out.println("server closed");
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
      broadCast(manager.Analyze("{\"type\":\"join\",\"roomid\":\"MainHall\"}",this));
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
}
