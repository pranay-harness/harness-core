package io.harness.encryption;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SecretRefHelper {
  public SecretRefData createSecretRef(String secretConfigString) {
    return new SecretRefData(secretConfigString);
  }

  public SecretRefData createSecretRef(String secretConfigString, Scope scope, char[] decryptedValue) {
    return new SecretRefData(secretConfigString, scope, decryptedValue);
  }

  public String getSecretConfigString(SecretRefData secretRefData) {
    if (secretRefData == null) {
      return null;
    }
    return secretRefData.toSecretRefStringValue();
  }
}
