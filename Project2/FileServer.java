/* This is a simple example implementation of fileSystemAPI, 
   using local file system calls. 
*/

/* standard java classes. */
import java.io.*;
import java.net.*;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

// Connection runner class
class Connection extends Thread {
  DataInputStream in;
  DataOutputStream out;
  Socket clientSocket;

  public Connection (Socket aClientSocket) {
    try {
      clientSocket = aClientSocket;
      in = new DataInputStream( clientSocket.getInputStream());
      out = new DataOutputStream( clientSocket.getOutputStream());
      this.start();
    } catch(IOException e){
      System.out.println("Connection:" + e.getMessage());
    }
  }

  // run: listens for data and sends to handleRequest and handles errors
  public synchronized void run() {
    try {
      String data = in.readUTF();
      // System.out.println("server received: " + data);
      handleRequest(data);
    } catch(EOFException e) {
      System.out.println("EOF:" + e.getMessage());
    } catch(IOException e) {
      System.out.println("IO:" + e.getMessage());
    } finally { 
      try {
        clientSocket.close();
      } catch (IOException e){
        System.out.println("close:"+e.getMessage());
      }
    }
  }

  // handleRequest: run server functions {write, read, lookup, and getAttr}
  //                function to fun is the first char of requests
  private void handleRequest(String req) {
    int function = req.charAt(0);
    String[] reqSplit = req.split("&");
    String filename = reqSplit[0].split("/")[1];
    switch (function) {
      case '4':
      case '0':
        try {
          write(filename, 
                Integer.parseInt(reqSplit[1]), 
                reqSplit[2]);
        } catch (IOException e) {
          System.out.println("error parsing offset for write: " + e);
        }
        break;
      case '1':
        try {
          read(filename, 
                Integer.parseInt(reqSplit[1]), 
                Integer.parseInt(reqSplit[2]));
        } catch (IOException e) {
          System.out.println("error parsing offset for read: " + e);
        }
        break;
      case '2':
        lookup(req.substring(1, req.length()));
        break;
      case '3':
        getAttr(filename);
        break;
      default:
        break;
    }
  }

  // lookup file from url, send "1&<size>&<lastmodified> if file exists
  //                       else send "-1"
  private void lookup(String url) {
    String filename = url.split("/")[1];
    File tempFile = new File(filename);
    String responseMsg = "";
    if (tempFile.exists()) {
      long size = tempFile.length();
      long modified = tempFile.lastModified();

      responseMsg = "1" + "&" + size + "&" + modified;
    } else {
      responseMsg = "-1";
    }
    try {
      out.writeUTF(responseMsg);
      System.out.println("looked up: " + responseMsg);
    } catch (IOException e) {
      System.out.println("error responding: " + e);
    }
  }

  // write accepts filename, offset, and data to be written.
  // returns a response back through socket
  public void write(String filename, int offset, String data)
    throws java.io.IOException {
      File tempFile = new File(filename);

      // byte arrays for current byte content and recved bytes to insert at offset
      byte[] fileContent = Files.readAllBytes(tempFile.toPath());
      byte[] dataContent = data.getBytes();
      byte[] newFileContent;
      // offset will be -1 when cache is being flushed
      // if (offset == -1) {
      //   tempFile.delete();
      //   newFileContent = dataContent;
      // } else {
        tempFile.delete();
        newFileContent = new byte[fileContent.length + dataContent.length];
        if (offset > fileContent.length) {
          offset = fileContent.length;
        }
        int xIndex = 0;
        int yIndex = 0;
        for (int i = 0; i < newFileContent.length; i++) {
          if (i >= offset && xIndex < dataContent.length) {
            newFileContent[i] = dataContent[xIndex++];
          } else {
            newFileContent[i] = fileContent[yIndex++];
          }
        }
        System.out.printf("newFilecontent length: %d, dataContent: %d, fileContent: %d", 
            newFileContent.length, dataContent.length, fileContent.length);
      //}

      try {
        // Create file output stream and write bytes from newFileContent
        FileOutputStream fos = new FileOutputStream(filename);
        fos.write(newFileContent);
        fos.close();
        File tempFile2 = new File(filename);
        System.out.println("Wrote bytes: " + newFileContent.length + " file length: " + tempFile2.length());
      } catch (IOException e) {
        System.out.println("error writing bytes to file: " + e);
      }

      while (tempFile.length() != (long) newFileContent.length) {
        try {
          Thread.sleep(100);
        } catch (Exception e) {
          System.out.println(e);
        }
      }
      
      // Respond with 0 for response to a write and 1 for success and last modified
      String responseMsg = "0" + "&" + 1 + "&" + tempFile.lastModified();
      try {
        out.writeUTF(responseMsg);
        // System.out.println("sent response: " + responseMsg);
      } catch (IOException e) {
        System.out.println("error responding: " + e);
      }
  }

  // read bytes from the current position. returns the number of bytes read.
  // sends read data back to client
  public void read(String filename, int offset, int n)
    throws java.io.IOException {
      File tempFile = new File(filename);

      byte[] fileContent = Files.readAllBytes(tempFile.toPath());
      byte[] data = new byte[n];

      int dataIndex = 0;
      for (int i = offset; i < fileContent.length && dataIndex < n; i++) {
        data[dataIndex++] = fileContent[i];
      }

      String responseMsg = "";
      responseMsg += "2" + "&" + tempFile.lastModified() + "&" + new String(data);

      try {
        out.writeUTF(responseMsg);
        System.out.println("read bytes: " + dataIndex);
      } catch (IOException e) {
        System.out.println("error responding: " + e);
      }
  }

  // getAttr takes filename and responds with lastmodified to client
  public void getAttr(String filename) {
    File tempFile = new File(filename);

    String responseMsg = "3&" + tempFile.lastModified();
    try{
      out.writeUTF(responseMsg);
      System.out.println("attr: " + responseMsg);
    } catch (IOException e) {
      System.out.println("error responding: " + e);
    }
  }
}

// File server class runs Connection class
public class FileServer { 
    //initialize socket and input stream 
    private Socket          socket   = null; 
    private ServerSocket    server   = null; 
    private DataInputStream in       = null;

    private int MSGSIZE = 40;

    public FileServer(int port) { 
        // starts server and waits for a connection 
        try {
          server = new ServerSocket(port); 

          System.out.printf("Server started on port: %d\n", port); 
          System.out.println("Waiting for a client ..."); 

          // Start Connection instance thread repeadedly, each blocks until message is received
          while(true) {
            Socket clientSocket = server.accept();
            Connection c = new Connection(clientSocket);
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
    
	
