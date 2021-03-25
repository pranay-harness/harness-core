package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.INVALID_REQUEST;
import static io.harness.exception.WingsException.USER;
import static io.harness.remote.client.RestClientUtils.getResponse;

import io.harness.annotations.dev.OwnedBy;
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
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
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

  @Override
  public void validateUpdateRequest(SecretDTOV2 existingSecret, SecretDTOV2 dto) {
    SecretTextSpecDTO specDTO = (SecretTextSpecDTO) dto.getSpec();
    SecretTextSpecDTO existingSpecDTO = (SecretTextSpecDTO) existingSecret.getSpec();
    Optional.ofNullable(specDTO.getSecretManagerIdentifier())
        .filter(x -> x.equals(existingSpecDTO.getSecretManagerIdentifier()))
        .orElseThrow(()
                         -> new InvalidRequestException(
                             "Cannot change secret manager after creation of secret", INVALID_REQUEST, USER));
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

  @Override
  public boolean update(String accountIdentifier, SecretDTOV2 existingSecret, SecretDTOV2 dto) {
    // validate update request
    validateUpdateRequest(existingSecret, dto);

    SecretTextSpecDTO specDTO = (SecretTextSpecDTO) dto.getSpec();
    SecretTextUpdateDTO updateDTO = getUpdateDTO(dto, specDTO);
    return getResponse(secretManagerClient.updateSecret(
        dto.getIdentifier(), accountIdentifier, dto.getOrgIdentifier(), dto.getProjectIdentifier(), updateDTO));
  }

  @Override
  public boolean updateViaYaml(String accountIdentifier, SecretDTOV2 existingSecret, SecretDTOV2 dto) {
    // validate update request
    validateUpdateRequest(existingSecret, dto);

    SecretTextSpecDTO specDTO = (SecretTextSpecDTO) dto.getSpec();
    SecretTextUpdateDTO updateDTO = getUpdateDTO(dto, specDTO);
    updateDTO.setDraft(true);
    return getResponse(secretManagerClient.updateSecret(
        dto.getIdentifier(), accountIdentifier, dto.getOrgIdentifier(), dto.getProjectIdentifier(), updateDTO));
  }
}
