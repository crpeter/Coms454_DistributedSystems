package src.server.model.request;

public class Request {
  public final String fileName;
  public final RequestType requestType;

  public Request(String fileName, RequestType requestType) {
    this.fileName = fileName;
    this.requestType = requestType;
  }
}
