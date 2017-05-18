package software.wings.security.encryption;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Utility classes used for encryption-related work.
 * Created by mike@ on 4/25/17.
 */
public class EncryptionUtils {
  public static final int DEFAULT_SALT_SIZE = 32;

  /**
   * Generate a salt to use for encryption.
   * @param bytes the length of the salt in bytes
   * @return a byte array containing the salt to use
   */
  public static byte[] generateSalt(int bytes) {
    SecureRandom random = new SecureRandom();
    byte salt[] = new byte[bytes];
    random.nextBytes(salt);
    return salt;
  }

  public static byte[] generateSalt() {
    return generateSalt(DEFAULT_SALT_SIZE);
  }

  public static byte[] toBytes(char[] chars, Charset charset) {
    CharBuffer charBuffer = CharBuffer.wrap(chars);
    ByteBuffer byteBuffer = charset.encode(charBuffer);
    byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
    Arrays.fill(charBuffer.array(), '\u0000');
    Arrays.fill(byteBuffer.array(), (byte) 0);
    return bytes;
  }
}
