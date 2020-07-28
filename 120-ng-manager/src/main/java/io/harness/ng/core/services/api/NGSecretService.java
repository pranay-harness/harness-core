package io.harness.ng.core.services.api;

import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretTextCreateDTO;
import io.harness.secretmanagerclient.dto.SecretTextUpdateDTO;

import java.util.List;

public interface NGSecretService {
  EncryptedDataDTO getSecret(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  List<EncryptedDataDTO> listSecrets(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, SecretType secretType);

  EncryptedDataDTO createSecret(SecretTextCreateDTO dto);

  boolean updateSecret(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier,
      SecretTextUpdateDTO dto);

  boolean deleteSecret(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
}
