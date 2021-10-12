package src.server.service.io;

import java.io.IOException;

import src.server.factory.RequestFactory;
import src.server.model.request.ReadRequest;
import src.server.model.request.Request;
import src.server.model.request.WriteRequest;
import src.server.service.FileWriteService;
import src.server.service.cache.WriteQueueService;

public class FileRequestService {

  // handleRequest: run server functions {write, read, lookup, and getAttr}
  //                function to fun is the first char of requests
  public static String handleRequest(String rawRequest, WriteQueueService writeQueueService) throws IOException {
    String response;
    
    Request request = RequestFactory.creatRequest(rawRequest);
    switch (request.requestType) {
      case WRITE:
        // writeQueueService.addWriteRequestListFor((WriteRequest) request);
        response = FileWriteService.write((WriteRequest) request);
        break;
      case READ:
        response = FileReadService.read((ReadRequest) request);
        break;
      case LOOKUP:
        response = FileReadService.lookUp(request);
        break;
      case GET_ATTRIBUTE:
        response = FileReadService.getAttr(request);
        break;
      default:
        response = "Unrecognized function";
        break;
    }

    return response;
  }
}
