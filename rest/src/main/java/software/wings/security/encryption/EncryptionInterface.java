package software.wings.security.encryption;

import javax.crypto.SecretKey;

/**
 * Created by mike@ on 4/24/17.
 */
public interface EncryptionInterface {
  SecretKey getSecretKey();

  byte[] getSalt();
  void setSalt(byte[] salt);
  byte[] encrypt(byte[] content);

  byte[] decrypt(byte[] encrypted);
}
