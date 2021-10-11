package src.server.factory;

import src.server.model.request.Request;
import src.server.model.request.RequestType;
import src.server.model.request.ReadRequest;
import src.server.model.request.WriteRequest;

public class RequestFactory {
  
  public static Request creatRequest(String rawRequest) {
    System.out.println("raw request:" + rawRequest);
    int function = rawRequest.charAt(0);
    
    String[] reqSplit = rawRequest.split("&");
    String fileName = reqSplit[0].split("/")[1];

    Request request;
    
    switch (function) {
      case '4':
      case '0':
        String data = reqSplit[2];
        int writeOffset = Integer.parseInt(reqSplit[1]);
        request = new WriteRequest(fileName, writeOffset, data);
        break;
      case '1':
        Integer requestedBytes = Integer.parseInt(reqSplit[2]);
        int readOffset = Integer.parseInt(reqSplit[1]);
        request = new ReadRequest(fileName, readOffset, requestedBytes);
        break;
      case '2':
        request = new Request(fileName, RequestType.LOOKUP);
        break;
      case '3':
        request = new Request(fileName, RequestType.GET_ATTRIBUTE);
        break;
      default:
        request = null;
        break;
    }

    return request;
  }
}
