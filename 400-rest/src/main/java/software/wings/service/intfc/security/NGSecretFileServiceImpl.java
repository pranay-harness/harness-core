package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessModule._890_SM_CORE;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.encoding.EncodingUtils.encodeBase64ToByteArray;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.FileBucket.CONFIGS;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.SRE;
import static io.harness.exception.WingsException.USER;
import static io.harness.security.SimpleEncryption.CHARSET;

import static software.wings.service.intfc.security.NGSecretManagerService.isReadOnlySecretManager;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EncryptedData;
import io.harness.beans.SecretManagerConfig;
import io.harness.encryptors.KmsEncryptorsRegistry;
import io.harness.encryptors.VaultEncryptor;
import io.harness.encryptors.VaultEncryptorsRegistry;
import io.harness.exception.SecretManagementException;
import io.harness.secretmanagerclient.NGEncryptedDataMetadata;
import io.harness.secretmanagerclient.dto.SecretFileDTO;
import io.harness.secretmanagerclient.dto.SecretFileUpdateDTO;
import io.harness.secretmanagers.SecretManagerConfigService;
import io.harness.secrets.SecretsFileService;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptedRecordData;

import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FileService;
import software.wings.settings.SettingVariableTypes;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;

@OwnedBy(PL)
@TargetModule(_890_SM_CORE)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class NGSecretFileServiceImpl implements NGSecretFileService {
  private final String ILLEGAL_CHARACTERS = "[~!@#$%^&*'\"/?<>,;.]";
  private final NGSecretManagerService ngSecretManagerService;
  private final NGSecretService ngSecretService;
  private final VaultEncryptorsRegistry vaultRegistry;
  private final KmsEncryptorsRegistry kmsRegistry;
  private final SecretsFileService secretFileService;
  private final WingsPersistence wingsPersistence;
  private final SecretManagerConfigService secretManagerConfigService;
  private final FileService fileService;

  private EncryptedData encrypt(String accountIdentifier, SecretManagerConfig secretManagerConfig, String name,
      byte[] bytes, EncryptedData savedEncryptedData) {
    String fileContent = new String(CHARSET.decode(ByteBuffer.wrap(encodeBase64ToByteArray(bytes))).array());
    EncryptedRecord encryptedRecord;
    switch (secretManagerConfig.getEncryptionType()) {
      case VAULT:
      case AZURE_VAULT:
        VaultEncryptor vaultEncryptor = vaultRegistry.getVaultEncryptor(secretManagerConfig.getEncryptionType());
        if (savedEncryptedData == null) {
          encryptedRecord = vaultEncryptor.createSecret(accountIdentifier, name, fileContent, secretManagerConfig);
        } else {
          encryptedRecord = vaultEncryptor.updateSecret(
              accountIdentifier, name, fileContent, savedEncryptedData, secretManagerConfig);
        }
        break;
      case GCP_KMS:
      case KMS:
      case LOCAL:
        encryptedRecord = kmsRegistry.getKmsEncryptor(secretManagerConfig)
                              .encryptSecret(accountIdentifier, fileContent, secretManagerConfig);
        String encryptedFileId =
            secretFileService.createFile(name, accountIdentifier, encryptedRecord.getEncryptedValue());
        encryptedRecord = EncryptedRecordData.builder()
                              .encryptedValue(encryptedFileId.toCharArray())
                              .encryptionKey(encryptedRecord.getEncryptionKey())
                              .build();
        break;
      default:
        throw new UnsupportedOperationException(
            "Encryption type " + secretManagerConfig.getEncryptionType() + " not supported in next gen");
    }
    return EncryptedData.builder()
        .name(name)
        .encryptedValue(encryptedRecord.getEncryptedValue())
        .encryptionKey(encryptedRecord.getEncryptionKey())
        .base64Encoded(true)
        .fileSize(bytes.length)
        .build();
  }

  @Override
  public EncryptedData create(SecretFileDTO dto, @NotNull InputStream inputStream) {
    // create NG meta data out of DTO
    NGEncryptedDataMetadata metadata = NGEncryptedDataMetadata.builder()
                                           .accountIdentifier(dto.getAccount())
                                           .orgIdentifier(dto.getOrg())
                                           .projectIdentifier(dto.getProject())
                                           .identifier(dto.getIdentifier())
                                           .secretManagerIdentifier(dto.getSecretManager())
                                           .description(dto.getDescription())
                                           .tags(dto.getTags())
                                           .build();

    // check for validity of name
    if (isEmpty(dto.getName()) || containsIllegalCharacters(dto.getName())) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR,
          "Encrypted file name/identifier should not have any of the following characters " + ILLEGAL_CHARACTERS, USER);
    }

    // check if a file with the same identifier does not exist in the scope (account/org/project)
    if (ngSecretService
            .get(metadata.getAccountIdentifier(), metadata.getOrgIdentifier(), metadata.getProjectIdentifier(),
                metadata.getIdentifier())
            .isPresent()) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR,
          String.format("Secret with identifier [%s] exists in this scope", dto.getIdentifier()), USER);
    }

    // get secret manager with which the file is to be encrypted
    Optional<SecretManagerConfig> secretManagerConfigOptional =
        ngSecretManagerService.get(metadata.getAccountIdentifier(), metadata.getOrgIdentifier(),
            metadata.getProjectIdentifier(), metadata.getSecretManagerIdentifier(), true);

    // in case of file creation of YAML, we receive an empty stream, so we create an empty byte array to handle it
    byte[] inputBytes = new byte[0];
    if (secretManagerConfigOptional.isPresent()) {
      if (isReadOnlySecretManager(secretManagerConfigOptional.get())) {
        throw new SecretManagementException(
            SECRET_MANAGEMENT_ERROR, "Cannot create a secret in read only secret manager", USER);
      }
      if (inputStream != null) {
        try {
          inputBytes = ByteStreams.toByteArray(inputStream);
        } catch (IOException exception) {
          throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unable to convert input stream to bytes", SRE);
        }
      }

      // decrypt secrets of secret manager before sending the config to delegate
      secretManagerConfigService.decryptEncryptionConfigSecrets(
          metadata.getAccountIdentifier(), secretManagerConfigOptional.get(), false);

      // send to delegate for saving the secret in secret manager
      EncryptedData savedEncryptedData =
          encrypt(metadata.getAccountIdentifier(), secretManagerConfigOptional.get(), dto.getName(), inputBytes, null);

      // set other fields and save secret file in DB
      savedEncryptedData.setKmsId(secretManagerConfigOptional.get().getUuid());
      savedEncryptedData.setEncryptionType(secretManagerConfigOptional.get().getEncryptionType());
      savedEncryptedData.setType(SettingVariableTypes.CONFIG_FILE);
      savedEncryptedData.setAccountId(metadata.getAccountIdentifier());
      metadata.setSecretManagerName(secretManagerConfigOptional.get().getName());
      savedEncryptedData.setNgMetadata(metadata);
      wingsPersistence.save(savedEncryptedData);

      return savedEncryptedData;
    } else {
      throw new SecretManagementException(
          SECRET_MANAGEMENT_ERROR, "No secret manager with given details found in scope.", USER);
    }
  }

  @Override
  public boolean update(@NotNull String account, String org, String project, String identifier, SecretFileUpdateDTO dto,
      @NotNull InputStream inputStream) {
    // get secret file saved in DB
    Optional<EncryptedData> encryptedDataOptional = ngSecretService.get(account, org, project, identifier);

    if (encryptedDataOptional.isPresent()) {
      EncryptedData encryptedData = encryptedDataOptional.get();
      NGEncryptedDataMetadata metadata = encryptedData.getNgMetadata();

      // In case of creating with YAML, file upload is not allowed, so we initialize inputBytes as an empty array
      byte[] inputBytes = new byte[0];
      try {
        if (inputStream != null) {
          inputBytes = ByteStreams.toByteArray(inputStream);
        }
      } catch (IOException exception) {
        throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unable to convert input stream to bytes", SRE);
      }

      // get secret manager to be used to save file
      Optional<SecretManagerConfig> secretManagerConfigOptional =
          ngSecretManagerService.get(metadata.getAccountIdentifier(), metadata.getOrgIdentifier(),
              metadata.getProjectIdentifier(), metadata.getSecretManagerIdentifier(), true);

      if (secretManagerConfigOptional.isPresent()) {
        if (isReadOnlySecretManager(secretManagerConfigOptional.get())) {
          throw new SecretManagementException(
              SECRET_MANAGEMENT_ERROR, "Cannot update a secret in read only secret manager", USER);
        }
        // If name has changed, delete the old file (we do not allow reference with files)
        if (!dto.getName().equals(encryptedData.getName())) {
          ngSecretService.deleteSecretInSecretManager(account, encryptedData, secretManagerConfigOptional.get());
        }
        char[] existingFileId = encryptedData.getEncryptedValue();

        // decrypt secrets of secret manager before sending secret manager config to delegate
        secretManagerConfigService.decryptEncryptionConfigSecrets(
            metadata.getAccountIdentifier(), secretManagerConfigOptional.get(), false);

        // save/override secret file in secret manager
        EncryptedData savedEncryptedData = encrypt(metadata.getAccountIdentifier(), secretManagerConfigOptional.get(),
            encryptedData.getName(), inputBytes, encryptedData);

        // set new fields before saving to DB
        encryptedData.setEncryptionKey(savedEncryptedData.getEncryptionKey());
        encryptedData.setEncryptedValue(savedEncryptedData.getEncryptedValue());
        encryptedData.setFileSize(savedEncryptedData.getFileSize());
        encryptedData.setName(dto.getName());
        encryptedData.getNgMetadata().setDescription(dto.getDescription());
        encryptedData.getNgMetadata().setTags(dto.getTags());

        // save to DB and return
        wingsPersistence.save(encryptedData);

        switch (secretManagerConfigOptional.get().getEncryptionType()) {
          case LOCAL:
          case GCP_KMS:
          case KMS:
            fileService.deleteFile(String.valueOf(existingFileId), CONFIGS);
            break;
          default:
        }
        return true;
      } else {
        throw new SecretManagementException(
            SECRET_MANAGEMENT_ERROR, "No secret manager with given details found in scope.", USER);
      }
    }
    throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "No such secret file found", USER);
  }

  private boolean containsIllegalCharacters(String name) {
    String[] parts = name.split(ILLEGAL_CHARACTERS, 2);
    return parts.length > 1;
  }
}
