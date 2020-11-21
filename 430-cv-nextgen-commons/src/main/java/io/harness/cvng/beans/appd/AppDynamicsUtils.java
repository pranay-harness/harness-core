package io.harness.cvng.beans.appd;

import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;

import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.binary.Base64;

public class AppDynamicsUtils {
  private AppDynamicsUtils() {}

  public static String getAuthorizationHeader(AppDynamicsConnectorDTO appDynamicsConnectorDTO) {
    return "Basic "
        + Base64.encodeBase64String(
            String
                .format("%s@%s:%s", appDynamicsConnectorDTO.getUsername(), appDynamicsConnectorDTO.getAccountname(),
                    new String(appDynamicsConnectorDTO.getPasswordRef().getDecryptedValue()))
                .getBytes(StandardCharsets.UTF_8));
  }
}
