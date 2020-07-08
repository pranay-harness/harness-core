package io.harness.ng.core.services.api;

import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretText;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;

public interface NGSecretService {
  EncryptedData getSecretById(String accountId, String id);

  List<EncryptedData> getSecretsByType(String accountId, SettingVariableTypes type, boolean includeDetails);

  String createSecret(String accountId, boolean localMode, SecretText secretText);

  boolean updateSecret(String accountId, String uuid, SecretText secretText);

  boolean deleteSecret(String accountId, String uuId);
}
