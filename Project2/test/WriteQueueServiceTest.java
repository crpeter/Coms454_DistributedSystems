package test;

import src.server.model.request.WriteRequest;
import src.server.service.WriteQueueService;

public class WriteQueueServiceTest {

  public static void main(String argv[]) {
    WriteQueueService writeQueueService = new WriteQueueService();

    writeQueueService.addWriteRequestListFor(new WriteRequest("file1", 0, "file1 first line"));
    writeQueueService.addWriteRequestListFor(new WriteRequest("file1", 0, "\nfile1 second line"));

    writeQueueService.addWriteRequestListFor(new WriteRequest("file2", 0, "file2 is only one line"));

    System.out.println("WriteQueueServiceTest Success");
  }
}
