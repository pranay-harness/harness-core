package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.delegate.beans.FileBucket.CONFIGS;
import static io.harness.eraro.ErrorCode.INVALID_REQUEST;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.security.SimpleEncryption.CHARSET;
import static io.harness.security.encryption.EncryptionType.GCP_KMS;
import static io.harness.security.encryption.EncryptionType.KMS;
import static io.harness.security.encryption.EncryptionType.LOCAL;

import static software.wings.service.intfc.security.NGSecretManagerService.isReadOnlySecretManager;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedData.EncryptedDataKeys;
import io.harness.beans.SecretManagerConfig;
import io.harness.encryptors.VaultEncryptorsRegistry;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.SecretManagementException;
import io.harness.secretmanagerclient.NGEncryptedDataMetadata;
import io.harness.secretmanagerclient.NGMetadata.NGMetadataKeys;
import io.harness.secretmanagerclient.NGSecretManagerMetadata.NGSecretManagerMetadataKeys;
import io.harness.secretmanagerclient.dto.EncryptedDataMigrationDTO;
import io.harness.secretmanagers.SecretManagerConfigService;
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

  private final VaultEncryptorsRegistry vaultRegistry;
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
  public boolean deleteSecretText(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    // Get secret text to delete from DB
    Optional<EncryptedData> encryptedDataOptional =
        get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);

    if (encryptedDataOptional.isPresent()) {
      EncryptedData encryptedData = encryptedDataOptional.get();
      NGEncryptedDataMetadata metadata = encryptedData.getNgMetadata();

      // Get secret manager with which it was encrypted
      Optional<SecretManagerConfig> secretManagerConfigOptional = ngSecretManagerService.get(
          accountIdentifier, orgIdentifier, projectIdentifier, metadata.getSecretManagerIdentifier(), true);
      if (secretManagerConfigOptional.isPresent()) {
        if (isReadOnlySecretManager(secretManagerConfigOptional.get())
            && Optional.ofNullable(encryptedData.getEncryptedValue()).isPresent()) {
          throw new SecretManagementException(
              SECRET_MANAGEMENT_ERROR, "Cannot delete an Inline secret in read only secret manager", USER);
        }
        // if  secret text was created inline (not referenced), delete the secret in secret manager also
        if (!Optional.ofNullable(encryptedData.getPath()).isPresent()) {
          deleteSecretInSecretManager(accountIdentifier, encryptedData, secretManagerConfigOptional.get());
        }
        if (encryptedData.getType() == SettingVariableTypes.CONFIG_FILE) {
          switch (secretManagerConfigOptional.get().getEncryptionType()) {
            case LOCAL:
            case GCP_KMS:
            case KMS:
              fileService.deleteFile(String.valueOf(encryptedData.getEncryptedValue()), CONFIGS);
              break;
            default:
          }
        }
        // delete secret text finally in db
        return wingsPersistence.delete(EncryptedData.class, encryptedData.getUuid());
      }
    }
    throw new InvalidRequestException("No such secret found", INVALID_REQUEST, USER);
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
        setEncryptedValueToFileContent(encryptedData);
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

  private void setEncryptedValueToFileContent(EncryptedData encryptedData) {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    fileService.downloadToStream(String.valueOf(encryptedData.getEncryptedValue()), os, CONFIGS);
    encryptedData.setEncryptedValue(CHARSET.decode(ByteBuffer.wrap(os.toByteArray())).array());
  }
}
