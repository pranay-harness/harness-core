package software.wings.utils;

public class WinRmHelperUtil {
  public static String HandleWinRmClientException(Throwable e) {
    String message = "Generic Error";
    Throwable e1 = e;
    while (e1.getCause() != null) {
      e1 = e1.getCause();
      if (e1 instanceof java.net.ConnectException) {
        message = String.format("Cannot reach remote host. Details: %s", e1.getMessage());
        break;
      } else if (e1 instanceof javax.net.ssl.SSLHandshakeException) {
        message = "Error in SSL negotiation. Check if server certificate is correct. You may try with skipCertChecks";
        break;
      } else if (e1 instanceof java.io.IOException) {
        if (e1.getMessage().contains("Authorization")) {
          message = "Authorization Error: Invalid credentials. Check AuthenticationScheme, username and password.";
        } else {
          message = e1.getMessage();
        }
        break;
      }
    }
    return message;
  }
}
