package software.wings.service.impl.security.kms;

import static io.harness.data.encoding.EncodingUtils.decodeBase64;
import static io.harness.data.encoding.EncodingUtils.encodeBase64ToByteArray;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.security.SimpleEncryption.CHARSET;
import static io.harness.security.encryption.SecretManagerType.KMS;
import static io.harness.security.encryption.SecretManagerType.VAULT;

import io.harness.beans.SecretManagerConfig;
import io.harness.encryptors.KmsEncryptor;
import io.harness.encryptors.KmsEncryptorsRegistry;
import io.harness.encryptors.VaultEncryptor;
import io.harness.encryptors.VaultEncryptorsRegistry;
import io.harness.exception.SecretManagementDelegateException;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.nio.ByteBuffer;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class TerraformPlanEncryptDecryptHelper {
  @Inject protected KmsEncryptorsRegistry kmsEncryptorsRegistry;
  @Inject protected VaultEncryptorsRegistry vaultEncryptorsRegistry;

  public EncryptedRecord encryptTerraformPlan(byte[] content, String name, EncryptionConfig config) {
    String value = new String(CHARSET.decode(ByteBuffer.wrap(encodeBase64ToByteArray(content))).array());
    EncryptedRecordData record;
    if (KMS == config.getType()) {
      record = (EncryptedRecordData) encryptKmsSecret(value, config);
    } else if (VAULT == config.getType()) {
      record = (EncryptedRecordData) encryptVaultSecret(name, value, config);
    } else {
      throw new SecretManagementDelegateException(SECRET_MANAGEMENT_ERROR,
          String.format("Encryptor for fetch secret task for encryption config %s not configured", config.getName()),
          USER);
    }
    record.setUuid(generateUuid());
    return record;
  }

  public EncryptedRecord encryptKmsSecret(String value, EncryptionConfig encryptionConfig) {
    KmsEncryptor kmsEncryptor = kmsEncryptorsRegistry.getKmsEncryptor(encryptionConfig);
    return kmsEncryptor.encryptSecret(encryptionConfig.getAccountId(), value, encryptionConfig);
  }

  public EncryptedRecord encryptVaultSecret(String name, String value, EncryptionConfig encryptionConfig) {
    VaultEncryptor vaultEncryptor = vaultEncryptorsRegistry.getVaultEncryptor(encryptionConfig.getEncryptionType());
    return vaultEncryptor.createSecret(encryptionConfig.getAccountId(), name, value, encryptionConfig);
  }

  public byte[] getDecryptedTerraformPlan(EncryptionConfig config, EncryptedRecord record) {
    char[] decryptedTerraformPlan;
    if (KMS == config.getType()) {
      decryptedTerraformPlan = fetchKmsSecretValue(record, config);
    } else if (VAULT == config.getType()) {
      decryptedTerraformPlan = fetchVaultSecretValue(record, config);
    } else {
      throw new SecretManagementDelegateException(SECRET_MANAGEMENT_ERROR,
          String.format("Encryptor for fetch secret task for encryption config %s not configured", config.getName()),
          USER);
    }
    return decodeBase64(decryptedTerraformPlan);
  }

  public char[] fetchKmsSecretValue(EncryptedRecord record, EncryptionConfig config) {
    KmsEncryptor kmsEncryptor = kmsEncryptorsRegistry.getKmsEncryptor(config);
    return kmsEncryptor.fetchSecretValue(config.getAccountId(), record, config);
  }

  public char[] fetchVaultSecretValue(EncryptedRecord record, EncryptionConfig config) {
    VaultEncryptor vaultEncryptor = vaultEncryptorsRegistry.getVaultEncryptor(config.getEncryptionType());
    return vaultEncryptor.fetchSecretValue(config.getAccountId(), record, config);
  }

  public boolean deleteTfPlanFromVault(SecretManagerConfig config, EncryptedRecord record) {
    // Only for Vault type Secret Manager, Plan is saved in Secret Manager
    if (VAULT == config.getType()) {
      VaultEncryptor vaultEncryptor = vaultEncryptorsRegistry.getVaultEncryptor(config.getEncryptionType());
      return vaultEncryptor.deleteSecret(config.getAccountId(), record, config);
    }
    return false;
  }
}
