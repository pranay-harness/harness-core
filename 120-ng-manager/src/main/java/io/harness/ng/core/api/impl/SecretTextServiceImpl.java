package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.INVALID_REQUEST;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.remote.client.RestClientUtils.getResponse;
import static io.harness.secretmanagerclient.ValueType.Inline;
import static io.harness.security.encryption.EncryptionType.VAULT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.SecretManagerConfig;
import io.harness.encryptors.KmsEncryptorsRegistry;
import io.harness.encryptors.VaultEncryptor;
import io.harness.encryptors.VaultEncryptorsRegistry;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.SecretManagementException;
import io.harness.mappers.SecretManagerConfigMapper;
import io.harness.ng.core.api.SecretModifyService;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretTextSpecDTO;
import io.harness.ng.entities.NGEncryptedData;
import io.harness.repositories.ng.core.spring.NGEncryptedDataRepository;
import io.harness.secretmanagerclient.NGEncryptedDataMetadata;
import io.harness.secretmanagerclient.ValueType;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.SecretTextDTO;
import io.harness.secretmanagerclient.dto.SecretTextUpdateDTO;
import io.harness.secretmanagerclient.dto.VaultConfigDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionType;

import software.wings.resources.secretsmanagement.EncryptedDataMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(PL)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class SecretTextServiceImpl implements SecretModifyService {
  private final SecretManagerClient secretManagerClient;
  private final VaultEncryptorsRegistry vaultRegistry;
  private final KmsEncryptorsRegistry kmsRegistry;
  private final NGEncryptedDataRepository ngEncryptedDataRepository;

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
    return EncryptedDataMapper.toDTO(createSecretText(secretTextDTO));
  }

  private EncryptedData createSecretText(@NotNull SecretTextDTO dto) {
    // both value and path cannot be present, only one of them is allowed
    if (Optional.ofNullable(dto.getPath()).isPresent() && Optional.ofNullable(dto.getValue()).isPresent()) {
      throw new InvalidRequestException("Cannot provide both path and value while saving secret", USER);
    }

    // create encrypted data from dto
    EncryptedData data = EncryptedDataMapper.fromDTO(dto);
    NGEncryptedDataMetadata metadata = data.getNgMetadata();

    // Fetch secret manager with which this secret will be saved
    SecretManagerConfigDTO secretManagerConfig =
        getResponse(secretManagerClient.getSecretManager(metadata.getSecretManagerIdentifier(),
            metadata.getAccountIdentifier(), metadata.getOrgIdentifier(), metadata.getProjectIdentifier(), false));

    if (secretManagerConfig != null) {
      if (isReadOnlySecretManager(secretManagerConfig)
          && (Inline.equals(dto.getValueType()) || Optional.ofNullable(dto.getValue()).isPresent())) {
        throw new SecretManagementException(
            SECRET_MANAGEMENT_ERROR, "Cannot create an Inline secret in read only secret manager", USER);
      }

      // validate format of path as per type of secret manager
      validatePath(data.getPath(), secretManagerConfig.getEncryptionType());

      data.setKmsId(secretManagerConfig.getIdentifier());
      data.setEncryptionType(secretManagerConfig.getEncryptionType());
      data.getNgMetadata().setSecretManagerName(secretManagerConfig.getName());

      if (!StringUtils.isEmpty(dto.getValue())) {
        // send task to delegate for saving secret
        EncryptedData encryptedData =
            encrypt(data, dto.getValue(), SecretManagerConfigMapper.fromDTO(secretManagerConfig));

        // set fields and save secret in DB
        data.setEncryptionKey(encryptedData.getEncryptionKey());
        data.setEncryptedValue(encryptedData.getEncryptedValue());
      }
      ngEncryptedDataRepository.save(NGEncryptedData.builder()
                                         .accountIdentifier(metadata.getAccountIdentifier())
                                         .orgIdentifier(metadata.getOrgIdentifier())
                                         .projectIdentifier(metadata.getProjectIdentifier())
                                         .identifier(metadata.getIdentifier())
                                         .name(data.getName())
                                         .path(data.getPath())
                                         .encryptionType(data.getEncryptionType())
                                         .encryptedValue(data.getEncryptedValue())
                                         .encryptionKey(data.getEncryptionKey())
                                         .secretManagerIdentifier(data.getKmsId())
                                         .parameters(data.getParameters())
                                         .additionalMetadata(data.getAdditionalMetadata())
                                         .base64Encoded(data.isBase64Encoded())
                                         .backupEncryptedValue(data.getBackupEncryptedValue())
                                         .backupEncryptionType(data.getBackupEncryptionType())
                                         .backupEncryptionKey(data.getBackupEncryptionKey())
                                         .backupKmsId(data.getBackupKmsId())
                                         .internal(false)
                                         .build());
      return data;
    } else {
      String message = "No such secret manager found";
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR,
          formNotFoundMessage(message, metadata.getOrgIdentifier(), metadata.getProjectIdentifier()), USER);
    }
  }

  private EncryptedData encrypt(
      @NotNull EncryptedData encryptedData, String secretValue, SecretManagerConfig secretManagerConfig) {
    EncryptedRecord encryptedRecord;
    switch (encryptedData.getEncryptionType()) {
      case VAULT:
        VaultEncryptor vaultEncryptor = vaultRegistry.getVaultEncryptor(secretManagerConfig.getEncryptionType());
        encryptedRecord = vaultEncryptor.createSecret(
            encryptedData.getAccountId(), encryptedData.getName(), secretValue, secretManagerConfig);
        break;
      case GCP_KMS:
      case KMS:
      case LOCAL:
        encryptedRecord = kmsRegistry.getKmsEncryptor(secretManagerConfig)
                              .encryptSecret(encryptedData.getAccountId(), secretValue, secretManagerConfig);
        break;
      default:
        throw new UnsupportedOperationException("Encryption type not supported: " + encryptedData.getEncryptionType());
    }
    return EncryptedData.builder()
        .encryptionKey(encryptedRecord.getEncryptionKey())
        .encryptedValue(encryptedRecord.getEncryptedValue())
        .build();
  }

  private void validatePath(String path, EncryptionType encryptionType) {
    if (path != null && encryptionType == VAULT && path.indexOf('#') < 0) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR,
          "Secret path need to include the # sign with the the key name after. E.g. /foo/bar/my-secret#my-key.", USER);
    }
  }

  private String formNotFoundMessage(String baseMessage, String orgIdentifier, String projectIdentifier) {
    if (!StringUtils.isEmpty(orgIdentifier)) {
      baseMessage += String.format("in org: %s", orgIdentifier);
      if (!StringUtils.isEmpty(projectIdentifier)) {
        baseMessage += String.format(" and project: %s", projectIdentifier);
      }
    } else if (!StringUtils.isEmpty(projectIdentifier)) {
      baseMessage += "in project: %s" + projectIdentifier;
    } else {
      baseMessage += "in this scope.";
    }
    return baseMessage;
  }

  static boolean isReadOnlySecretManager(SecretManagerConfigDTO secretManagerConfig) {
    if (secretManagerConfig == null) {
      return false;
    }
    if (VAULT.equals(secretManagerConfig.getEncryptionType())) {
      return ((VaultConfigDTO) secretManagerConfig).isReadOnly();
    }
    return false;
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
    return updateSecretText(
        accountIdentifier, dto.getOrgIdentifier(), dto.getProjectIdentifier(), dto.getIdentifier(), updateDTO);
  }

  public boolean updateSecretText(
      @NotNull String account, String org, String project, String identifier, SecretTextUpdateDTO dto) {
    // while updating a secret, only one of value/path can be provided
    if (Optional.ofNullable(dto.getPath()).isPresent() && Optional.ofNullable(dto.getValue()).isPresent()) {
      throw new InvalidRequestException("Cannot provide both path and value while saving secret", USER);
    }

    // get existing secret text
    Optional<NGEncryptedData> encryptedDataOptional = get(account, org, project, identifier);

    if (encryptedDataOptional.isPresent()) {
      NGEncryptedData ngEncryptedData = encryptedDataOptional.get();
      EncryptedData encryptedData = EncryptedData.builder()
                                        .name(ngEncryptedData.getName())
                                        .path(ngEncryptedData.getPath())
                                        .encryptionType(ngEncryptedData.getEncryptionType())
                                        .encryptedValue(ngEncryptedData.getEncryptedValue())
                                        .encryptionKey(ngEncryptedData.getEncryptionKey())
                                        .kmsId(ngEncryptedData.getKmsId())
                                        .parameters(ngEncryptedData.getParameters())
                                        .additionalMetadata(ngEncryptedData.getAdditionalMetadata())
                                        .base64Encoded(ngEncryptedData.isBase64Encoded())
                                        .backupEncryptedValue(ngEncryptedData.getBackupEncryptedValue())
                                        .backupEncryptionType(ngEncryptedData.getBackupEncryptionType())
                                        .backupEncryptionKey(ngEncryptedData.getBackupEncryptionKey())
                                        .backupKmsId(ngEncryptedData.getBackupKmsId())
                                        .build();

      // Fetch secret manager with which this secret will be saved
      SecretManagerConfigDTO secretManagerConfig = getResponse(secretManagerClient.getSecretManager(
          ngEncryptedData.getSecretManagerIdentifier(), ngEncryptedData.getAccountIdentifier(),
          ngEncryptedData.getOrgIdentifier(), ngEncryptedData.getProjectIdentifier(), false));

      if (secretManagerConfig != null) {
        if (isReadOnlySecretManager(secretManagerConfig)
            && (Inline.equals(dto.getValueType()) || Optional.ofNullable(dto.getValue()).isPresent())) {
          throw new SecretManagementException(
              SECRET_MANAGEMENT_ERROR, "Cannot update to an Inline secret in read only secret manager", USER);
        }
        validatePath(dto.getPath(), secretManagerConfig.getEncryptionType());

        // if name has been changed, delete old text if it was created inline
        if (!encryptedData.getName().equals(dto.getName())
            && !Optional.ofNullable(encryptedData.getPath()).isPresent()) {
          deleteSecretInSecretManager(ngEncryptedData.getAccountIdentifier(), encryptedData,
              SecretManagerConfigMapper.fromDTO(secretManagerConfig));
        }

        // set updated values
        encryptedData.setName(dto.getName());
        encryptedData.setPath(dto.getPath());
        encryptedData.getNgMetadata().setTags(dto.getTags());
        encryptedData.getNgMetadata().setDescription(dto.getDescription());
        encryptedData.getNgMetadata().setDraft(dto.isDraft());

        // send to delegate to create/update secret
        if (!StringUtils.isEmpty(dto.getValue())) {
          EncryptedData updatedEncryptedData =
              encrypt(encryptedData, dto.getValue(), SecretManagerConfigMapper.fromDTO(secretManagerConfig));

          // set encryption key and value and save secret in DB
          encryptedData.setEncryptionKey(updatedEncryptedData.getEncryptionKey());
          encryptedData.setEncryptedValue(updatedEncryptedData.getEncryptedValue());
        }
        ngEncryptedDataRepository.save(NGEncryptedData.builder()
                                           .accountIdentifier(ngEncryptedData.getAccountIdentifier())
                                           .orgIdentifier(ngEncryptedData.getOrgIdentifier())
                                           .projectIdentifier(ngEncryptedData.getProjectIdentifier())
                                           .identifier(ngEncryptedData.getIdentifier())
                                           .name(encryptedData.getName())
                                           .path(encryptedData.getPath())
                                           .encryptionType(encryptedData.getEncryptionType())
                                           .encryptedValue(encryptedData.getEncryptedValue())
                                           .encryptionKey(encryptedData.getEncryptionKey())
                                           .secretManagerIdentifier(encryptedData.getKmsId())
                                           .parameters(encryptedData.getParameters())
                                           .additionalMetadata(encryptedData.getAdditionalMetadata())
                                           .base64Encoded(encryptedData.isBase64Encoded())
                                           .backupEncryptedValue(encryptedData.getBackupEncryptedValue())
                                           .backupEncryptionType(encryptedData.getBackupEncryptionType())
                                           .backupEncryptionKey(encryptedData.getBackupEncryptionKey())
                                           .backupKmsId(encryptedData.getBackupKmsId())
                                           .internal(false)
                                           .build());
        return true;
      } else {
        throw new InvalidRequestException(
            formNotFoundMessage("No such secret manager found", ngEncryptedData.getOrgIdentifier(),
                ngEncryptedData.getProjectIdentifier()),
            INVALID_REQUEST, USER);
      }
    }
    throw new InvalidRequestException(formNotFoundMessage("No such secret found", org, project), INVALID_REQUEST, USER);
  }

  public void deleteSecretInSecretManager(
      String accountIdentifier, EncryptedData encryptedData, SecretManagerConfig secretManagerConfig) {
    switch (secretManagerConfig.getEncryptionType()) {
      case VAULT:
        vaultRegistry.getVaultEncryptor(secretManagerConfig.getEncryptionType())
            .deleteSecret(accountIdentifier, encryptedData, secretManagerConfig);
        return;
      case LOCAL:
      case GCP_KMS:
      case KMS:
        return;
      default:
        throw new UnsupportedOperationException(
            "Encryption type " + secretManagerConfig.getEncryptionType() + " is not supported in next gen");
    }
  }

  public Optional<NGEncryptedData> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return ngEncryptedDataRepository
        .findNGEncryptedDataByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, identifier);
  }

  @Override
  public boolean updateViaYaml(String accountIdentifier, SecretDTOV2 existingSecret, SecretDTOV2 dto) {
    // validate update request
    validateUpdateRequest(existingSecret, dto);

    SecretTextSpecDTO specDTO = (SecretTextSpecDTO) dto.getSpec();
    SecretTextUpdateDTO updateDTO = getUpdateDTO(dto, specDTO);
    updateDTO.setDraft(true);
    return updateSecretText(
        accountIdentifier, dto.getOrgIdentifier(), dto.getProjectIdentifier(), dto.getIdentifier(), updateDTO);
  }
}
