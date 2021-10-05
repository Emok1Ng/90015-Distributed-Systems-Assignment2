package com.assignment1;

import com.alibaba.fastjson.JSONObject;
import com.assignment1.base.Enum.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {

  private int port = 4444;
  private String hostName;
  public volatile int status;
  public volatile boolean senderAlive;
  public volatile boolean receiverAlive;
  public volatile String roomid;
  //1:Creating 2:Deleting 0:others
  public Client(String hostName) {
    this.hostName = hostName;
    status = 0;
    senderAlive = false;
    receiverAlive = false;
    this.port = 4444;
  }

  public Client(String hostName, int port) {
    this.hostName = hostName;
    status = 0;
    senderAlive = false;
    receiverAlive = false;
    this.port = port;
  }

  public static void main(String[] args) {
    Client client;
    try{
      if(args.length == 1){
        client = new Client(args[0]);
      }
      else if(args.length == 3 && args[1].equals("-p")){
        client = new Client(args[0],Integer.parseInt(args[2]));
      }
      else{
        client = new Client("127.0.0.1",4444);
      }
    }
    catch (Exception e){
      client = new Client("127.0.0.1",4444);
    }
    try{
      client.handle();
    }
    catch (Exception e){
      System.out.println("Cannot connect to server");
    }
  }

  public void handle() throws IOException{
    Socket socket = new Socket(hostName, port);
    ExecutorService threadpool = Executors.newFixedThreadPool(2);
    Sender sender = new Sender(socket);
    Receiver receiver = new Receiver(socket);
    threadpool.execute(sender);
    threadpool.execute(receiver);
    threadpool.shutdown();
  }

  class Sender implements Runnable{
    private PrintWriter writer;
    private BufferedReader keyboard;
    private OutputParser outputParser;

    public Sender(Socket socket) throws IOException {
      this.writer = new PrintWriter(socket.getOutputStream(), true);
      this.keyboard = new BufferedReader(new InputStreamReader(System.in));
      this.outputParser = new OutputParser();
    }
    @Override
    public void run() {
      senderAlive = true;
      while (senderAlive) {
        try {
          String message = keyboard.readLine();
          String toSend = outputParser.toJSON(message);
          //System.out.println(toSend);
          if (toSend != null) {
            if(JSONObject.parseObject(toSend).get("type").equals(Command.CREATEROOM.getCommand())){
              status = 1;
              roomid = JSONObject.parseObject(toSend).get("roomid").toString();
            }
            else if(JSONObject.parseObject(toSend).get("type").equals(Command.DELETEROOM.getCommand())){
              status = 2;
              roomid = JSONObject.parseObject(toSend).get("roomid").toString();
            }
            else if(JSONObject.parseObject(toSend).get("type").equals(Command.QUIT.getCommand())){
              senderAlive = false;
            }
            writer.println(toSend);
          } else {
            System.out.println("[ERROR]Unable to send message due to Invalid command/Lack of arguments/Invalid identity(names begin with 'guest' followed by numbers are preserved) or roomid");
          }
        } catch (Exception e) {
          senderAlive = false;
        }
      }
    }
  }

  class Receiver implements Runnable{
    private BufferedReader reader;
    private InputParser inputParser;
    public Receiver(Socket socket) throws IOException {
      this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      this.inputParser = new InputParser();
    }
    public void run() {
      receiverAlive = true;
      while(receiverAlive){
        try {
          String response = reader.readLine();
          receiverAlive = inputParser.print(response, status, roomid);
          roomid = "";
          status = 0;
        }
        catch (Exception e){
          receiverAlive = false;
          senderAlive = false;
        }
      }
    }
  }
}
