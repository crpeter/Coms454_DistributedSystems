package src.server.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import src.server.factory.ResponseFactory;
import src.server.model.request.WriteRequest;

public class FileWriteService {
  private static final String RESOURCE_DIR = "/Users/petercody/Documents/Projects/DistributedSystemsProjects/Project2/src/server/resources/";

  // write accepts filename, offset, and data to be written.
  public static String write(WriteRequest writeRequest) {
    String filePath = RESOURCE_DIR + writeRequest.fileName;
    byte[] writeData = writeRequest.data;
    int writeOffset = writeRequest.offset;

    File tempFile = new File(filePath);
    
    // byte arrays for current byte content and recved bytes to insert at offset
    byte[] fileContent = new byte[0];
    try {
      fileContent = Files.readAllBytes(tempFile.toPath());
    } catch (IOException e) {
      System.out.println(e);
    }
    byte[] newFileContent = createNewFileContent(fileContent, writeData, writeData.length, writeOffset);
    
    tempFile.delete();

    File updatedFile = updateFile(filePath, newFileContent);
    
    // Respond with 0 for response to a write and 1 for success and last modified
    String responseMsg = ResponseFactory.createWriteResponse(updatedFile.lastModified());

    return responseMsg;
  }

  private static byte[] createNewFileContent(byte[] fileContent, byte[] writeData, int writeDataLength, int writeOffset) {
    byte[] newFileContent;
    
    newFileContent = new byte[fileContent.length + writeDataLength];
    if (writeOffset > fileContent.length) {
      writeOffset = fileContent.length;
    }
    int xIndex = 0;
    int yIndex = 0;
    for (int i = 0; i < newFileContent.length; i++) {
      if (i >= writeOffset && xIndex < writeDataLength) {
        newFileContent[i] = writeData[xIndex++];
      } else {
        newFileContent[i] = fileContent[yIndex++];
      }
    }
    System.out.printf("\nnewFilecontent length: %d, dataContent: %d, fileContent: %d\n", 
        newFileContent.length, writeData.length, fileContent.length);

    return newFileContent;
  }

  private static File updateFile(String filePath, byte[] newFileContent) {
    File updatedFile = null;
    
    try {
      // Create file output stream and write bytes from newFileContent
      FileOutputStream fos = new FileOutputStream(filePath);
      fos.write(newFileContent);
      fos.close();
      updatedFile = new File(filePath);
    } catch (IOException e) {
      System.out.println("error writing bytes to file: " + e);
    }

    while (updatedFile != null && updatedFile.length() != (long) newFileContent.length) {
      try {
        Thread.sleep(100);
      } catch (Exception e) {
        System.out.println(e);
      }
    }

    if (updatedFile != null)
      System.out.println("Wrote bytes: " + newFileContent.length + " file length: " + updatedFile.length());
    
    return updatedFile;
  }

}
