package software.wings.security.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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
  @Category(UnitTests.class)
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
  @Category(UnitTests.class)
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
  @Category(UnitTests.class)
  public void testEncryptDecryptCharsWithCustomKey() {
    String testInput = "test";
    SimpleEncryption encryption = new SimpleEncryption("kmpySmUISimoRrJL6NL73w");
    char[] encryptedChars = encryption.encryptChars(testInput.toCharArray());
    String encryptedString = new String(encryptedChars);
    logger.info("encryptedString: {}", encryptedString);
    assertThat(testInput).isNotEqualTo(encryptedString);
    char[] decryptedChars = encryption.decryptChars(encryptedString.toCharArray());
    String decryptedString = new String(decryptedChars);
    logger.info("decryptedString: {}", decryptedString);
    assertThat(testInput).isEqualTo(decryptedString);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldFailWithIncorrectKeyLength() {
    thrown.expect(WingsException.class);
    thrown.expectMessage(EncryptionUtils.DEFAULT_SALT_SIZE + " characters");
    char[] KEY = "abc".toCharArray();
    String testInput = "abc";
    SimpleEncryption encryption = new SimpleEncryption(KEY);
    encryption.encrypt(testInput.getBytes(StandardCharsets.ISO_8859_1));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldHaveJCEEnabled() {
    try {
      int maxKeyLength = Cipher.getMaxAllowedKeyLength("AES");
      assertThat(maxKeyLength).isEqualTo(2147483647);
    } catch (NoSuchAlgorithmException exception) {
      logger.error("", exception);
    }
  }
}
