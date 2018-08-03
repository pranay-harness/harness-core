package software.wings.generator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.codec.binary.Hex;
import software.wings.beans.Account;
import software.wings.exception.WingsException;
import software.wings.generator.OwnerManager.Owners;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.security.SecretManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.util.Properties;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

@Singleton
public class SecretGenerator {
  protected static final String passphrase = System.getenv("HARNESS_GENERATION_PASSPHRASE");

  @Inject SecretManager secretManager;

  private final Properties secrets;

  @Value
  @Builder
  @AllArgsConstructor
  public static class SecretName {
    String value;
  }

  SecretGenerator() {
    secrets = new Properties();
    try (InputStream in = getClass().getResourceAsStream("/secrets.properties")) {
      secrets.load(in);
    } catch (IOException exception) {
      throw new WingsException(exception);
    }
  }

  public boolean isInitialized() {
    return passphrase != null;
  }

  public byte[] decrypt(SecretName name) {
    return decrypt(secrets.getProperty(name.getValue()));
  }

  public byte[] decrypt(String cipheredSecretHex) {
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

  public String decryptToString(SecretName name) {
    return decryptToString(secrets.getProperty(name.getValue()));
  }

  public String decryptToString(String cipheredSecretHex) {
    try {
      return new String(decrypt(cipheredSecretHex), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new WingsException(e);
    }
  }

  public char[] decryptToCharArray(SecretName name) {
    return decryptToCharArray(secrets.getProperty(name.getValue()));
  }

  public char[] decryptToCharArray(String cipheredSecretHex) {
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

  String ensureStored(String accountId, SecretName name) {
    final EncryptedData encryptedData = secretManager.getEncryptedDataByName(accountId, name.getValue());
    if (encryptedData != null) {
      return encryptedData.getUuid();
    }

    return secretManager.saveSecret(accountId, name.getValue(), decryptToString(name), null);
  }

  public String ensureStored(Owners owners, SecretName name) {
    final Account account = owners.obtainAccount();
    return ensureStored(account.getUuid(), name);
  }
}
