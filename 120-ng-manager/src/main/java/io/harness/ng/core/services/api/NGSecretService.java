package io.harness.ng.core.services.api;

import software.wings.security.encryption.EncryptedData;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;

public interface NGSecretService {
  EncryptedData getSecretById(String accountId, String id);

  List<EncryptedData> getSecretsByType(String accountId, SettingVariableTypes type, boolean includeDetails);
}
