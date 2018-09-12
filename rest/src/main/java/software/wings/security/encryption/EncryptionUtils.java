package software.wings.security.encryption;

import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static software.wings.security.encryption.SimpleEncryption.CHARSET;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import io.harness.exception.WingsException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

  public static char[] encrypt(InputStream content, String containerId) {
    try {
      SimpleEncryption encryption = new SimpleEncryption(containerId);
      byte[] encryptedBytes = encryption.encrypt(ByteStreams.toByteArray(content));
      return CHARSET.decode(ByteBuffer.wrap(encryptedBytes)).array();
    } catch (IOException ioe) {
      throw new WingsException(DEFAULT_ERROR_CODE, ioe);
    }
  }

  public static File decrypt(File file, String containerId) {
    try {
      SimpleEncryption encryption = new SimpleEncryption(containerId);
      byte[] outputBytes = encryption.decrypt(Files.toByteArray(file));
      Files.write(outputBytes, file);
      return file;
    } catch (IOException ioe) {
      throw new WingsException(DEFAULT_ERROR_CODE, ioe);
    }
  }

  public static void decryptToStream(File file, String containerId, OutputStream output) {
    try {
      SimpleEncryption encryption = new SimpleEncryption(containerId);
      byte[] outputBytes = encryption.decrypt(Files.toByteArray(file));
      output.write(outputBytes, 0, outputBytes.length);
      output.flush();
    } catch (IOException ioe) {
      throw new WingsException(DEFAULT_ERROR_CODE, ioe);
    }
  }
}
