package software.wings.security.encryption;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by mike@ on 4/24/17.
 */
public interface EncryptionInterface {
  public byte[] encrypt(byte[] content, char[] passphrase)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
             InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException;
  public byte[] decrypt(byte[] encrypted, char[] passphrase)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
             InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException;
}
