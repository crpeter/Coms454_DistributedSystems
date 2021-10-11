package src.server;

import java.net.*;
import java.io.*;

import src.server.service.WriteQueueService;
import src.server.worker.Connection;

public class FileServer { 
    private Socket          socket   = null; 
    private ServerSocket    server   = null; 
    private DataInputStream in       = null;

    private int MSGSIZE = 40;

    private final WriteQueueService writeQueueService;

    public FileServer(int port) { 
      writeQueueService = new WriteQueueService();
      
      // starts server and waits for a connection 
      try {
        server = new ServerSocket(port); 

        System.out.printf("Server started on port: %d\n", port); 
        System.out.println("Waiting for a client ..."); 

        // Start Connection instance thread repeadedly, each blocks until message is received
        while (true) {
          Socket clientSocket = server.accept();
          Connection c = new Connection(clientSocket, writeQueueService);
          
        } 
      } 
      catch(IOException i) { 
        System.out.println(i); 
      }

      try {
        // close connection
        System.out.println("Closing connection"); 
        socket.close(); 
        in.close();
      } catch (Exception e) {
        System.out.println(e);
      }
    }
  
    // args[0] is optionally a port number default = 10044
    public static void main(String args[]) 
    { 
      String port = "10044";
      if (args.length > 0) {
        port = args[0];
      }
      FileServer server = new FileServer(Integer.parseInt(port));
    }
}