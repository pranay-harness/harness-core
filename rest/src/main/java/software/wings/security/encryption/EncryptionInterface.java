package software.wings.security.encryption;

import javax.crypto.SecretKey;

/**
 * Created by mike@ on 4/24/17.
 */
public interface EncryptionInterface {
  public EncryptionType getEncryptionType();
  public SecretKey getSecretKey();

  public byte[] getSalt();
  public void setSalt(byte[] salt);

  public byte[] encrypt(byte[] content);

  public byte[] decrypt(byte[] encrypted);
}
