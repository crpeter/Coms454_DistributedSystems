package src.server.worker;

import src.server.service.cache.WriteQueueService;

public class FileWriter extends Thread {
  private final WriteQueueService sharedWriteQueueService;

  public FileWriter(WriteQueueService writeQueueService) {
    sharedWriteQueueService = writeQueueService;
  }

  public synchronized void run() {
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      System.out.println(e);
    }

    
  }
}
