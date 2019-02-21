package software.wings.service.impl.security;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KmsConfig;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.intfc.security.KmsService;

import java.util.UUID;

/**
 * Created by rsingh on 11/6/17.
 */
public abstract class AbstractSecretServiceImpl {
  protected static final Logger logger = LoggerFactory.getLogger(AbstractSecretServiceImpl.class);
  @Inject protected WingsPersistence wingsPersistence;
  @Inject protected DelegateProxyFactory delegateProxyFactory;
  @Inject private KmsService kmsService;

  EncryptedData encryptLocal(char[] value) {
    final String encryptionKey = UUID.randomUUID().toString();
    final SimpleEncryption simpleEncryption = new SimpleEncryption(encryptionKey);
    char[] encryptChars = simpleEncryption.encryptChars(value);

    return EncryptedData.builder()
        .encryptionKey(encryptionKey)
        .encryptedValue(encryptChars)
        .encryptionType(EncryptionType.LOCAL)
        .build();
  }

  char[] decryptLocal(EncryptedData data) {
    final SimpleEncryption simpleEncryption = new SimpleEncryption(data.getEncryptionKey());
    return simpleEncryption.decryptChars(data.getEncryptedValue());
  }

  char[] decryptKey(char[] key) {
    final EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, new String(key));
    return decryptLocal(encryptedData);
  }

  char[] decryptVaultToken(EncryptedData encryptedVaultToken) {
    EncryptionType encryptionType = encryptedVaultToken.getEncryptionType();
    String accountId = encryptedVaultToken.getAccountId();
    switch (encryptionType) {
      case LOCAL:
        // Newly created Vault config should never be encrypted by KMS.
        return decryptLocal(encryptedVaultToken);
      case KMS:
        // This is for backward compatibility. Existing vault configs may still have their root token
        // encrypted by KMS.
        KmsConfig kmsConfig = kmsService.getKmsConfig(accountId, encryptedVaultToken.getKmsId());
        char[] decrypted = kmsService.decrypt(encryptedVaultToken, accountId, kmsConfig);

        // Runtime migration of vault root token from KMS to LOCAL at time of reading
        try {
          EncryptedData reencryptedData = encryptLocal(decrypted);
          encryptedVaultToken.setEncryptionType(reencryptedData.getEncryptionType());
          encryptedVaultToken.setEncryptionKey(reencryptedData.getEncryptionKey());
          encryptedVaultToken.setEncryptedValue(reencryptedData.getEncryptedValue());
          encryptedVaultToken.setKmsId(null);
          wingsPersistence.save(encryptedVaultToken);
          logger.info(
              "Successfully migrated vault token {} from KMS to LOCAL encryption.", encryptedVaultToken.getName());
        } catch (WingsException e) {
          logger.warn(
              "Failed in migrating vault token " + encryptedVaultToken.getName() + " from KMS to LOCAL encryption.", e);
        }

        return decrypted;
      default:
        throw new IllegalStateException("Unexpected Vault root token encryption type: " + encryptionType);
    }
  }
}
