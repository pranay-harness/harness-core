package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.encoding.EncodingUtils.encodeBase64ToByteArray;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.ENCRYPT_DECRYPT_ERROR;
import static io.harness.eraro.ErrorCode.INVALID_REQUEST;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.SRE;
import static io.harness.exception.WingsException.USER;
import static io.harness.secretmanagerclient.SecretType.SecretFile;
import static io.harness.secretmanagerclient.SecretType.SecretText;
import static io.harness.secretmanagerclient.ValueType.Inline;
import static io.harness.secretmanagerclient.ValueType.Reference;
import static io.harness.security.SimpleEncryption.CHARSET;
import static io.harness.security.encryption.EncryptionType.GCP_KMS;
import static io.harness.security.encryption.EncryptionType.LOCAL;
import static io.harness.security.encryption.SecretManagerType.KMS;
import static io.harness.security.encryption.SecretManagerType.VAULT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.beans.SecretManagerConfig;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.encryptors.KmsEncryptorsRegistry;
import io.harness.encryptors.VaultEncryptorsRegistry;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.SecretManagementException;
import io.harness.mappers.SecretManagerConfigMapper;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.api.NGEncryptedDataService;
import io.harness.ng.core.dao.NGEncryptedDataDao;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretFileSpecDTO;
import io.harness.ng.core.dto.secrets.SecretTextSpecDTO;
import io.harness.ng.core.entities.NGEncryptedData;
import io.harness.secretmanagerclient.dto.LocalConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.VaultConfigDTO;
import io.harness.secrets.SecretsFileService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionType;
import io.harness.security.encryption.SecretManagerType;

import software.wings.settings.SettingVariableTypes;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(PL)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class NGEncryptedDataServiceImpl implements NGEncryptedDataService {
  private static final Set<EncryptionType> ENCRYPTION_TYPES_REQUIRING_FILE_DOWNLOAD =
      EnumSet.of(LOCAL, GCP_KMS, EncryptionType.KMS);
  private final NGEncryptedDataDao encryptedDataDao;
  private final KmsEncryptorsRegistry kmsEncryptorsRegistry;
  private final VaultEncryptorsRegistry vaultEncryptorsRegistry;
  private final SecretsFileService secretsFileService;

  @Override
  public NGEncryptedData createSecretText(String accountIdentifier, SecretDTOV2 dto) {
    SecretTextSpecDTO secret = (SecretTextSpecDTO) dto.getSpec();

    SecretManagerConfigDTO secretManager = getSecretManagerOrThrow(accountIdentifier, dto.getOrgIdentifier(),
        dto.getProjectIdentifier(), secret.getSecretManagerIdentifier(), false);

    NGEncryptedData encryptedData = buildNGEncryptedData(accountIdentifier, dto, secretManager);

    if (Inline.equals(secret.getValueType())) {
      if (isReadOnlySecretManager(secretManager)) {
        throw new SecretManagementException(
            SECRET_MANAGEMENT_ERROR, "Cannot create an Inline secret in read only secret manager", USER);
      }
      EncryptedRecord encryptedRecord =
          getEncryptedRecord(encryptedData, secret.getValue(), SecretManagerConfigMapper.fromDTO(secretManager));

      validateEncryptedRecord(encryptedRecord);
      encryptedData.setEncryptionKey(encryptedRecord.getEncryptionKey());
      encryptedData.setEncryptedValue(encryptedRecord.getEncryptedValue());
    } else {
      validatePath(encryptedData.getPath(), encryptedData.getEncryptionType());
    }
    return encryptedDataDao.save(encryptedData);
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

  private NGEncryptedData buildNGEncryptedData(
      String accountIdentifier, SecretDTOV2 dto, SecretManagerConfigDTO secretManager) {
    NGEncryptedData.NGEncryptedDataBuilder builder = NGEncryptedData.builder();
    builder.accountIdentifier(accountIdentifier)
        .orgIdentifier(dto.getOrgIdentifier())
        .projectIdentifier(dto.getProjectIdentifier())
        .identifier(dto.getIdentifier())
        .name(dto.getName());
    builder.secretManagerIdentifier(secretManager.getIdentifier()).encryptionType(secretManager.getEncryptionType());
    if (SecretText.equals(dto.getType())) {
      SecretTextSpecDTO secret = (SecretTextSpecDTO) dto.getSpec();
      if (Reference.equals(secret.getValueType())) {
        builder.path(secret.getValue());
      }
      builder.type(SettingVariableTypes.SECRET_TEXT);
    } else if (SecretFile.equals(dto.getType())) {
      builder.type(SettingVariableTypes.CONFIG_FILE);
    }
    return builder.build();
  }

  private EncryptedRecord getEncryptedRecord(
      NGEncryptedData encryptedData, String value, SecretManagerConfig secretManagerConfig) {
    if (value == null) {
      return encryptedData;
    }
    SecretManagerType secretManagerType = secretManagerConfig.getType();
    if (KMS.equals(secretManagerType)) {
      return kmsEncryptorsRegistry.getKmsEncryptor(secretManagerConfig)
          .encryptSecret(encryptedData.getAccountIdentifier(), value, secretManagerConfig);
    } else if (VAULT.equals(secretManagerType)) {
      return vaultEncryptorsRegistry.getVaultEncryptor(secretManagerConfig.getEncryptionType())
          .createSecret(encryptedData.getAccountIdentifier(), encryptedData.getName(), value, secretManagerConfig);
    } else {
      throw new UnsupportedOperationException("Secret Manager type not supported: " + secretManagerType);
    }
  }

  private void validateEncryptedRecord(EncryptedRecord encryptedRecord) {
    if (encryptedRecord == null || isEmpty(encryptedRecord.getEncryptionKey())
        || isEmpty(encryptedRecord.getEncryptedValue())) {
      String message =
          "Encryption of secret failed unexpectedly. Please check your secret manager configuration and try again.";
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, message, USER);
    }
  }

  private void validatePath(String path, EncryptionType encryptionType) {
    if (path != null && encryptionType == EncryptionType.VAULT && path.indexOf('#') < 0) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR,
          "Secret path need to include the # sign with the the key name after. E.g. /foo/bar/my-secret#my-key.", USER);
    }
  }

  private boolean isReadOnlySecretManager(SecretManagerConfigDTO secretManager) {
    if (secretManager == null) {
      return false;
    }
    if (EncryptionType.VAULT.equals(secretManager.getEncryptionType())) {
      return ((VaultConfigDTO) secretManager).isReadOnly();
    }
    return false;
  }

  private SecretManagerConfigDTO getSecretManagerOrThrow(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String identifier, boolean maskSecrets) {
    SecretManagerConfigDTO secretManager =
        getSecretManager(accountIdentifier, orgIdentifier, projectIdentifier, identifier, maskSecrets);
    if (secretManager == null) {
      String message = String.format("No such secret manager found with identifier %s ", identifier);
      throw new SecretManagementException(
          SECRET_MANAGEMENT_ERROR, formNotFoundMessage(message, orgIdentifier, projectIdentifier), USER);
    }
    return secretManager;
  }

  private SecretManagerConfigDTO getSecretManager(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String identifier, boolean maskSecrets) {
    return LocalConfigDTO.builder()
        .isDefault(true)
        .encryptionType(EncryptionType.LOCAL)
        .name("dummy")
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .identifier(identifier)
        .harnessManaged(true)
        .build();
  }

  @Override
  public NGEncryptedData createSecretFile(String accountIdentifier, SecretDTOV2 dto, InputStream inputStream) {
    SecretFileSpecDTO secret = (SecretFileSpecDTO) dto.getSpec();

    SecretManagerConfigDTO secretManager = getSecretManagerOrThrow(accountIdentifier, dto.getOrgIdentifier(),
        dto.getProjectIdentifier(), secret.getSecretManagerIdentifier(), false);

    NGEncryptedData encryptedData = buildNGEncryptedData(accountIdentifier, dto, secretManager);

    if (isReadOnlySecretManager(secretManager)) {
      throw new SecretManagementException(
          SECRET_MANAGEMENT_ERROR, "Cannot create an Inline secret in read only secret manager", USER);
    }

    byte[] inputBytes;
    String fileContent;
    if (inputStream != null) {
      try {
        inputBytes = ByteStreams.toByteArray(inputStream);
      } catch (IOException exception) {
        throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unable to convert input stream to bytes", SRE);
      }
      fileContent = new String(CHARSET.decode(ByteBuffer.wrap(encodeBase64ToByteArray(inputBytes))).array());
    } else {
      fileContent = null;
    }

    EncryptedRecord encryptedRecord =
        getEncryptedRecord(encryptedData, fileContent, SecretManagerConfigMapper.fromDTO(secretManager));
    validateEncryptedRecord(encryptedRecord);

    encryptedData.setEncryptionKey(encryptedRecord.getEncryptionKey());
    if (ENCRYPTION_TYPES_REQUIRING_FILE_DOWNLOAD.contains(encryptedData.getEncryptionType())) {
      String encryptedFileId = null;
      if (fileContent != null) {
        encryptedFileId = secretsFileService.createFile(
            encryptedData.getName(), accountIdentifier, encryptedRecord.getEncryptedValue());
      }
      encryptedData.setEncryptedValue(encryptedFileId == null ? null : encryptedFileId.toCharArray());
    } else {
      encryptedData.setEncryptedValue(encryptedRecord.getEncryptedValue());
    }
    encryptedData.setBase64Encoded(true);

    return encryptedDataDao.save(encryptedData);
  }

  @Override
  public List<EncryptedDataDetail> getEncryptionDetails(NGAccess ngAccess, DecryptableEntity object) {
    // if object is already decrypted, return empty list
    if (object.isDecrypted()) {
      return Collections.emptyList();
    }
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    List<Field> encryptedFields = object.getSecretReferenceFields();

    // iterate over all the fields with @SecretReference annotation
    for (Field field : encryptedFields) {
      try {
        field.setAccessible(true);

        // type cast the field to SecretRefData, if the type casted value is not present, continue
        SecretRefData secretRefData = (SecretRefData) field.get(object);
        if (!Optional.ofNullable(secretRefData).isPresent()) {
          continue;
        }
        String secretIdentifier = secretRefData.getIdentifier();
        Scope secretScope = secretRefData.getScope();

        // if sufficient information is there to process this field, try to process it
        if (Optional.ofNullable(secretIdentifier).isPresent() && Optional.ofNullable(secretScope).isPresent()) {
          String accountIdentifier = ngAccess.getAccountIdentifier();
          String orgIdentifier = getOrgIdentifier(ngAccess.getOrgIdentifier(), secretScope);
          String projectIdentifier = getProjectIdentifier(ngAccess.getProjectIdentifier(), secretScope);

          // get encrypted data from DB
          NGEncryptedData encryptedData = get(accountIdentifier, orgIdentifier, projectIdentifier, secretIdentifier);
          if (encryptedData != null) {
            // if type is file and file is saved elsewhere, download and save contents in encryptedValue
            if (encryptedData.getType() == SettingVariableTypes.CONFIG_FILE
                && ENCRYPTION_TYPES_REQUIRING_FILE_DOWNLOAD.contains(encryptedData.getEncryptionType())) {
              setEncryptedValueToFileContent(encryptedData);
            }

            // get secret manager with which this was secret was encrypted
            SecretManagerConfigDTO secretManager = getSecretManager(
                accountIdentifier, orgIdentifier, projectIdentifier, encryptedData.getSecretManagerIdentifier(), false);
            if (secretManager != null) {
              EncryptedRecordData encryptedRecordData = buildEncryptedRecordData(encryptedData);
              encryptedDataDetails.add(EncryptedDataDetail.builder()
                                           .encryptedData(encryptedRecordData)
                                           .encryptionConfig(SecretManagerConfigMapper.fromDTO(secretManager))
                                           .fieldName(field.getName())
                                           .build());
            }
          }
        }
      } catch (IllegalAccessException exception) {
        throw new SecretManagementException(ENCRYPT_DECRYPT_ERROR, exception, USER);
      }
    }
    return encryptedDataDetails;
  }

  private EncryptedRecordData buildEncryptedRecordData(NGEncryptedData encryptedData) {
    return EncryptedRecordData.builder()
        .uuid(encryptedData.getUuid())
        .name(encryptedData.getName())
        .path(encryptedData.getPath())
        .parameters(encryptedData.getParameters())
        .encryptionKey(encryptedData.getEncryptionKey())
        .encryptedValue(encryptedData.getEncryptedValue())
        .kmsId(encryptedData.getKmsId())
        .encryptionType(encryptedData.getEncryptionType())
        .base64Encoded(encryptedData.isBase64Encoded())
        .build();
  }

  private void setEncryptedValueToFileContent(NGEncryptedData encryptedData) {
    char[] fileContent = secretsFileService.getFileContents(String.valueOf(encryptedData.getEncryptedValue()));
    encryptedData.setEncryptedValue(fileContent);
  }

  @Override
  public NGEncryptedData get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return encryptedDataDao.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
  }

  private String getOrgIdentifier(String parentOrgIdentifier, @NotNull Scope scope) {
    if (scope != Scope.ACCOUNT) {
      return parentOrgIdentifier;
    }
    return null;
  }

  private String getProjectIdentifier(String parentProjectIdentifier, @NotNull Scope scope) {
    if (scope == Scope.PROJECT) {
      return parentProjectIdentifier;
    }
    return null;
  }

  @Override
  public boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    NGEncryptedData encryptedData = get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);

    if (encryptedData != null) {
      SecretManagerConfigDTO secretManager = getSecretManagerOrThrow(
          accountIdentifier, orgIdentifier, projectIdentifier, encryptedData.getSecretManagerIdentifier(), true);

      if (isReadOnlySecretManager(secretManager)
          && Optional.ofNullable(encryptedData.getEncryptedValue()).isPresent()) {
        throw new SecretManagementException(
            SECRET_MANAGEMENT_ERROR, "Cannot delete an Inline secret in read only secret manager", USER);
      }
      if (!Optional.ofNullable(encryptedData.getPath()).isPresent()
          && Optional.ofNullable(encryptedData.getEncryptedValue()).isPresent()) {
        deleteSecretInSecretManager(accountIdentifier, encryptedData, SecretManagerConfigMapper.fromDTO(secretManager));
      }
      if (encryptedData.getType() == SettingVariableTypes.CONFIG_FILE
          && ENCRYPTION_TYPES_REQUIRING_FILE_DOWNLOAD.contains(encryptedData.getEncryptionType())) {
        secretsFileService.deleteFile(encryptedData.getEncryptedValue());
      }
      return encryptedDataDao.delete(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    }
    throw new InvalidRequestException("No such secret found", INVALID_REQUEST, USER);
  }

  private void deleteSecretInSecretManager(
      String accountIdentifier, NGEncryptedData encryptedData, SecretManagerConfig secretManagerConfig) {
    SecretManagerType secretManagerType = secretManagerConfig.getType();
    if (VAULT.equals(secretManagerType)) {
      vaultEncryptorsRegistry.getVaultEncryptor(secretManagerConfig.getEncryptionType())
          .deleteSecret(accountIdentifier, encryptedData, secretManagerConfig);
    }
  }
}
