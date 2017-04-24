package software.wings.security.encryption;

import software.wings.exception.WingsException;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
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
  private final byte[] IV = "EncryptionIV0*d&".getBytes(StandardCharsets.UTF_8);
  private final char[] KEY = "EncryptionKey2a@".toCharArray();
  private final byte[] SALT = "megasalt".getBytes(StandardCharsets.UTF_8);
  private SecretKeyFactory FACTORY;

  public SimpleEncryption() {
    try {
      FACTORY = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
    } catch (NoSuchAlgorithmException nsae) {
    }
  }

  private SecretKey generateKey(char[] key) throws InvalidKeySpecException {
    try {
      KeySpec spec = new PBEKeySpec(key, SALT, 65536, 128);
      SecretKey tmp = FACTORY.generateSecret(spec);
      return new SecretKeySpec(tmp.getEncoded(), "AES");
    } catch (InvalidKeySpecException ikse) {
    }
    return null;
  }

  public byte[] encrypt(byte[] content, char[] passphrase) {
    try {
      Cipher c = Cipher.getInstance("AES/CBC/PKCS5PADDING");
      SecretKey secretKey = this.generateKey(passphrase);
      c.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(IV));
      return c.doFinal(content);
    } catch (InvalidKeyException e) {
      throw new WingsException("Key must be 16 characters.");
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeySpecException
        | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
    }
    return null;
  }

  public byte[] encrypt(byte[] content) {
    return encrypt(content, KEY);
  }

  public byte[] decrypt(byte[] encrypted, char[] passphrase) {
    try {
      Cipher c = Cipher.getInstance("AES/CBC/PKCS5PADDING");
      SecretKey secretKey = this.generateKey(passphrase);
      c.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(IV));
      return c.doFinal(encrypted);
    } catch (InvalidKeyException e) {
      throw new WingsException("Key must be 16 characters.");
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeySpecException
        | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
    }
    return null;
  }

  public byte[] decrypt(byte[] encrypted) {
    return decrypt(encrypted, KEY);
  }
}
