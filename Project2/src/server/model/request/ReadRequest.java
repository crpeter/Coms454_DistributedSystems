package src.server.model.request;

public class ReadRequest extends Request {
  public final int offset;
  public final int requestedBytes;

  public ReadRequest(String fileName, Integer offset, int requestedBytes) {
    super(fileName, RequestType.READ);

    this.offset = offset;
    this.requestedBytes = requestedBytes;
  }
}
