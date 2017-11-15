package software.wings.service.intfc.security;

import software.wings.beans.KmsConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.utils.BoundedInputStream;

import java.io.File;
import java.util.Collection;

/**
 * Created by rsingh on 9/29/17.
 */
public interface KmsService {
  EncryptedData encrypt(char[] value, String accountId, KmsConfig kmsConfig);

  char[] decrypt(EncryptedData data, String accountId, KmsConfig kmsConfig);

  KmsConfig getSecretConfig(String accountId);

  KmsConfig getKmsConfig(String accountId, String entityId);

  String saveGlobalKmsConfig(String accountId, KmsConfig kmsConfig);

  String saveKmsConfig(String accountId, KmsConfig kmsConfig);

  boolean deleteKmsConfig(String accountId, String kmsConfigId);

  Collection<KmsConfig> listKmsConfigs(String accountId, boolean maskSecret);

  boolean transitionKms(String accountId, String fromKmsId, String toKmsId);

  void changeKms(String accountId, String entityId, String fromKmsId, String toKmsId);

  EncryptedData encryptFile(BoundedInputStream inputStream, String accountId, String uuid);

  File decryptFile(File file, String accountId, EncryptedData encryptedData);
}
