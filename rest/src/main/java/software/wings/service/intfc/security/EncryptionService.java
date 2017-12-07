package software.wings.service.intfc.security;

import software.wings.annotation.Encryptable;
import software.wings.security.encryption.EncryptedDataDetail;

import java.io.IOException;
import java.util.List;

/**
 * Created by rsingh on 10/18/17.
 */
public interface EncryptionService {
  void decrypt(Encryptable object, List<EncryptedDataDetail> encryptedDataDetails);

  char[] getDecryptedValue(EncryptedDataDetail encryptedDataDetail) throws IOException;
}
