package io.harness.ng.core.api;

import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;

public interface SecretModifyService {
  EncryptedDataDTO create(String accountIdentifier, SecretDTOV2 dto);

  void validateUpdateRequest(SecretDTOV2 existingSecret, SecretDTOV2 dto);

  boolean update(String accountIdentifier, SecretDTOV2 existingSecret, SecretDTOV2 dto);

  boolean updateViaYaml(String accountIdentifier, SecretDTOV2 existingSecret, SecretDTOV2 dto);
}
