package io.harness.connector.mappers;

import io.harness.encryption.SecretRefData;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SecretRefHelper {
  public SecretRefData createSecretRef(String secretConfigString) {
    return new SecretRefData(secretConfigString);
  }

  public String getSecretConfigString(SecretRefData secretRefData) {
    if (secretRefData == null) {
      return null;
    }
    return secretRefData.toSecretRefStringValue();
  }
}
