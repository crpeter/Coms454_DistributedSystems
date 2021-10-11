package src.server.service;

import java.io.File;
import java.nio.file.Files;

import src.server.factory.ResponseFactory;
import src.server.model.request.Request;
import src.server.model.request.ReadRequest;

public class FileIOService {

  private static final String RESOURCE_DIR = "/Users/petercody/Documents/Projects/DistributedSystemsProjects/Project2/src/server/resources/";

  // read bytes from the current position. returns the number of bytes read.
  // sends read data back to client
  public static String read(ReadRequest readRequest) throws java.io.IOException {
    String filename = readRequest.fileName;
    int offset = readRequest.offset;
    int requestedBytes = readRequest.requestedBytes;

    File tempFile = new File(RESOURCE_DIR + filename);

    byte[] fileContent = Files.readAllBytes(tempFile.toPath());
    byte[] data = new byte[requestedBytes];

    int dataIndex = 0;
    for (int i = offset; i < fileContent.length && dataIndex < requestedBytes; i++) {
      data[dataIndex++] = fileContent[i];
    }

    String responseMsg = ResponseFactory.createReadResponse(tempFile.lastModified(), data);

    return responseMsg;
  }

  // lookup file from url, send "1&<size>&<lastmodified> if file exists else send "-1"
  public static String lookUp(Request request) {
    File tempFile = new File(RESOURCE_DIR + request.fileName);
    String responseMsg = ResponseFactory.createLookupFileResponse(tempFile.exists(), tempFile.lastModified(), tempFile.length());
    
    return responseMsg;
  }

  // getAttr takes filename and responds with lastmodified to client
  public static String getAttr(Request request) {
    File tempFile = new File(RESOURCE_DIR + request.fileName);

    String responseMsg = ResponseFactory.createLastModifiedResponse(tempFile.lastModified());

    return responseMsg;
  }
}
