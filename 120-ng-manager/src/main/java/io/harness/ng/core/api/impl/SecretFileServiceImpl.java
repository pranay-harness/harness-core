package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.INVALID_REQUEST;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.api.SecretModifyService;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretFileSpecDTO;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class SecretFileServiceImpl implements SecretModifyService {
  @Override
  public EncryptedDataDTO create(String accountIdentifier, SecretDTOV2 dto) {
    // no need to make a call to 400-rest in case of creating file without any content
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
  public void validateUpdateRequest(SecretDTOV2 existingSecret, SecretDTOV2 dto) {
    SecretFileSpecDTO specDTO = (SecretFileSpecDTO) dto.getSpec();
    SecretFileSpecDTO existingSecretSpecDTO = (SecretFileSpecDTO) existingSecret.getSpec();
    Optional.ofNullable(specDTO.getSecretManagerIdentifier())
        .filter(x -> x.equals(existingSecretSpecDTO.getSecretManagerIdentifier()))
        .orElseThrow(()
                         -> new InvalidRequestException(
                             "Cannot change secret manager after creation of secret file", INVALID_REQUEST, USER));
  }

  @Override
  public boolean update(String accountIdentifier, SecretDTOV2 existingSecret, SecretDTOV2 dto) {
    validateUpdateRequest(existingSecret, dto);

    // no need to make a call to 400-rest in case of updating file without any content
    return true;
  }

  @Override
  public boolean updateViaYaml(String accountIdentifier, SecretDTOV2 existingSecret, SecretDTOV2 dto) {
    validateUpdateRequest(existingSecret, dto);

    // no need to make a call to 400-rest in case of updating file without any content
    return true;
  }
}
