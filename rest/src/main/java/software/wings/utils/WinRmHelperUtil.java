package software.wings.utils;

import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.eraro.ErrorCode.SSL_HANDSHAKE_FAILED;
import static io.harness.eraro.ErrorCode.UNKNOWN_ERROR;
import static io.harness.eraro.ErrorCode.UNREACHABLE_HOST;
import static java.lang.String.format;
import static software.wings.beans.ResponseMessage.ResponseMessageBuilder;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.wings.beans.ResponseMessage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import javax.xml.ws.soap.SOAPFaultException;

public class WinRmHelperUtil {
  @SuppressFBWarnings("NM_METHOD_NAMING_CONVENTION")
  public static ResponseMessage GetErrorDetailsFromWinRmClientException(Throwable e) {
    ResponseMessageBuilder builder = ResponseMessage.aResponseMessage().code(UNKNOWN_ERROR).message("Generic Error");
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
        builder.code(UNREACHABLE_HOST);
        builder.message(format("Cannot reach remote host. Details: %s", e1.getMessage()));
        break;
      } else if (e1 instanceof javax.net.ssl.SSLHandshakeException) {
        builder.code(SSL_HANDSHAKE_FAILED);
        builder.message(
            "Error in SSL negotiation. Check if server certificate is correct. You may try with skipCertChecks");
        break;
      } else if (e1 instanceof SOAPFaultException) {
        builder.message(e1.getMessage());
        break;
      } else if (e1 instanceof java.io.IOException) {
        if (e1.getMessage().contains("Authorization")) {
          builder.code(INVALID_CREDENTIAL);
          builder.message(
              "Authorization Error: Invalid credentials. Check AuthenticationScheme, username and password");
        } else {
          builder.message(e1.getMessage());
        }
        break;
      }
      e1 = e1.getCause();
    }
    return builder.build();
  }
}
