package io.harness.ng.core.api.impl;

import static io.harness.exception.WingsException.USER;
import static io.harness.ng.core.utils.NGUtils.verifyValuesNotChanged;
import static io.harness.secretmanagerclient.SecretType.toSettingVariableType;
import static io.harness.secretmanagerclient.utils.RestClientUtils.getResponse;
import static software.wings.resources.secretsmanagement.EncryptedDataMapper.fromDTO;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.NGPageResponse;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.api.NGSecretService;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.ValueType;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretTextDTO;
import io.harness.secretmanagerclient.dto.SecretTextUpdateDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.resources.secretsmanagement.EncryptedDataMapper;
import software.wings.security.encryption.EncryptedData;

import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class NGSecretServiceImpl implements NGSecretService {
  private final SecretManagerClient secretManagerClient;
  private final SecretEntityReferenceHelper secretEntityReferenceHelper;

  @Override
  public EncryptedData get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return fromDTO(
        getResponse(secretManagerClient.getSecret(identifier, accountIdentifier, orgIdentifier, projectIdentifier)));
  }

  private NGPageResponse<EncryptedData> getNGPageResponseFromPageResponse(PageResponse<EncryptedDataDTO> pageResponse) {
    NGPageResponse<EncryptedData> encryptedDataPageResponse =
        NGPageResponse.<EncryptedData>builder()
            .pageIndex(pageResponse.getStart())
            .empty(pageResponse.isEmpty())
            .pageSize(pageResponse.size())
            .itemCount(pageResponse.getTotal())
            .content(pageResponse.getResponse().stream().map(EncryptedDataMapper::fromDTO).collect(Collectors.toList()))
            .build();
    if (encryptedDataPageResponse.getItemCount() > 0 && encryptedDataPageResponse.getPageSize() > 0) {
      encryptedDataPageResponse.setPageCount(
          (int) Math.ceil((double) encryptedDataPageResponse.getItemCount() / encryptedDataPageResponse.getPageSize()));
    }
    return encryptedDataPageResponse;
  }

  @Override
  public NGPageResponse<EncryptedData> list(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      SecretType secretType, String searchTerm, int page, int size) {
    PageResponse<EncryptedDataDTO> pageResponse = getResponse(secretManagerClient.listSecrets(accountIdentifier,
        orgIdentifier, projectIdentifier, toSettingVariableType(secretType), searchTerm, page, size));
    return getNGPageResponseFromPageResponse(pageResponse);
  }

  @Override
  public EncryptedData create(SecretTextDTO dto, boolean viaYaml) {
    // setting inline secret using yaml is not allowed
    if (viaYaml && dto.getValueType() == ValueType.Inline) {
      if (Optional.ofNullable(dto.getValue()).isPresent()) {
        throw new InvalidRequestException("Secret cannot be created via YAML", USER);
      }
      dto.setDraft(true);
    }
    EncryptedDataDTO encryptedData = getResponse(secretManagerClient.createSecret(dto));
    secretEntityReferenceHelper.createEntityReferenceForSecret(encryptedData);
    return fromDTO(encryptedData);
  }

  @Override
  public boolean update(SecretTextDTO dto, boolean viaYaml) {
    // cannot update secret values via yaml
    if (viaYaml && ValueType.Inline == dto.getValueType()) {
      if (Optional.ofNullable(dto.getValue()).isPresent()) {
        throw new InvalidRequestException("Updating secret using YAML is not allowed");
      }
      dto.setDraft(true);
    }

    EncryptedData encryptedData = get(dto.getAccount(), dto.getOrg(), dto.getProject(), dto.getIdentifier());
    if (Optional.ofNullable(encryptedData).isPresent()) {
      verifyValuesNotChanged(Lists.newArrayList(Pair.of(dto.getSettingVariableType(), encryptedData.getType()),
          Pair.of(dto.getSecretManager(), encryptedData.getNgMetadata().getSecretManagerIdentifier())));
      SecretTextUpdateDTO updateDTO = SecretTextUpdateDTO.builder()
                                          .description(dto.getDescription())
                                          .name(dto.getName())
                                          .tags(dto.getTags())
                                          .value(dto.getValue())
                                          .path(dto.getPath())
                                          .valueType(dto.getValueType())
                                          .draft(dto.isDraft())
                                          .build();
      return getResponse(secretManagerClient.updateSecret(
          dto.getIdentifier(), dto.getAccount(), dto.getOrg(), dto.getProject(), updateDTO));
    }
    throw new InvalidRequestException("No such secret found", USER);
  }

  @Override
  public boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    EncryptedDataDTO encryptedDataDTO =
        getResponse(secretManagerClient.getSecret(identifier, accountIdentifier, orgIdentifier, projectIdentifier));
    boolean isSecretDeleted =
        getResponse(secretManagerClient.deleteSecret(identifier, accountIdentifier, orgIdentifier, projectIdentifier));
    if (isSecretDeleted && encryptedDataDTO != null) {
      secretEntityReferenceHelper.deleteSecretEntityReferenceWhenSecretGetsDeleted(encryptedDataDTO);
    }
    return isSecretDeleted;
  }
}
