package software.wings.security.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.charset.StandardCharsets;
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
public class HardcodedEncryptionTest {
  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldEncryptAndDecrypt()
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException,
             InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException {
    String testInput = "abc";
    HardcodedEncryption encryption = new HardcodedEncryption();
    byte[] encryptedBytes = encryption.encrypt(testInput.getBytes(StandardCharsets.UTF_8));
    String encryptedString = new String(encryptedBytes, StandardCharsets.UTF_8);
    assertThat(testInput).isNotEqualTo(encryptedString);
    byte[] decryptedBytes = encryption.decrypt(encryptedBytes);
    String decryptedString = new String(decryptedBytes, StandardCharsets.UTF_8);
    assertThat(testInput).isEqualTo(decryptedString);
  }

  @Test
  public void shouldEncryptAndDecryptWithCustomKey()
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
             InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
    char[] KEY = "abcdefghijklmnop".toCharArray();
    String testInput = "abc";
    HardcodedEncryption encryption = new HardcodedEncryption();
    byte[] encryptedBytes = encryption.encrypt(testInput.getBytes(StandardCharsets.UTF_8), KEY);
    String encryptedString = new String(encryptedBytes, StandardCharsets.UTF_8);
    assertThat(testInput).isNotEqualTo(encryptedString);
    byte[] decryptedBytes = encryption.decrypt(encryptedBytes, KEY);
    String decryptedString = new String(decryptedBytes, StandardCharsets.UTF_8);
    assertThat(testInput).isEqualTo(decryptedString);
  }

  @Test
  public void shouldFailWithIncorrectKeyLength()
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
             InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
    thrown.expect(InvalidKeyException.class);
    char[] KEY = "abc".toCharArray();
    String testInput = "abc";
    HardcodedEncryption encryption = new HardcodedEncryption();
    byte[] encryptedBytes = encryption.encrypt(testInput.getBytes(StandardCharsets.UTF_8), KEY);
  }
}
