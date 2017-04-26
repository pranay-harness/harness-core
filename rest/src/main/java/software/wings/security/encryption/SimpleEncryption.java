package software.wings.security.encryption;

import org.mongodb.morphia.annotations.Transient;
import software.wings.exception.WingsException;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Very simple hardcoded encryption package for encrypting user passwords in persistence.
 * Created by mike@ on 4/24/17.
 */
public class SimpleEncryption implements EncryptionInterface {
  // IV and KEY both need to be 16 characters long.
  @Transient private static final byte[] IV = "EncryptionIV0*d&".getBytes(StandardCharsets.ISO_8859_1);
  @Transient private static final char[] DEFAULT_KEY = "EncryptionKey2a@".toCharArray();
  @Transient private SecretKeyFactory FACTORY;

  private static final EncryptionType encryptionType = EncryptionType.SIMPLE;
  @Transient private char[] key;
  private byte[] salt;
  @Transient private SecretKey secretKey;

  public SimpleEncryption() {
    this(DEFAULT_KEY, EncryptionUtils.generateSalt());
  }

  public SimpleEncryption(char[] key) {
    this(key, EncryptionUtils.generateSalt());
  }

  public SimpleEncryption(String keySource) {
    this(keySource.toCharArray(), EncryptionUtils.generateSalt());
    //    this(Charsets.ISO_8859_1.decode(ByteBuffer.wrap(Hashing.sha256().hashString(keySource,
    //    Charsets.ISO_8859_1).asBytes())).array(), EncryptionUtils.generateSalt());
  }

  public SimpleEncryption(String keySource, byte[] salt) {
    this(keySource.toCharArray(), salt);
    //    this(Charsets.ISO_8859_1.decode(ByteBuffer.wrap(Hashing.sha256().hashString(keySource,
    //    Charsets.ISO_8859_1).asBytes())).array(), salt);
  }

  public SimpleEncryption(char[] key, byte[] salt) {
    if (key.length == 32) {
      key = Arrays.copyOf(key, 16);
    }
    if (key.length != 16) {
      throw new WingsException("Key must be 16 characters. Key is " + key.length);
    }
    this.key = key;
    this.salt = salt;
    try {
      FACTORY = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
      KeySpec spec = new PBEKeySpec(key, salt, 65536, 128);
      SecretKey tmp = FACTORY.generateSecret(spec);
      secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      System.out.println(e.getMessage());
      System.out.println(key.toString());
    }
  }

  public EncryptionType getEncryptionType() {
    return this.encryptionType;
  }

  public SecretKey getSecretKey() {
    return this.secretKey;
  }

  public byte[] getSalt() {
    return this.salt;
  }

  public void setSalt(byte[] salt) {
    this.salt = salt;
  }

  public void setSalt() {
    this.salt = EncryptionUtils.generateSalt();
  }

  public byte[] encrypt(byte[] content) {
    try {
      Cipher c = Cipher.getInstance("AES/CBC/PKCS5PADDING");
      c.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(IV));
      return c.doFinal(content);
    } catch (InvalidKeyException e) {
      throw new WingsException("Key must be 16 characters.");
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException
        | IllegalBlockSizeException | BadPaddingException e) {
    }
    return null;
  }

  public byte[] decrypt(byte[] encrypted) {
    try {
      Cipher c = Cipher.getInstance("AES/CBC/PKCS5PADDING");
      c.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(IV));
      return c.doFinal(encrypted);
    } catch (InvalidKeyException e) {
      throw new WingsException("Key must be 16 characters.");
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException
        | IllegalBlockSizeException | BadPaddingException e) {
    }
    return null;
  }
}
