/**
 * Implementation of fileSystemAPI with socket using t file system calls. 
 */

/* standard java classes. */
import java.io.*;
import java.net.*;
import java.util.*;

public class ClientFileSystem implements fileSystemAPI { 
    private Hashtable<filehandle, byte[]> fileTbl = new Hashtable<>();

    //initialize socket and input stream 
    private Socket          socket   = null; 
    private ServerSocket    server   = null; 
    private DataInputStream in       = null; 

    private DataInputStream  input   = null; 
    private DataOutputStream out     = null;

    private String address = "";
    private int port = 0;

    public ClientFileSystem(String address, int port) {
      this.address = address;
      this.port = port;
    }
  
    // public static void main(String args[]) { 
    //     fileSystem server = new fileSystem("127.0.0.1", 10044); 
    // }

    /**
     * @params
     */
    public String[] sendTCP(String req) {
      try {
        // sends output to the socket 
        // System.out.println("Sending: " + req);

        socket = new Socket(address, port);
        out = new DataOutputStream(socket.getOutputStream());
        input = new DataInputStream(socket.getInputStream());

        out.writeUTF(req);
        
        //Get reesponse
        String data = input.readUTF();
        String[] dataSplit = data.split("&");

        socket.close();
        input.close(); 
        out.close();

        return dataSplit;
      }
      catch(IOException i) {
        System.out.println(i);
      }
      return null;
    }

    /* url SHOULD HAVE form IP:port/path, but here simply a file name.*/
    public filehandle open(String url) {
        filehandle fh = new filehandle();
        fh.url = url;
        fileTbl.put(fh, new byte[0]);

	      return fh;
    }

    public boolean write(filehandle fh, byte[] data)
	      throws java.io.IOException {
        String req = "0" + fh.url + "&" + fh.getPointer() + "&" + new String(data);
        sendTCP(req);
        
        fh.incrementPointer(data.length);
        
		    return true;
    }

    /* read bytes from the current position. returns the number of bytes read. */
    public int read(filehandle fh, byte[] data)
	      throws java.io.IOException {
        String req = "1" + fh.url + "&" + fh.getPointer() + "&" + data.length;
        String[] response = sendTCP(req);
        byte[] responseData = response[2].getBytes();

        for (int i = 0; i < data.length && i < responseData.length; i++) {
          data[i] = responseData[i];
        }
        
        fh.incrementPointer(data.length);
        
        return data.length;
    }

    /* close file. */  
    public boolean close(filehandle fh)
	      throws java.io.IOException {
        fileTbl.remove(fh);
        fh.discard();
        return true;
    }

    /* check if it is the end-of-file. */
    public boolean isEOF(filehandle fh)
	      throws java.io.IOException {
        String req = "2" + fh.url;
        String[] res = sendTCP(req);
        return Integer.parseInt(res[1]) < fh.getPointer();
    }
} 
    
	
