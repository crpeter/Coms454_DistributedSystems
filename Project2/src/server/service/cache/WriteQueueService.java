package src.server.service.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

import src.server.model.request.WriteRequest;

public class WriteQueueService {
  private final ConcurrentHashMap<String, List<WriteRequest>> writeRequestMap;

  public WriteQueueService() {
    writeRequestMap = new ConcurrentHashMap<>();
  }

  public synchronized void addWriteRequestListFor(WriteRequest writeRequest) {
    String fileName = writeRequest.fileName;
    if (writeRequestMap.get(fileName) == null) {
      List<WriteRequest> requestList = new ArrayList<>();
      requestList.add(writeRequest);
      writeRequestMap.put(fileName, requestList);
    } else {
      writeRequestMap.get(fileName).add(writeRequest);
    }
  }

  // todo - implement
  // private synchronized List<WriteRequest> getWriteRequests() {
    
  // }
}
