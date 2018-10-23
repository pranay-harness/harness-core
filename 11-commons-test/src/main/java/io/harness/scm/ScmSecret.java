package io.harness.scm;

import lombok.Getter;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Properties;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class ScmSecret {
  public static final String passphrase = System.getenv("HARNESS_GENERATION_PASSPHRASE");

  @Getter private final Properties secrets;

  ScmSecret() {
    secrets = new Properties();
    try (InputStream in = getClass().getResourceAsStream("/secrets.properties")) {
      secrets.load(in);
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  public boolean isInitialized() {
    return passphrase != null;
  }

  public byte[] decrypt(SecretName name) {
    return decrypt(secrets.getProperty(name.getValue()));
  }

  public byte[] decrypt(String cipheredSecretHex) {
    try {
      if (!isInitialized()) {
        return "You can't decrypt in this environment. You need to set HARNESS_GENERATION_PASSPHRASE in your environment!"
            .getBytes(StandardCharsets.UTF_8);
      }
      Key aesKey = new SecretKeySpec(Hex.decodeHex(passphrase.toCharArray()), "AES");
      Cipher cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.DECRYPT_MODE, aesKey);
      return cipher.doFinal(Hex.decodeHex(cipheredSecretHex.toCharArray()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String decryptToString(SecretName name) {
    return decryptToString(secrets.getProperty(name.getValue()));
  }

  public String decryptToString(String cipheredSecretHex) {
    return new String(decrypt(cipheredSecretHex), StandardCharsets.UTF_8);
  }

  public char[] decryptToCharArray(SecretName name) {
    return decryptToCharArray(secrets.getProperty(name.getValue()));
  }

  public char[] decryptToCharArray(String cipheredSecretHex) {
    return decryptToString(cipheredSecretHex).toCharArray();
  }

  public static String encrypt(byte[] secret, String passphrase) {
    try {
      Key aesKey = new SecretKeySpec(Hex.decodeHex(passphrase.toCharArray()), "AES");
      Cipher cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.ENCRYPT_MODE, aesKey);
      return Hex.encodeHexString(cipher.doFinal(secret));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String encrypt(byte[] secret) {
    return encrypt(secret, passphrase);
  }
}
