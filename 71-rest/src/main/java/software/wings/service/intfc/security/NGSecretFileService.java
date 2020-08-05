package software.wings.service.intfc.security;

import io.harness.stream.BoundedInputStream;
import software.wings.security.encryption.EncryptedData;

public interface NGSecretFileService {
  EncryptedData create(EncryptedData encryptedData, BoundedInputStream file);

  boolean update(EncryptedData encryptedData, BoundedInputStream file);
}
