package software.wings.service.intfc.security;

import software.wings.annotation.Encryptable;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

/**
 * Created by rsingh on 6/7/18.
 */
public interface ManagerDecryptionService {
  Encryptable decrypt(Encryptable object, List<EncryptedDataDetail> encryptedDataDetails);
}
