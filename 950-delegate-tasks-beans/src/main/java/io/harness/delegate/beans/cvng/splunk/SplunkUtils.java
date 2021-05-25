package io.harness.delegate.beans.cvng.splunk;

import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;

import java.nio.charset.Charset;
import java.util.Base64;

public class SplunkUtils {
  private SplunkUtils() {}

  public static String getAuthorizationHeader(SplunkConnectorDTO splunkConnectorDTO) {
    String decryptedPassword = new String(splunkConnectorDTO.getPasswordRef().getDecryptedValue());
    String usernameColonPassword = splunkConnectorDTO.getUsername().concat(":").concat(decryptedPassword);
    return "Basic " + Base64.getEncoder().encodeToString(usernameColonPassword.getBytes(Charset.forName("UTF-8")));
  }
}
