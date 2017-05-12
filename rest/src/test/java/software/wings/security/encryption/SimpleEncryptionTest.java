package software.wings.security.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.wings.exception.WingsException;

import java.nio.charset.StandardCharsets;

/**
 * Created by mike@ on 4/24/17.
 */
public class SimpleEncryptionTest {
  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldEncryptAndDecrypt() {
    String testInput = "abc";
    SimpleEncryption encryption = new SimpleEncryption();
    byte[] encryptedBytes = encryption.encrypt(testInput.getBytes(StandardCharsets.ISO_8859_1));
    String encryptedString = new String(encryptedBytes, StandardCharsets.ISO_8859_1);
    assertThat(testInput).isNotEqualTo(encryptedString);
    byte[] decryptedBytes = encryption.decrypt(encryptedBytes);
    String decryptedString = new String(decryptedBytes, StandardCharsets.ISO_8859_1);
    assertThat(testInput).isEqualTo(decryptedString);
  }

  @Test
  public void shouldEncryptAndDecryptWithCustomKey() {
    char[] KEY = "abcdefghijklmnopabcdefghijklmnop".toCharArray();
    String testInput = "abc";
    SimpleEncryption encryption = new SimpleEncryption(KEY);
    byte[] encryptedBytes = encryption.encrypt(testInput.getBytes(StandardCharsets.ISO_8859_1));
    String encryptedString = new String(encryptedBytes, StandardCharsets.ISO_8859_1);
    assertThat(testInput).isNotEqualTo(encryptedString);
    byte[] decryptedBytes = encryption.decrypt(encryptedBytes);
    String decryptedString = new String(decryptedBytes, StandardCharsets.ISO_8859_1);
    assertThat(testInput).isEqualTo(decryptedString);
  }

  @Test
  public void shouldFailWithIncorrectKeyLength() {
    thrown.expect(WingsException.class);
    thrown.expectMessage(EncryptionUtils.DEFAULT_SALT_SIZE + " characters");
    char[] KEY = "abc".toCharArray();
    String testInput = "abc";
    SimpleEncryption encryption = new SimpleEncryption(KEY);
    byte[] encryptedBytes = encryption.encrypt(testInput.getBytes(StandardCharsets.ISO_8859_1));
  }
}
