package src.server.worker;

import java.io.*;
import java.net.*;

import src.server.service.cache.WriteQueueService;
import src.server.service.io.FileRequestService;

// Connection runner class
public class Connection extends Thread {
  DataInputStream in;
  DataOutputStream out;
  Socket clientSocket;

  // ref to queue instance shared with all connections
  private final WriteQueueService sharedWriteQueueService;

  public Connection (Socket aClientSocket, WriteQueueService writeQueueService) {
    sharedWriteQueueService = writeQueueService;

    try {
      clientSocket = aClientSocket;
      in = new DataInputStream( clientSocket.getInputStream());
      out = new DataOutputStream( clientSocket.getOutputStream());
      this.start();
    } catch(IOException e) {
      System.out.println("Connection:" + e.getMessage());
    }
  }

  // run: listens for data and sends to handleRequest and handles errors
  public synchronized void run() {
    try {
      String data = in.readUTF();
      String response = FileRequestService.handleRequest(data, sharedWriteQueueService);
      out.writeUTF(response);
      System.out.println("server wrote:" + response);
    } catch(EOFException e) {
      System.out.println("EOF:" + e.getLocalizedMessage() + " " + e);
    } catch(IOException e) {
      System.out.println("IO:" + e.getLocalizedMessage() + " " + e);
    } finally { 
      try {
        clientSocket.close();
      } catch (IOException e){
        System.out.println("close:" + e.getMessage());
      }
    }
  }
}