package software.wings.service.intfc.security;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.SRE;
import static io.harness.exception.WingsException.USER;
import static io.harness.security.encryption.EncryptionType.VAULT;
import static software.wings.service.intfc.security.SecretManager.ILLEGAL_CHARACTERS;
import static software.wings.service.intfc.security.SecretManager.containsIllegalCharacters;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import io.harness.secretmanagerclient.NGEncryptedDataMetadata;
import io.harness.stream.BoundedInputStream;
import lombok.AllArgsConstructor;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.VaultConfig;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretManagementException;
import software.wings.settings.SettingVariableTypes;

import java.io.IOException;
import java.util.Optional;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class NGSecretFileServiceImpl implements NGSecretFileService {
  private final NGSecretManagerService ngSecretManagerService;
  private final NGSecretService ngSecretService;
  private final VaultService vaultService;
  private final WingsPersistence wingsPersistence;

  private EncryptedData encrypt(
      String accountIdentifier, SecretManagerConfig secretManagerConfig, String name, byte[] bytes) {
    if (secretManagerConfig.getEncryptionType() == VAULT) {
      vaultService.decryptVaultConfigSecrets(
          secretManagerConfig.getAccountId(), (VaultConfig) secretManagerConfig, false);
      return vaultService.encryptFile(accountIdentifier, (VaultConfig) secretManagerConfig, name, bytes, null);
    }
    throw new UnsupportedOperationException(
        "Encryption type " + secretManagerConfig.getEncryptionType() + " not supported in next gen");
  }

  @Override
  public EncryptedData create(EncryptedData encryptedData, BoundedInputStream inputStream) {
    NGEncryptedDataMetadata metadata = encryptedData.getNgMetadata();

    if (Optional.ofNullable(metadata).isPresent()) {
      if (isEmpty(encryptedData.getName()) || containsIllegalCharacters(encryptedData.getName())) {
        throw new SecretManagementException(SECRET_MANAGEMENT_ERROR,
            "Encrypted file name/identifier should not have any of the following characters " + ILLEGAL_CHARACTERS,
            USER);
      }

      Optional<EncryptedData> encryptedDataOptional = ngSecretService.get(metadata.getAccountIdentifier(),
          metadata.getOrgIdentifier(), metadata.getProjectIdentifier(), metadata.getIdentifier());
      if (encryptedDataOptional.isPresent()) {
        throw new SecretManagementException(
            SECRET_MANAGEMENT_ERROR, "Encrypted file with same identifier exists in the scope", USER);
      }

      Optional<SecretManagerConfig> secretManagerConfigOptional =
          ngSecretManagerService.getSecretManager(metadata.getAccountIdentifier(), metadata.getOrgIdentifier(),
              metadata.getProjectIdentifier(), metadata.getSecretManagerIdentifier());
      byte[] inputBytes;
      if (secretManagerConfigOptional.isPresent()) {
        try {
          inputBytes = ByteStreams.toByteArray(inputStream);
        } catch (IOException exception) {
          throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unable to convert input stream to bytes", SRE);
        }
        EncryptedData savedEncryptedData = encrypt(
            metadata.getAccountIdentifier(), secretManagerConfigOptional.get(), encryptedData.getName(), inputBytes);
        encryptedData.setEncryptionKey(savedEncryptedData.getEncryptionKey());
        encryptedData.setEncryptedValue(savedEncryptedData.getEncryptedValue());
        encryptedData.setKmsId(secretManagerConfigOptional.get().getUuid());
        encryptedData.setEncryptionType(secretManagerConfigOptional.get().getEncryptionType());
        encryptedData.setType(SettingVariableTypes.CONFIG_FILE);
        encryptedData.setAccountId(metadata.getAccountIdentifier());
        encryptedData.setFileSize(inputBytes.length);
        wingsPersistence.save(encryptedData);
        return encryptedData;
      } else {
        throw new InvalidRequestException("No secret manager with given details found in scope.", USER);
      }
    } else {
      throw new InvalidRequestException("NG Metadata not present in encrypted data", SRE);
    }
  }

  @Override
  public boolean update(EncryptedData encryptedData, BoundedInputStream inputStream) {
    NGEncryptedDataMetadata metadata = encryptedData.getNgMetadata();
    if (Optional.ofNullable(metadata).isPresent()) {
      byte[] inputBytes;
      try {
        inputBytes = ByteStreams.toByteArray(inputStream);
      } catch (IOException exception) {
        throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unable to convert input stream to bytes", SRE);
      }
      Optional<SecretManagerConfig> secretManagerConfigOptional =
          ngSecretManagerService.getSecretManager(metadata.getAccountIdentifier(), metadata.getOrgIdentifier(),
              metadata.getProjectIdentifier(), metadata.getSecretManagerIdentifier());
      if (secretManagerConfigOptional.isPresent()) {
        EncryptedData savedEncryptedData = encrypt(
            metadata.getAccountIdentifier(), secretManagerConfigOptional.get(), encryptedData.getName(), inputBytes);
        encryptedData.setEncryptionKey(savedEncryptedData.getEncryptionKey());
        encryptedData.setEncryptedValue(savedEncryptedData.getEncryptedValue());
        encryptedData.setKmsId(secretManagerConfigOptional.get().getUuid());
        encryptedData.setEncryptionType(secretManagerConfigOptional.get().getEncryptionType());
        encryptedData.setType(SettingVariableTypes.CONFIG_FILE);
        encryptedData.setAccountId(metadata.getAccountIdentifier());
        encryptedData.setFileSize(inputBytes.length);
        wingsPersistence.save(encryptedData);
        return true;
      } else {
        throw new InvalidRequestException("No secret manager with given details found in scope.", USER);
      }
    } else {
      throw new InvalidRequestException("NG Metadata not present in encrypted data");
    }
  }
}
