package io.harness.ng.service;

import io.harness.encryption.SecretRefData;
import io.harness.ng.core.NGAccess;

public interface SecretRefService {
  SecretRefData createSecretRef(String secretConfigString);
  String validateAndGetSecretConfigString(SecretRefData secretRefData, NGAccess ngAccess);
}
