package software.wings.service.intfc.security;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.service.DelegateAgentFileService.FileBucket.CONFIGS;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.SRE;
import static io.harness.exception.WingsException.USER;
import static software.wings.service.intfc.security.SecretManager.ILLEGAL_CHARACTERS;
import static software.wings.service.intfc.security.SecretManager.containsIllegalCharacters;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.secretmanagerclient.NGEncryptedDataMetadata;
import io.harness.secretmanagerclient.dto.SecretFileDTO;
import io.harness.secretmanagerclient.dto.SecretFileUpdateDTO;
import io.harness.stream.BoundedInputStream;
import lombok.AllArgsConstructor;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.LocalEncryptionConfig;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.VaultConfig;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretManagementException;
import software.wings.service.intfc.FileService;
import software.wings.settings.SettingVariableTypes;

import java.io.IOException;
import java.util.Optional;
import javax.validation.constraints.NotNull;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class NGSecretFileServiceImpl implements NGSecretFileService {
  private final NGSecretManagerService ngSecretManagerService;
  private final NGSecretService ngSecretService;
  private final VaultService vaultService;
  private final GcpKmsService gcpKmsService;
  private final LocalEncryptionService localEncryptionService;
  private final WingsPersistence wingsPersistence;
  private final SecretManagerConfigService secretManagerConfigService;
  private final FileService fileService;

  private EncryptedData encrypt(String accountIdentifier, SecretManagerConfig secretManagerConfig, String name,
      byte[] bytes, EncryptedData savedEncryptedData) {
    switch (secretManagerConfig.getEncryptionType()) {
      case VAULT:
        return vaultService.encryptFile(
            accountIdentifier, (VaultConfig) secretManagerConfig, name, bytes, savedEncryptedData);
      case GCP_KMS:
        return gcpKmsService.encryptFile(
            accountIdentifier, (GcpKmsConfig) secretManagerConfig, name, bytes, savedEncryptedData);
      case LOCAL:
        return localEncryptionService.encryptFile(
            accountIdentifier, (LocalEncryptionConfig) secretManagerConfig, name, bytes);
      default:
        throw new UnsupportedOperationException(
            "Encryption type " + secretManagerConfig.getEncryptionType() + " not supported in next gen");
    }
  }

  @Override
  public EncryptedData create(SecretFileDTO dto, @NotNull BoundedInputStream inputStream) {
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
      throw new SecretManagementException(
          SECRET_MANAGEMENT_ERROR, "Encrypted file with same identifier exists in the scope", USER);
    }

    // get secret manager with which the file is to be encrypted
    Optional<SecretManagerConfig> secretManagerConfigOptional =
        ngSecretManagerService.getSecretManager(metadata.getAccountIdentifier(), metadata.getOrgIdentifier(),
            metadata.getProjectIdentifier(), metadata.getSecretManagerIdentifier());

    // in case of file creation of YAML, we receive an empty stream, so we create an empty byte array to handle it
    byte[] inputBytes = new byte[0];
    if (secretManagerConfigOptional.isPresent()) {
      if (inputStream.getTotalBytesRead() > 0) {
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
      metadata.setSecretManagerName(secretManagerConfigOptional.get().getName()); // TODO{phoenikx} remove this later
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
      @NotNull BoundedInputStream inputStream) {
    // get secret file saved in DB
    Optional<EncryptedData> encryptedDataOptional = ngSecretService.get(account, org, project, identifier);

    if (encryptedDataOptional.isPresent()) {
      EncryptedData encryptedData = encryptedDataOptional.get();
      NGEncryptedDataMetadata metadata = encryptedData.getNgMetadata();

      // In case of creating with YAML, file upload is not allowed, so we initialize inputBytes as an empty array
      byte[] inputBytes = new byte[0];
      try {
        if (inputStream.getTotalBytesRead() > 0) {
          inputBytes = ByteStreams.toByteArray(inputStream);
        }
      } catch (IOException exception) {
        throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unable to convert input stream to bytes", SRE);
      }

      // get secret manager to be used to save file
      Optional<SecretManagerConfig> secretManagerConfigOptional =
          ngSecretManagerService.getSecretManager(metadata.getAccountIdentifier(), metadata.getOrgIdentifier(),
              metadata.getProjectIdentifier(), metadata.getSecretManagerIdentifier());

      if (secretManagerConfigOptional.isPresent()) {
        // If name has changed, delete the old file (we do not allow reference with files)
        if (!dto.getName().equals(encryptedData.getName())) {
          switch (secretManagerConfigOptional.get().getEncryptionType()) {
            case LOCAL:
            case GCP_KMS:
              fileService.deleteFile(String.valueOf(encryptedData.getEncryptedValue()), CONFIGS);
              break;
            default:
              ngSecretService.deleteSecretInSecretManager(
                  account, encryptedData.getEncryptionKey(), secretManagerConfigOptional.get());
          }
        }

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
        return true;
      } else {
        throw new SecretManagementException(
            SECRET_MANAGEMENT_ERROR, "No secret manager with given details found in scope.", USER);
      }
    }
    throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "No such secret file found", USER);
  }
}
