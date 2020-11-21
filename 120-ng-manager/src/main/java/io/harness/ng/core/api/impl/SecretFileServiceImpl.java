package io.harness.ng.core.api.impl;

import static io.harness.exception.WingsException.USER;
import static io.harness.remote.client.RestClientUtils.getResponse;

import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.api.SecretModifyService;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretFileSpecDTO;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretFileDTO;
import io.harness.secretmanagerclient.dto.SecretFileUpdateDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import io.harness.serializer.JsonUtils;

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
    SecretFileSpecDTO specDTO = (SecretFileSpecDTO) dto.getSpec();
    SecretFileDTO secretFileDTO = SecretFileDTO.builder()
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
    return getResponse(secretManagerClient.createSecretFile(getRequestBody(JsonUtils.asJson(secretFileDTO)), null));
  }

  @Override
  public boolean update(String accountIdentifier, SecretDTOV2 dto) {
    EncryptedDataDTO encryptedDataDTO = getResponse(secretManagerClient.getSecret(
        dto.getIdentifier(), accountIdentifier, dto.getOrgIdentifier(), dto.getProjectIdentifier()));
    SecretFileSpecDTO specDTO = (SecretFileSpecDTO) dto.getSpec();
    if (encryptedDataDTO == null || !specDTO.getSecretManagerIdentifier().equals(encryptedDataDTO.getSecretManager())) {
      throw new InvalidRequestException("Cannot update secret manager after creation of secret", USER);
    }
    SecretFileUpdateDTO updateDTO =
        SecretFileUpdateDTO.builder().name(dto.getName()).tags(null).description(dto.getDescription()).build();
    return getResponse(secretManagerClient.updateSecretFile(dto.getIdentifier(), accountIdentifier,
        dto.getOrgIdentifier(), dto.getProjectIdentifier(), null, getRequestBody(JsonUtils.asJson(updateDTO))));
  }

  @Override
  public boolean updateViaYaml(String accountIdentifier, SecretDTOV2 dto) {
    return update(accountIdentifier, dto);
  }
}
