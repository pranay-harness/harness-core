package software.wings.security.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.exception.WingsException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.utils.Util;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;

/**
 * Created by mike@ on 4/24/17.
 */
public class SimpleEncryptionTest {
  private static final Logger logger = LoggerFactory.getLogger(Util.class);

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

  @Test
  public void shouldHaveJCEEnabled() {
    try {
      int maxKeyLength = Cipher.getMaxAllowedKeyLength("AES");
      assertThat(maxKeyLength).isEqualTo(2147483647);
    } catch (NoSuchAlgorithmException exception) {
      logger.error("", exception);
    }
  }
}
