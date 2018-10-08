package software.wings.service.intfc.security;

import software.wings.annotation.EncryptableSetting;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

/**
 * Created by rsingh on 6/7/18.
 */
public interface ManagerDecryptionService {
  void decrypt(EncryptableSetting object, List<EncryptedDataDetail> encryptedDataDetails);
}
