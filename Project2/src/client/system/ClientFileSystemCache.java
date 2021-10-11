package src.client.system;

import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.util.*;

import src.client.model.FileHandle;

// Implementation of fileSystemAPI with a cache
public class ClientFileSystemCache implements FileSystemAPI {
  // Class to represent a buffered write in cache with data and offset
  public class BufferedData {
    public int offset = 0;
    public byte[] data;
    public BufferedData() {}
    public BufferedData clone() {
      BufferedData temp = new BufferedData();
      temp.offset = this.offset;
      temp.data = this.data.clone();
      return temp;
    }
  }
  // Class to represent the cached data for a filehandle
  private class CachedFileData {
    private byte[] readData;
    private ArrayList<BufferedData> writeData = new ArrayList<>();

    public CachedFileData() {}
    public CachedFileData(byte[] data) {
      readData = data.clone();
    }

    public void setReadData(byte[] data) {
      readData = data.clone();
    }

    public void writeToBuffer(byte[] data, int offset) {
      BufferedData newData = new BufferedData();
      newData.offset = offset;
      newData.data = data.clone();
      writeData.add(newData);
    }

    public ArrayList<BufferedData> getBuffer() {
      ArrayList<BufferedData> temp = new ArrayList<>();
      for (BufferedData data : writeData) {
        temp.add(data.clone());
      }
      return temp;
    }

    public byte[] getReadData() {
      return readData.clone();
    }

    public int readDataLength() {
      if (readData == null) {
        return 0;
      }
      return readData.length;
    }
  }
  // END CachedFileData class
  private Hashtable<FileHandle, CachedFileData> fileTbl = new Hashtable<>();

  //initialize socket and input stream 
  private Socket          socket   = null; 
  private DataInputStream  input   = null; 
  private DataOutputStream out     = null;

  private String address = "";
  private int port = 0;
  private boolean hasWritten = false;
  private int READSIZE = 32768;

  public ClientFileSystemCache(String address, int port) {
    this.address = address;
    this.port = port;
  } 

  /**
   * @params reg will be formatted before being passed
   * open and close sockets and input/output stream each function call
   */
  public String[] sendTCP(String req) {
    try {
      // sends output to the socket
      socket = new Socket(address, port);
      out = new DataOutputStream(socket.getOutputStream());
      input = new DataInputStream(socket.getInputStream());

      out.writeUTF(req);
      
      //Get response
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

  /**
   * url SHOULD HAVE form IP:port/path, but here simply a file name.
   * read through file and add to cacheß
   **/
  public FileHandle open(String url) {
    FileHandle fh = new FileHandle();
    fh.url = url;
    fileTbl.put(fh, new CachedFileData());

    // Cache file for reading
    try {
      read(fh, new byte[0]);
    } catch (IOException e) {
      System.out.println("Error opening file");
    }

    return fh;
  }

  /**
   * write takes filehandle and data to be written
   * check if file is cached, if not cache it
   * write data to cache at pointer
   */
  public boolean write(FileHandle fh, byte[] data) throws java.io.IOException {
    if (fileTbl.get(fh).readDataLength() == 0) {
      // Send a 2 to lookup file and get size and last modified
      String req = "2" + fh.url;
      String[] res = sendTCP(req);

      int size = Integer.parseInt(res[1]);

      // Send a 1 to read all bytes of file to store in the cache
      req = "1" + fh.url + "&" + 0 + "&" + size;
      String[] response = sendTCP(req);
      byte[] responseData = response[2].getBytes();

      fileTbl.put(fh, new CachedFileData(responseData));
      // System.out.printf("put: %d bytes in fh: %d\n", responseData.length, fh.index);
    }

    // write data to buffer
    fileTbl.get(fh).writeToBuffer(data, fh.getPointer());
    fh.incrementPointer(data.length);
    
    hasWritten = true;
    return true;
  }

  /**
   * read data.length amount of bytes from fh on server system
   */
  public int read(FileHandle fh, byte[] data) throws java.io.IOException {
    // Send a 2 to lookup file and get size and last modified
    String req = "2" + fh.url;
    String[] res = sendTCP(req);

    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    long currentTime = timestamp.getTime();
    
    // If cache is stale or fh has no cached data
    if (Long.parseLong(res[2]) > currentTime || fileTbl.get(fh).readDataLength() == 0) {
      int size = Integer.parseInt(res[1]);
      int readBytes = 0;
      byte[] recvedData = new byte[size];
      while (readBytes < size) {
        int bytesToRead = (readBytes + READSIZE > size ? size - readBytes : READSIZE);
        // Send a 1 to read all bytes of file to store in the cache
        // results of writes are stored in 2 param of response
        req = "1" + fh.url + "&" + fh.getPointer() + "&" + bytesToRead;
        String[] response = sendTCP(req);
        byte[] respData = response[2].getBytes();
        readBytes += respData.length;
        int respI = 0;
        for (int i = fh.getPointer(); i < fh.getPointer() + respData.length; i++) {
          recvedData[i] = respData[respI++];
        }
        fh.incrementPointer(respData.length);
      }
      fh.resetPointer();
      fileTbl.get(fh).setReadData(recvedData);

      fh.readToCache = true;
      // System.out.printf("put: %d bytes in fh: %d\n", responseData.length, fh.index);
    }
    // save read bytes from cache
    byte[] cachedData = fileTbl.get(fh).getReadData();
    int dataIndex = 0;
    for (int i = fh.getPointer(); i < cachedData.length && dataIndex < data.length; i++) {
      data[dataIndex++] = cachedData[i];
    }
    fh.incrementPointer(dataIndex);
    // System.out.printf("read %d bytes from cache, pointer: %d:\n\n", dataIndex, fh.getPointer());
    return data.length;
  }

  /**
   * Close file
   * if file has been written to
   * if byte array is larger than max socket send length 
   * max send size is sent until all bytes have been sent
   **/ 
  public boolean close(FileHandle fh) throws java.io.IOException {
    if (hasWritten) {
      // get file data in byte array
      ArrayList<BufferedData> fileContents = fileTbl.get(fh).getBuffer();
      // int offset = 0;
      for (BufferedData write : fileContents) {
        // Send a 4 to write bytes in write.data starting at offset write.offset
        String req = "4" + fh.url + "&" + write.offset + "&" + new String(write.data);
        sendTCP(req);
      }
    }

    fileTbl.remove(fh);
    fh.discard();
    return true;
  }

  /**
   * returns true if fh pointer is greater than or equal to 
   * length of cached data
   */
  public boolean isEOF(FileHandle fh) throws java.io.IOException {
    //
    byte[] cachedData = fileTbl.get(fh).getReadData();
    if (cachedData.length != 0) {
      return fh.getPointer() >= cachedData.length;
    } else {
      return true;
    }
  }
}
    
	
