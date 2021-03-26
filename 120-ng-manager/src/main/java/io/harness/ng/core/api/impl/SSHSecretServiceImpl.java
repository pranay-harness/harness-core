package io.harness.ng.core.api.impl;

import static io.harness.NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.api.SecretModifyService;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class SSHSecretServiceImpl implements SecretModifyService {
  @Override
  public EncryptedDataDTO create(String accountIdentifier, SecretDTOV2 dto) {
    // no need to make a call to 400-rest for ssh secrets
    return EncryptedDataDTO.builder()
        .name(dto.getName())
        .account(accountIdentifier)
        .org(dto.getOrgIdentifier())
        .project(dto.getProjectIdentifier())
        .description(dto.getDescription())
        .identifier(dto.getIdentifier())
        .tags(null)
        .type(SecretType.SSHKey)
        .secretManager(HARNESS_SECRET_MANAGER_IDENTIFIER)
        .build();
  }

  @Override
  public void validateUpdateRequest(SecretDTOV2 existingSecret, SecretDTOV2 dto) {
    // pass, nothing required to validate, but we can apply future validations like we cannot switch between auth types
  }

  @Override
  public boolean update(String accountIdentifier, SecretDTOV2 existingSecret, SecretDTOV2 dto) {
    validateUpdateRequest(existingSecret, dto);

    // no need to make a rest call to 400-rest for ssh secrets
    return true;
  }

  @Override
  public boolean updateViaYaml(String accountIdentifier, SecretDTOV2 existingSecret, SecretDTOV2 dto) {
    validateUpdateRequest(existingSecret, dto);

    // no need to make a call to 400-rest for ssh secrets
    return true;
  }
}
