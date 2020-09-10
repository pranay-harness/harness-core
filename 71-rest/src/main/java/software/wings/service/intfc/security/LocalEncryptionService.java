package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.LocalEncryptionConfig;
import software.wings.security.encryption.EncryptedData;

import java.io.File;
import java.io.OutputStream;

/**
 * @author marklu on 2019-05-14
 */
@OwnedBy(PL)
public interface LocalEncryptionService {
  EncryptedData encrypt(char[] value, String accountId, LocalEncryptionConfig localEncryptionConfig);

  char[] decrypt(EncryptedData data, String accountId, LocalEncryptionConfig localEncryptionConfig);

  LocalEncryptionConfig getEncryptionConfig(String accountId);

  EncryptedData encryptFile(
      String accountId, LocalEncryptionConfig localEncryptionConfig, String name, byte[] inputBytes);

  File decryptFile(File file, String accountId, EncryptedData encryptedData);

  void decryptToStream(String accountId, EncryptedData encryptedData, OutputStream output);

  String saveLocalEncryptionConfig(String accountId, LocalEncryptionConfig localEncryptionConfig);
}
