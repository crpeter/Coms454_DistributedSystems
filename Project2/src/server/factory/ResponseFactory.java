package src.server.factory;

public class ResponseFactory {
  
  public static String createWriteResponse(long lastModified) {
    String responseMsg = "0" + "&" + 1 + "&" + lastModified;

    return responseMsg;
  }

  public static String createReadResponse(long lastModified, byte[] data) {
    String responseMsg = "2" + "&" + lastModified + "&" + new String(data);

    return responseMsg;
  }

  public static String createLookupFileResponse(boolean exists, Long lastModified, Long size) {
    String responseMsg;

    if (exists) {
      responseMsg = "1" + "&" + size + "&" + lastModified;
    } else {
      responseMsg = "-1";
    }

    return responseMsg;
  }

  public static String createLastModifiedResponse(long lastModified) {
    String responseMsg = "3&" + lastModified;

    return responseMsg;
  }
}
