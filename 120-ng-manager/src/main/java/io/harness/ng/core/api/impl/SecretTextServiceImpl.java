package io.harness.ng.core.api.impl;

import static io.harness.exception.WingsException.USER;
import static io.harness.remote.client.RestClientUtils.getResponse;

import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.api.SecretModifyService;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretTextSpecDTO;
import io.harness.secretmanagerclient.ValueType;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretTextDTO;
import io.harness.secretmanagerclient.dto.SecretTextUpdateDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class SecretTextServiceImpl implements SecretModifyService {
  private final SecretManagerClient secretManagerClient;

  @Override
  public EncryptedDataDTO create(@NotNull String accountIdentifier, @NotNull @Valid SecretDTOV2 dto) {
    SecretTextSpecDTO specDTO = (SecretTextSpecDTO) dto.getSpec();
    SecretTextDTO secretTextDTO = SecretTextDTO.builder()
                                      .account(accountIdentifier)
                                      .org(dto.getOrgIdentifier())
                                      .project(dto.getProjectIdentifier())
                                      .secretManager(specDTO.getSecretManagerIdentifier())
                                      .identifier(dto.getIdentifier())
                                      .name(dto.getName())
                                      .description(dto.getDescription())
                                      .type(dto.getType())
                                      .valueType(specDTO.getValueType())
                                      .value(specDTO.getValue())
                                      .build();
    return getResponse(secretManagerClient.createSecret(secretTextDTO));
  }

  private SecretTextUpdateDTO getUpdateDTO(SecretDTOV2 dto, SecretTextSpecDTO specDTO) {
    SecretTextUpdateDTO updateDTO = SecretTextUpdateDTO.builder()
                                        .name(dto.getName())
                                        .description(dto.getDescription())
                                        .draft(false)
                                        .valueType(specDTO.getValueType())
                                        .tags(null)
                                        .build();
    if (specDTO.getValueType() == ValueType.Inline) {
      updateDTO.setValue(specDTO.getValue());
    } else {
      updateDTO.setPath(specDTO.getValue());
    }
    return updateDTO;
  }

  private void validateUpdateRequest(String accountIdentifier, SecretDTOV2 dto, SecretTextSpecDTO specDTO) {
    EncryptedDataDTO encryptedDataDTO = getResponse(secretManagerClient.getSecret(
        dto.getIdentifier(), accountIdentifier, dto.getOrgIdentifier(), dto.getProjectIdentifier()));
    if (encryptedDataDTO == null) {
      throw new InvalidRequestException("No such secret found.");
    } else if (!specDTO.getSecretManagerIdentifier().equals(encryptedDataDTO.getSecretManager())) {
      throw new InvalidRequestException("Cannot change secret manager after creation of secret.", USER);
    }
  }

  @Override
  public boolean update(String accountIdentifier, SecretDTOV2 dto) {
    SecretTextSpecDTO specDTO = (SecretTextSpecDTO) dto.getSpec();
    SecretTextUpdateDTO updateDTO = getUpdateDTO(dto, specDTO);
    validateUpdateRequest(accountIdentifier, dto, specDTO);
    return getResponse(secretManagerClient.updateSecret(
        dto.getIdentifier(), accountIdentifier, dto.getOrgIdentifier(), dto.getProjectIdentifier(), updateDTO));
  }

  @Override
  public boolean updateViaYaml(String accountIdentifier, SecretDTOV2 dto) {
    SecretTextSpecDTO specDTO = (SecretTextSpecDTO) dto.getSpec();
    SecretTextUpdateDTO updateDTO = getUpdateDTO(dto, specDTO);
    updateDTO.setDraft(true);
    validateUpdateRequest(accountIdentifier, dto, specDTO);
    return getResponse(secretManagerClient.updateSecret(
        dto.getIdentifier(), accountIdentifier, dto.getOrgIdentifier(), dto.getProjectIdentifier(), updateDTO));
  }
}
