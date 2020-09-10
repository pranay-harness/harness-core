package io.harness.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.encoding.EncodingUtils.decodeBase64;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.io.Files;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.EncryptDecryptException;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
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
@OwnedBy(PL)
@UtilityClass
public class EncryptionUtils {
  public static final int DEFAULT_SALT_SIZE = 32;

  /**
   * Generate a salt to use for encryption.
   *
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

  public static byte[] encrypt(byte[] content, String containerId) {
    try {
      SimpleEncryption encryption =
          isNotEmpty(containerId) ? new SimpleEncryption(containerId) : new SimpleEncryption();
      return encryption.encrypt(content);
    } catch (Exception ioe) {
      throw new EncryptDecryptException("Failed to encrypt content", ioe);
    }
  }

  public static byte[] decrypt(byte[] encryptedText, String containerId) {
    try {
      SimpleEncryption encryption =
          isNotEmpty(containerId) ? new SimpleEncryption(containerId) : new SimpleEncryption();
      return encryption.decrypt(encryptedText);
    } catch (Exception ioe) {
      throw new EncryptDecryptException("Failed to decrypt encrypted text", ioe);
    }
  }

  public static File decrypt(File file, String containerId, boolean base64Encoded) {
    try {
      SimpleEncryption encryption = new SimpleEncryption(containerId);
      byte[] outputBytes = encryption.decrypt(Files.toByteArray(file));
      byte[] fileData = base64Encoded ? decodeBase64(outputBytes) : outputBytes;
      Files.write(fileData, file);
      return file;
    } catch (IOException ioe) {
      throw new EncryptDecryptException("Failed to decrypt file", ioe);
    }
  }

  public static void decryptToStream(File file, String containerId, OutputStream output, boolean base64Encoded) {
    try {
      SimpleEncryption encryption = new SimpleEncryption(containerId);
      byte[] outputBytes = encryption.decrypt(Files.toByteArray(file));
      byte[] fileData = base64Encoded ? decodeBase64(outputBytes) : outputBytes;
      output.write(fileData, 0, fileData.length);
      output.flush();
    } catch (IOException ioe) {
      throw new EncryptDecryptException("Failed to decrypt file to stream", ioe);
    }
  }
}
