package software.wings.service.intfc.security;

import software.wings.beans.GcpKmsConfig;
import software.wings.security.encryption.EncryptedData;

import java.io.File;
import java.io.OutputStream;

public interface GcpKmsService {
  EncryptedData encrypt(String value, String accountId, GcpKmsConfig gcpKmsConfig, EncryptedData encryptedData);

  char[] decrypt(EncryptedData data, String accountId, GcpKmsConfig gcpKmsConfig);

  EncryptedData encryptFile(
      String accountId, GcpKmsConfig gcpKmsConfig, String name, byte[] inputBytes, EncryptedData savedEncryptedData);

  File decryptFile(File file, String accountId, EncryptedData encryptedData);

  void decryptToStream(File file, String accountId, EncryptedData encryptedData, OutputStream output);
}
