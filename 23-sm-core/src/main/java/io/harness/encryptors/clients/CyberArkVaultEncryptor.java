package io.harness.encryptors.clients;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.CYBERARK_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.encryptors.VaultEncryptor;
import io.harness.exception.SecretManagementDelegateException;
import io.harness.helpers.ext.cyberark.CyberArkReadResponse;
import io.harness.helpers.ext.cyberark.CyberArkRestClientFactory;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;
import software.wings.beans.CyberArkConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
@Slf4j
@OwnedBy(PL)
public class CyberArkVaultEncryptor implements VaultEncryptor {
  private final TimeLimiter timeLimiter;

  @Inject
  public CyberArkVaultEncryptor(TimeLimiter timeLimiter) {
    this.timeLimiter = timeLimiter;
  }

  @Override
  public EncryptedRecord createSecret(
      String accountId, String name, String plaintext, EncryptionConfig encryptionConfig) {
    throw new UnsupportedOperationException();
  }

  @Override
  public EncryptedRecord updateSecret(String accountId, String name, String plaintext, EncryptedRecord existingRecord,
      EncryptionConfig encryptionConfig) {
    throw new UnsupportedOperationException();
  }

  @Override
  public EncryptedRecord renameSecret(
      String accountId, String name, EncryptedRecord existingRecord, EncryptionConfig encryptionConfig) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean deleteSecret(String accountId, EncryptedRecord existingRecord, EncryptionConfig encryptionConfig) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean validateReference(String accountId, String path, EncryptionConfig encryptionConfig) {
    return isNotEmpty(fetchSecretValue(accountId, EncryptedRecordData.builder().path(path).build(), encryptionConfig));
  }

  @Override
  public char[] fetchSecretValue(String accountId, EncryptedRecord encryptedRecord, EncryptionConfig encryptionConfig) {
    final int NUM_OF_RETRIES = 3;
    CyberArkConfig cyberArkConfig = (CyberArkConfig) encryptionConfig;
    int failedAttempts = 0;
    while (true) {
      try {
        return timeLimiter.callWithTimeout(
            () -> fetchValueInternal(encryptedRecord.getPath(), cyberArkConfig), 5, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        failedAttempts++;
        logger.warn("decryption failed. trial num: {}", failedAttempts, e);
        if (e instanceof SecretManagementDelegateException) {
          throw(SecretManagementDelegateException) e;
        } else {
          if (failedAttempts == NUM_OF_RETRIES) {
            String message = "Decryption of CyberArk secret at path %s failed after %s retries";
            throw new SecretManagementDelegateException(
                CYBERARK_OPERATION_ERROR, String.format(message, encryptedRecord.getPath(), NUM_OF_RETRIES), e, USER);
          }
          sleep(ofMillis(1000));
        }
      }
    }
  }

  private char[] fetchValueInternal(String query, CyberArkConfig cyberArkConfig) {
    String appId = cyberArkConfig.getAppId();
    // Sample query params: "Address=components;Username=svc_account" or
    // "Safe=Test;Folder=root\OS\Windows;Object=windows1"

    if (isEmpty(query)) {
      String errorMessage =
          "Query parameter is mandatory but it's not present in the encrypted record for CyberArk secret manager "
          + cyberArkConfig.getUuid();
      throw new IllegalArgumentException(errorMessage);
    }

    long startTime = System.currentTimeMillis();
    logger.info(
        "Reading secret CyberArk {} using AppID '{}' and query '{}'", cyberArkConfig.getCyberArkUrl(), appId, query);

    String secretValue = null;
    try {
      Response<CyberArkReadResponse> response =
          CyberArkRestClientFactory.create(cyberArkConfig).readSecret(appId, query).execute();
      if (response != null && response.isSuccessful()) {
        secretValue = response.body().getContent();
      }
    } catch (IOException e) {
      logger.error("Failed to read secret from CyberArk", e);
    }

    if (isNotEmpty(secretValue)) {
      logger.info("Done reading secret {} from CyberArk {} in {} ms.", query, cyberArkConfig.getCyberArkUrl(),
          System.currentTimeMillis() - startTime);
      return secretValue.toCharArray();
    } else {
      String errorMsg = "CyberArk query '" + query + "' is invalid in application '" + appId + "'";
      logger.error(errorMsg);
      throw new SecretManagementDelegateException(CYBERARK_OPERATION_ERROR, errorMsg, USER);
    }
  }
}
