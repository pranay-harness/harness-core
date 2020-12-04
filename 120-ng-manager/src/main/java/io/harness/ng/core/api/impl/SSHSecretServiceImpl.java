package io.harness.ng.core.api.impl;

import static io.harness.NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER;

import io.harness.ng.core.api.SecretModifyService;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class SSHSecretServiceImpl implements SecretModifyService {
  private final SecretManagerClient secretManagerClient;

  @Override
  public EncryptedDataDTO create(String accountIdentifier, SecretDTOV2 dto) {
    // no need to make a call to 71-rest for ssh secrets
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
  public boolean update(String accountIdentifier, SecretDTOV2 dto) {
    // no need to make a rest call to 71-rest for ssh secrets
    return true;
  }

  @Override
  public boolean updateViaYaml(String accountIdentifier, SecretDTOV2 dto) {
    // no need to make a call to 71-rest for ssh secrets
    return true;
  }
}
