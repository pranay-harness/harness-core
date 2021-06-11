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
import io.harness.exception.WingsException;
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

  public void setEncryptedValueToFileContent(EncryptedData encryptedData) {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    fileService.downloadToStream(String.valueOf(encryptedData.getEncryptedValue()), os, CONFIGS);
    encryptedData.setEncryptedValue(CHARSET.decode(ByteBuffer.wrap(os.toByteArray())).array());
  }
}
