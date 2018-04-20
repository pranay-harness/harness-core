package software.wings.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import javax.xml.ws.soap.SOAPFaultException;

public class WinRmHelperUtil {
  public static String HandleWinRmClientException(Throwable e) {
    String message = "Generic Error";
    Throwable e1 = e;

    while (e1 != null) {
      if (e1 instanceof InvocationTargetException) {
        e1 = ((InvocationTargetException) e1).getTargetException();
        continue;
      }
      if (e1 instanceof UndeclaredThrowableException) {
        e1 = ((UndeclaredThrowableException) e1).getUndeclaredThrowable();
        continue;
      }
      if (e1 instanceof java.net.ConnectException) {
        message = String.format("Cannot reach remote host. Details: %s", e1.getMessage());
        break;
      } else if (e1 instanceof javax.net.ssl.SSLHandshakeException) {
        message = "Error in SSL negotiation. Check if server certificate is correct. You may try with skipCertChecks";
        break;
      } else if (e1 instanceof SOAPFaultException) {
        message = e1.getMessage();
        break;
      } else if (e1 instanceof java.io.IOException) {
        if (e1.getMessage().contains("Authorization")) {
          message = "Authorization Error: Invalid credentials. Check AuthenticationScheme, username and password.";
        } else {
          message = e1.getMessage();
        }
        break;
      }
      e1 = e1.getCause();
    }
    return message;
  }
}
