/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.helpers;

import static io.harness.data.encoding.EncodingUtils.decodeBase64;
import static io.harness.data.encoding.EncodingUtils.encodeBase64ToByteArray;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.security.SimpleEncryption.CHARSET;
import static io.harness.security.encryption.SecretManagerType.KMS;
import static io.harness.security.encryption.SecretManagerType.VAULT;

import io.harness.encryptors.KmsEncryptor;
import io.harness.encryptors.KmsEncryptorsRegistry;
import io.harness.encryptors.VaultEncryptor;
import io.harness.encryptors.VaultEncryptorsRegistry;
import io.harness.exception.SecretManagementDelegateException;
import io.harness.secretmanagerclient.EncryptDecryptHelper;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.nio.ByteBuffer;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class EncryptDecryptHelperImpl implements EncryptDecryptHelper {
  @Inject protected KmsEncryptorsRegistry kmsEncryptorsRegistry;
  @Inject protected VaultEncryptorsRegistry vaultEncryptorsRegistry;

  @Override
  public EncryptedRecord encryptContent(byte[] content, String name, EncryptionConfig config) {
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

  private EncryptedRecord encryptKmsSecret(String value, EncryptionConfig encryptionConfig) {
    KmsEncryptor kmsEncryptor = kmsEncryptorsRegistry.getKmsEncryptor(encryptionConfig);
    return kmsEncryptor.encryptSecret(encryptionConfig.getAccountId(), value, encryptionConfig);
  }

  private EncryptedRecord encryptVaultSecret(String name, String value, EncryptionConfig encryptionConfig) {
    VaultEncryptor vaultEncryptor = vaultEncryptorsRegistry.getVaultEncryptor(encryptionConfig.getEncryptionType());
    return vaultEncryptor.createSecret(encryptionConfig.getAccountId(), name, value, encryptionConfig);
  }

  @Override
  public byte[] getDecryptedContent(EncryptionConfig config, EncryptedRecord record) {
    char[] decryptedContent;
    if (KMS == config.getType()) {
      decryptedContent = fetchKmsSecretValue(record, config);
    } else if (VAULT == config.getType()) {
      decryptedContent = fetchVaultSecretValue(record, config);
    } else {
      throw new SecretManagementDelegateException(SECRET_MANAGEMENT_ERROR,
          String.format("Encryptor for fetch secret task for encryption config %s not configured", config.getName()),
          USER);
    }
    return decodeBase64(decryptedContent);
  }

  private char[] fetchKmsSecretValue(EncryptedRecord record, EncryptionConfig config) {
    KmsEncryptor kmsEncryptor = kmsEncryptorsRegistry.getKmsEncryptor(config);
    return kmsEncryptor.fetchSecretValue(config.getAccountId(), record, config);
  }

  private char[] fetchVaultSecretValue(EncryptedRecord record, EncryptionConfig config) {
    VaultEncryptor vaultEncryptor = vaultEncryptorsRegistry.getVaultEncryptor(config.getEncryptionType());
    return vaultEncryptor.fetchSecretValue(config.getAccountId(), record, config);
  }

  @Override
  public boolean deleteEncryptedRecord(EncryptionConfig encryptionConfig, EncryptedRecord record) {
    // Only for Vault type Secret Manager, Plan is saved in Secret Manager
    if (VAULT == encryptionConfig.getType()) {
      VaultEncryptor vaultEncryptor = vaultEncryptorsRegistry.getVaultEncryptor(encryptionConfig.getEncryptionType());
      return vaultEncryptor.deleteSecret(encryptionConfig.getAccountId(), record, encryptionConfig);
    }
    return false;
  }
}
