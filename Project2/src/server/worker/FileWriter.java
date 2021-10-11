package src.server.worker;

import src.server.service.WriteQueueService;

public class FileWriter extends Thread{
  private final WriteQueueService sharedWriteQueueService;

  public FileWriter(WriteQueueService writeQueueService) {
    sharedWriteQueueService = writeQueueService;
  }

  public synchronized void run() {
    
  }
}
