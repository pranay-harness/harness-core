package io.harness.ng.core.services.api;

import io.harness.encryption.SecretType;
import io.harness.ng.core.dto.EncryptedDataDTO;
import io.harness.ng.core.dto.SecretTextDTO;

import java.util.List;

public interface NGSecretService {
  EncryptedDataDTO getSecretById(String accountId, String id);

  List<EncryptedDataDTO> getSecretsByType(String accountId, SecretType secretType);

  String createSecret(String accountId, boolean localMode, SecretTextDTO secretText);

  boolean updateSecret(String accountId, String uuid, SecretTextDTO secretText);

  boolean deleteSecret(String accountId, String uuId);
}
