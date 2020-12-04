package io.harness.ng.core.api.impl;

import io.harness.ng.core.api.SecretModifyService;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretFileSpecDTO;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class SecretFileServiceImpl implements SecretModifyService {
  private final SecretManagerClient secretManagerClient;

  @Override
  public EncryptedDataDTO create(String accountIdentifier, SecretDTOV2 dto) {
    // no need to make a call to 71-rest in case of creating file without any content
    SecretFileSpecDTO specDTO = (SecretFileSpecDTO) dto.getSpec();
    return EncryptedDataDTO.builder()
        .account(accountIdentifier)
        .org(dto.getOrgIdentifier())
        .project(dto.getProjectIdentifier())
        .identifier(dto.getIdentifier())
        .name(dto.getName())
        .description(dto.getDescription())
        .tags(null)
        .secretManager(specDTO.getSecretManagerIdentifier())
        .type(dto.getType())
        .build();
  }

  @Override
  public boolean update(String accountIdentifier, SecretDTOV2 dto) {
    // no need to make a call to 71-rest in case of updating file without any content
    return true;
  }

  @Override
  public boolean updateViaYaml(String accountIdentifier, SecretDTOV2 dto) {
    // no need to make a call to 71-rest in case of updating file without any content
    return true;
  }
}
