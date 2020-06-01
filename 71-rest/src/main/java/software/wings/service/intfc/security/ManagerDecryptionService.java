package software.wings.service.intfc.security;

import io.harness.security.encryption.EncryptableSettingWithEncryptionDetails;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.annotation.EncryptableSetting;

import java.util.List;

/**
 * Created by rsingh on 6/7/18.
 */
public interface ManagerDecryptionService {
  void decrypt(EncryptableSetting object, List<EncryptedDataDetail> encryptedDataDetails);

  void decrypt(
      String accountId, List<EncryptableSettingWithEncryptionDetails> encryptableSettingWithEncryptionDetailsList);

  char[] fetchSecretValue(String accountId, EncryptedDataDetail encryptedDataDetail);
}
