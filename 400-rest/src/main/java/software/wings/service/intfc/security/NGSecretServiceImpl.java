package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.delegate.beans.FileBucket.CONFIGS;
import static io.harness.security.SimpleEncryption.CHARSET;
import static io.harness.security.encryption.EncryptionType.GCP_KMS;
import static io.harness.security.encryption.EncryptionType.KMS;
import static io.harness.security.encryption.EncryptionType.LOCAL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedData.EncryptedDataKeys;
import io.harness.beans.SecretManagerConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.secretmanagerclient.NGEncryptedDataMetadata;
import io.harness.secretmanagerclient.NGMetadata.NGMetadataKeys;
import io.harness.secretmanagerclient.NGSecretManagerMetadata.NGSecretManagerMetadataKeys;
import io.harness.secretmanagerclient.dto.EncryptedDataMigrationDTO;
import io.harness.secrets.SecretsFileService;
import io.harness.security.encryption.EncryptionType;

import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FileService;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
  This file will be deleted after migration
 */
@OwnedBy(PL)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@TargetModule(HarnessModule._950_NG_CORE)
public class NGSecretServiceImpl implements NGSecretService {
  static final Set<EncryptionType> ENCRYPTION_TYPES_REQUIRING_FILE_DOWNLOAD = EnumSet.of(LOCAL, GCP_KMS, KMS);
  private static final String ACCOUNT_IDENTIFIER_KEY =
      EncryptedDataKeys.ngMetadata + "." + NGSecretManagerMetadataKeys.accountIdentifier;
  private static final String ORG_IDENTIFIER_KEY =
      EncryptedDataKeys.ngMetadata + "." + NGSecretManagerMetadataKeys.orgIdentifier;
  private static final String IDENTIFIER_KEY = EncryptedDataKeys.ngMetadata + "." + NGMetadataKeys.identifier;
  private static final String PROJECT_IDENTIFIER_KEY =
      EncryptedDataKeys.ngMetadata + "." + NGSecretManagerMetadataKeys.projectIdentifier;

  private final SecretsFileService secretFileService;
  private final NGSecretManagerService ngSecretManagerService;
  private final WingsPersistence wingsPersistence;
  private final FileService fileService;

  private Optional<EncryptedData> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return Optional.ofNullable(wingsPersistence.createQuery(EncryptedData.class)
                                   .field(ACCOUNT_IDENTIFIER_KEY)
                                   .equal(accountIdentifier)
                                   .field(ORG_IDENTIFIER_KEY)
                                   .equal(orgIdentifier)
                                   .field(PROJECT_IDENTIFIER_KEY)
                                   .equal(projectIdentifier)
                                   .field(IDENTIFIER_KEY)
                                   .equal(identifier)
                                   .get());
  }

  @Override
  public boolean save(EncryptedDataMigrationDTO encryptedData) {
    Optional<EncryptedData> encryptedDataOptional = get(encryptedData.getAccountIdentifier(),
        encryptedData.getOrgIdentifier(), encryptedData.getProjectIdentifier(), encryptedData.getIdentifier());
    if (!encryptedDataOptional.isPresent()) {
      EncryptedData existing = EncryptedData.builder().build();
      existing.setAccountId(encryptedData.getAccountIdentifier());
      Optional<SecretManagerConfig> secretManagerConfig =
          ngSecretManagerService.get(encryptedData.getAccountIdentifier(), encryptedData.getOrgIdentifier(),
              encryptedData.getProjectIdentifier(), encryptedData.getKmsId(), true);
      if (!secretManagerConfig.isPresent()) {
        return false;
      }
      existing.setKmsId(secretManagerConfig.get().getUuid());
      existing.setEncryptionType(encryptedData.getEncryptionType());
      existing.setNgMetadata(NGEncryptedDataMetadata.builder()
                                 .accountIdentifier(encryptedData.getAccountIdentifier())
                                 .orgIdentifier(encryptedData.getOrgIdentifier())
                                 .projectIdentifier(encryptedData.getProjectIdentifier())
                                 .identifier(encryptedData.getIdentifier())
                                 .secretManagerIdentifier(encryptedData.getKmsId())
                                 .build());
      existing.setName(encryptedData.getName());
      existing.setBase64Encoded(encryptedData.isBase64Encoded());
      existing.setEncryptionKey(encryptedData.getEncryptionKey());
      existing.setType(encryptedData.getType());
      existing.setPath(encryptedData.getPath());
      if (encryptedData.getType() == SettingVariableTypes.CONFIG_FILE
          && ENCRYPTION_TYPES_REQUIRING_FILE_DOWNLOAD.contains(encryptedData.getEncryptionType())
          && Optional.ofNullable(encryptedData.getEncryptedValue()).isPresent()) {
        String fileId = secretFileService.createFile(
            encryptedData.getName(), encryptedData.getAccountIdentifier(), encryptedData.getEncryptedValue());
        existing.setEncryptedValue(fileId.toCharArray());
      } else {
        existing.setEncryptedValue(encryptedData.getEncryptedValue());
      }
      wingsPersistence.save(existing);
      return true;
    }
    throw new InvalidRequestException(
        String.format("Secret with identifier %s already exists in this scope", encryptedData.getIdentifier()));
  }

  @Override
  public boolean update(EncryptedDataMigrationDTO encryptedData) {
    Optional<EncryptedData> encryptedDataOptional = get(encryptedData.getAccountIdentifier(),
        encryptedData.getOrgIdentifier(), encryptedData.getProjectIdentifier(), encryptedData.getIdentifier());
    if (encryptedDataOptional.isPresent()) {
      EncryptedData existing = encryptedDataOptional.get();
      existing.setBase64Encoded(encryptedData.isBase64Encoded());
      existing.setEncryptionKey(encryptedData.getEncryptionKey());
      existing.setPath(encryptedData.getPath());
      existing.setName(encryptedData.getName());
      if (encryptedData.getType() == SettingVariableTypes.CONFIG_FILE
          && ENCRYPTION_TYPES_REQUIRING_FILE_DOWNLOAD.contains(encryptedData.getEncryptionType())
          && Optional.ofNullable(encryptedData.getEncryptedValue()).isPresent()) {
        String fileId = secretFileService.createFile(
            encryptedData.getName(), encryptedData.getAccountIdentifier(), encryptedData.getEncryptedValue());
        if (Optional.ofNullable(existing.getEncryptedValue()).isPresent()) {
          try {
            secretFileService.deleteFile(existing.getEncryptedValue());
          } catch (Exception exception) {
            // ignore
          }
        }
        existing.setEncryptedValue(fileId.toCharArray());
      } else {
        existing.setEncryptedValue(encryptedData.getEncryptedValue());
      }
      wingsPersistence.save(existing);
      return true;
    }
    throw new InvalidRequestException(
        String.format("Secret with identifier %s not found in this scope", encryptedData.getIdentifier()));
  }

  @Override
  public Optional<EncryptedDataMigrationDTO> getEncryptedDataMigrationDTO(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Optional<EncryptedData> encryptedDataOptional =
        get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    if (encryptedDataOptional.isPresent()) {
      EncryptedData encryptedData = encryptedDataOptional.get();
      if (encryptedData.getType() == SettingVariableTypes.CONFIG_FILE
          && ENCRYPTION_TYPES_REQUIRING_FILE_DOWNLOAD.contains(encryptedData.getEncryptionType())
          && Optional.ofNullable(encryptedData.getEncryptedValue()).isPresent()) {
        try {
          setEncryptedValueToFileContent(encryptedData);
        } catch (WingsException exception) {
          // ignore can't do anything if file is not present
        }
      }
      return Optional.of(EncryptedDataMigrationDTO.builder()
                             .uuid(encryptedData.getUuid())
                             .name(encryptedData.getName())
                             .type(encryptedData.getType())
                             .encryptionType(encryptedData.getEncryptionType())
                             .encryptionKey(encryptedData.getEncryptionKey())
                             .encryptedValue(encryptedData.getEncryptedValue())
                             .path(encryptedData.getPath())
                             .base64Encoded(encryptedData.isBase64Encoded())
                             .kmsId(encryptedData.getNgMetadata().getSecretManagerIdentifier())
                             .accountIdentifier(accountIdentifier)
                             .orgIdentifier(orgIdentifier)
                             .projectIdentifier(projectIdentifier)
                             .identifier(identifier)
                             .build());
    }
    return Optional.empty();
  }

  @Override
  public boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Optional<EncryptedData> encryptedDataOptional =
        get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);

    if (encryptedDataOptional.isPresent()) {
      EncryptedData encryptedData = encryptedDataOptional.get();
      if (encryptedData.getType() == SettingVariableTypes.CONFIG_FILE
          && Optional.ofNullable(encryptedData.getEncryptedValue()).isPresent()
          && ENCRYPTION_TYPES_REQUIRING_FILE_DOWNLOAD.contains(encryptedData.getEncryptionType())) {
        try {
          secretFileService.deleteFile(encryptedData.getEncryptedValue());
        } catch (Exception exception) {
          // ignore
        }
        return wingsPersistence.delete(EncryptedData.class, encryptedData.getUuid());
      }
    }
    return false;
  }

  public void setEncryptedValueToFileContent(EncryptedData encryptedData) {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    fileService.downloadToStream(String.valueOf(encryptedData.getEncryptedValue()), os, CONFIGS);
    encryptedData.setEncryptedValue(CHARSET.decode(ByteBuffer.wrap(os.toByteArray())).array());
  }
}
