package software.wings.generator;

import com.google.inject.Singleton;

import org.apache.commons.codec.binary.Hex;
import software.wings.exception.WingsException;

import java.io.UnsupportedEncodingException;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

@Singleton
public class SecretGenerator {
  protected static final String passphrase = System.getenv("HARNESS_GENERATION_PASSPHRASE");

  boolean isInitialized() {
    return passphrase != null;
  }

  byte[] decrypt(String cipheredSecretHex) {
    if (!isInitialized()) {
      return "You can't decrypt in this environment".getBytes();
    }

    try {
      Key aesKey = new SecretKeySpec(Hex.decodeHex(passphrase.toCharArray()), "AES");
      Cipher cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.DECRYPT_MODE, aesKey);
      return cipher.doFinal(Hex.decodeHex(cipheredSecretHex.toCharArray()));
    } catch (Exception e) {
      throw new WingsException(e);
    }
  }

  String decryptToString(String cipheredSecretHex) {
    try {
      return new String(decrypt(cipheredSecretHex), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new WingsException(e);
    }
  }

  char[] decryptToCharArray(String cipheredSecretHex) {
    return decryptToString(cipheredSecretHex).toCharArray();
  }

  String encrypt(byte[] secret) {
    try {
      Key aesKey = new SecretKeySpec(Hex.decodeHex(passphrase.toCharArray()), "AES");
      Cipher cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.ENCRYPT_MODE, aesKey);
      return Hex.encodeHexString(cipher.doFinal(secret));
    } catch (Exception e) {
      throw new WingsException(e);
    }
  }
}
