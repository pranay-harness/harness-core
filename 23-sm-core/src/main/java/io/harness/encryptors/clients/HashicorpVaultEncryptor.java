package io.harness.encryptors.clients;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.VAULT_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.helpers.ext.vault.VaultRestClientFactory.getFullPath;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.encryptors.VaultEncryptor;
import io.harness.exception.SecretManagementDelegateException;
import io.harness.helpers.ext.vault.VaultRestClientFactory;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.VaultConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
@Slf4j
@OwnedBy(PL)
public class HashicorpVaultEncryptor implements VaultEncryptor {
  private final TimeLimiter timeLimiter;
  private final int NUM_OF_RETRIES = 3;

  @Inject
  public HashicorpVaultEncryptor(TimeLimiter timeLimiter) {
    this.timeLimiter = timeLimiter;
  }

  @Override
  public EncryptedRecord createSecret(
      String accountId, String name, String plaintext, EncryptionConfig encryptionConfig) {
    VaultConfig vaultConfig = (VaultConfig) encryptionConfig;
    int failedAttempts = 0;
    while (true) {
      try {
        return timeLimiter.callWithTimeout(
            () -> upsertSecretInternal(name, plaintext, accountId, null, vaultConfig), 5, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        if (e instanceof SecretManagementDelegateException) {
          throw(SecretManagementDelegateException) e;
        } else {
          failedAttempts++;
          logger.warn("encryption failed. trial num: {}", failedAttempts, e);
          if (failedAttempts == NUM_OF_RETRIES) {
            String message = "encryption failed after " + NUM_OF_RETRIES + " retries for vault secret " + name;
            throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
          }
          sleep(ofMillis(1000));
        }
      }
    }
  }

  @Override
  public EncryptedRecord updateSecret(String accountId, String name, String plaintext, EncryptedRecord existingRecord,
      EncryptionConfig encryptionConfig) {
    VaultConfig vaultConfig = (VaultConfig) encryptionConfig;
    int failedAttempts = 0;
    while (true) {
      try {
        return timeLimiter.callWithTimeout(
            ()
                -> upsertSecretInternal(name, plaintext, accountId, existingRecord, vaultConfig),
            5, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        if (e instanceof SecretManagementDelegateException) {
          throw(SecretManagementDelegateException) e;
        } else {
          failedAttempts++;
          logger.warn("encryption failed. trial num: {}", failedAttempts, e);
          if (failedAttempts == NUM_OF_RETRIES) {
            String message = "encryption failed after " + NUM_OF_RETRIES + " retries for vault secret " + name;
            throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
          }
          sleep(ofMillis(1000));
        }
      }
    }
  }

  @Override
  public EncryptedRecord renameSecret(
      String accountId, String name, EncryptedRecord existingRecord, EncryptionConfig encryptionConfig) {
    VaultConfig vaultConfig = (VaultConfig) encryptionConfig;
    int failedAttempts = 0;
    while (true) {
      try {
        return timeLimiter.callWithTimeout(
            () -> renameSecretInternal(name, accountId, existingRecord, vaultConfig), 5, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        if (e instanceof SecretManagementDelegateException) {
          throw(SecretManagementDelegateException) e;
        } else {
          failedAttempts++;
          logger.warn("encryption failed. trial num: {}", failedAttempts, e);
          if (failedAttempts == NUM_OF_RETRIES) {
            String message = "encryption failed after " + NUM_OF_RETRIES + " retries for vault secret " + name;
            throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
          }
          sleep(ofMillis(1000));
        }
      }
    }
  }

  @Override
  public boolean deleteSecret(String accountId, EncryptedRecord existingRecord, EncryptionConfig encryptionConfig) {
    VaultConfig vaultConfig = (VaultConfig) encryptionConfig;
    try {
      String fullPath = getFullPath(vaultConfig.getBasePath(), existingRecord.getEncryptionKey());
      return VaultRestClientFactory.create(vaultConfig)
          .deleteSecret(String.valueOf(vaultConfig.getAuthToken()), vaultConfig.getSecretEngineName(), fullPath);
    } catch (IOException e) {
      String message = "Deletion of Vault secret at " + existingRecord.getEncryptionKey() + " failed";
      throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
    }
  }

  private EncryptedRecord upsertSecretInternal(String keyUrl, String value, String accountId,
      EncryptedRecord existingRecord, VaultConfig vaultConfig) throws IOException {
    logger.info("Saving secret {} into Vault {}", keyUrl, vaultConfig.getBasePath());

    // With existing encrypted value. Need to delete it first and rewrite with new value.
    String fullPath = getFullPath(vaultConfig.getBasePath(), keyUrl);
    deleteSecret(accountId, EncryptedRecordData.builder().encryptionKey(keyUrl).build(), vaultConfig);

    boolean isSuccessful = VaultRestClientFactory.create(vaultConfig)
                               .writeSecret(String.valueOf(vaultConfig.getAuthToken()),
                                   vaultConfig.getSecretEngineName(), fullPath, value);

    if (isSuccessful) {
      logger.info("Done saving vault secret at {} in {}", keyUrl, vaultConfig.getBasePath());
      if (existingRecord != null) {
        String oldFullPath = getFullPath(vaultConfig.getBasePath(), existingRecord.getEncryptionKey());
        if (!oldFullPath.equals(fullPath)) {
          deleteSecret(accountId, existingRecord, vaultConfig);
        }
      }
      return EncryptedRecordData.builder().encryptionKey(keyUrl).encryptedValue(keyUrl.toCharArray()).build();
    } else {
      String errorMsg = "Encryption request for " + keyUrl + " was not successful.";
      logger.error(errorMsg);
      throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, errorMsg, USER);
    }
  }

  private EncryptedRecord renameSecretInternal(
      String keyUrl, String accountId, EncryptedRecord existingRecord, VaultConfig vaultConfig) throws IOException {
    char[] value = fetchSecretInternal(existingRecord, vaultConfig);
    return upsertSecretInternal(keyUrl, new String(value), accountId, existingRecord, vaultConfig);
  }

  @Override
  public boolean validateReference(String accountId, String path, EncryptionConfig encryptionConfig) {
    return isNotEmpty(fetchSecretValue(accountId, EncryptedRecordData.builder().path(path).build(), encryptionConfig));
  }

  @Override
  public char[] fetchSecretValue(String accountId, EncryptedRecord encryptedRecord, EncryptionConfig encryptionConfig) {
    if (isEmpty(encryptedRecord.getEncryptionKey()) && isEmpty(encryptedRecord.getPath())) {
      return null;
    }
    VaultConfig vaultConfig = (VaultConfig) encryptionConfig;
    int failedAttempts = 0;
    while (true) {
      try {
        return timeLimiter.callWithTimeout(
            () -> fetchSecretInternal(encryptedRecord, vaultConfig), 5, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        failedAttempts++;
        logger.warn("decryption failed. trial num: {}", failedAttempts, e);
        if (e instanceof SecretManagementDelegateException) {
          throw(SecretManagementDelegateException) e;
        } else {
          if (failedAttempts == NUM_OF_RETRIES) {
            String message = "Decryption failed after " + NUM_OF_RETRIES + " retries for secret "
                + encryptedRecord.getEncryptionKey() + " or path " + encryptedRecord.getPath();
            throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
          }
          sleep(ofMillis(1000));
        }
      }
    }
  }

  private char[] fetchSecretInternal(EncryptedRecord data, VaultConfig vaultConfig) throws IOException {
    String fullPath =
        isEmpty(data.getPath()) ? getFullPath(vaultConfig.getBasePath(), data.getEncryptionKey()) : data.getPath();
    long startTime = System.currentTimeMillis();
    logger.info("Reading secret {} from vault {}", fullPath, vaultConfig.getVaultUrl());

    String value =
        VaultRestClientFactory.create(vaultConfig)
            .readSecret(String.valueOf(vaultConfig.getAuthToken()), vaultConfig.getSecretEngineName(), fullPath);

    if (isNotEmpty(value)) {
      logger.info("Done reading secret {} from vault {} in {} ms.", fullPath, vaultConfig.getVaultUrl(),
          System.currentTimeMillis() - startTime);
      return value.toCharArray();
    } else {
      String errorMsg = "Secret key path '" + fullPath + "' is invalid.";
      logger.error(errorMsg);
      throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, errorMsg, USER);
    }
  }
}
