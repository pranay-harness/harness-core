/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
