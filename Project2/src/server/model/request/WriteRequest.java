package src.server.model.request;

public class WriteRequest extends Request {
  public final int offset;
  public final byte[] data;

  public WriteRequest(String fileName, int offset, String data) {
    super(fileName, RequestType.WRITE);
    
    this.offset = offset;
    this.data = data.getBytes();
  }
}
