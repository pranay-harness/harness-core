package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.KmsConfig;
import software.wings.security.encryption.EncryptedData;

import java.io.File;
import java.io.OutputStream;

/**
 * Created by rsingh on 9/29/17.
 */
@OwnedBy(PL)
public interface KmsService {
  EncryptedData encrypt(char[] value, String accountId, KmsConfig kmsConfig);

  char[] decrypt(EncryptedData data, String accountId, KmsConfig kmsConfig);

  KmsConfig getKmsConfig(String accountId, String entityId);

  String saveGlobalKmsConfig(String accountId, KmsConfig kmsConfig);

  KmsConfig getGlobalKmsConfig();

  String saveKmsConfig(String accountId, KmsConfig kmsConfig);

  boolean deleteKmsConfig(String accountId, String kmsConfigId);

  void decryptKmsConfigSecrets(String accountId, KmsConfig kmsConfig, boolean maskSecret);

  EncryptedData encryptFile(String accountId, KmsConfig kmsConfig, String name, byte[] inputBytes);

  File decryptFile(File file, String accountId, EncryptedData encryptedData);

  void decryptToStream(File file, String accountId, EncryptedData encryptedData, OutputStream output);
}
