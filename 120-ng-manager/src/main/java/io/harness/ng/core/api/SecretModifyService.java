package io.harness.ng.core.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;

@OwnedBy(PL)
public interface SecretModifyService {
  EncryptedDataDTO create(String accountIdentifier, SecretDTOV2 dto);

  void validateUpdateRequest(SecretDTOV2 existingSecret, SecretDTOV2 dto);

  boolean update(String accountIdentifier, SecretDTOV2 existingSecret, SecretDTOV2 dto);

  boolean updateViaYaml(String accountIdentifier, SecretDTOV2 existingSecret, SecretDTOV2 dto);
}
