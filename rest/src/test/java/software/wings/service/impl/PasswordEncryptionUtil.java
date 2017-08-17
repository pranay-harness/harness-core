package software.wings.service.impl;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.annotations.Transient;
import software.wings.WingsBaseTest;
import software.wings.beans.ConfigFile;
import software.wings.beans.ErrorCode;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.rules.Integration;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.security.encryption.EncryptionInterface;
import software.wings.security.encryption.EncryptionType;
import software.wings.security.encryption.EncryptionUtils;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.settings.SettingValue;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.List;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;

/**
 * Created by mike@ on 5/4/17.
 */
@Integration
@Ignore
public class PasswordEncryptionUtil extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;

  @Test
  @Ignore
  public void encryptUnencryptedPasswordsInSettingAttributes() {
    List<SettingAttribute> settings = wingsPersistence.list(SettingAttribute.class);
    settings.forEach(setting -> {
      //      System.out.println("setting: " + setting.toString());
      if (setting.getValue() instanceof Encryptable) {
        boolean passwordNeedsFixing = false;
        boolean accountIdFound = false;
        char[] outputChars = "x".toCharArray();
        for (Field passwordField : setting.getValue().getClass().getFields()) {
          System.out.println(passwordField.getName());
          if (passwordField.getName() == "accountId") {
            accountIdFound = true;
          }
          Encrypted a = passwordField.getAnnotation(Encrypted.class);
          if (a != null && a.value()) {
            //            System.out.println("Encryptable found: " + passwordField.getName() + " on " +
            //            setting.toString());
            try {
              passwordField.setAccessible(true);
              if (null != passwordField) {
                try {
                  SimpleEncryption encryption = new SimpleEncryption(setting.getAccountId());
                  outputChars = encryption.decryptChars((char[]) passwordField.get(setting.getValue()));
                  passwordField.set(setting.getValue(), outputChars);
                } catch (WingsException we) {
                  if (we.getCause() instanceof BadPaddingException) {
                    // try it with AES 128
                    try {
                      SimpleEncryptionAES128 e128 = new SimpleEncryptionAES128(setting.getAccountId());
                      outputChars = e128.decryptChars((char[]) passwordField.get(setting.getValue()));
                      passwordField.set(setting.getValue(), outputChars);
                      passwordNeedsFixing = true;
                    } catch (Exception e) {
                      System.out.println("Fixing failed: " + setting.getUuid());
                    }
                  }
                } catch (Exception e) {
                  passwordNeedsFixing = true;
                }
              } else {
                System.out.println("No password for this Encryptable, can't encrypt it, but will add account ID: "
                    + setting.toString());
                passwordNeedsFixing = true;
              }
            } catch (Exception e) {
              passwordNeedsFixing = true;
            }
          }
        }
        if (passwordNeedsFixing || !accountIdFound) {
          Encryptable e = (Encryptable) setting.getValue();
          e.setAccountId(setting.getAccountId());
          setting.setValue((SettingValue) e);
          System.out.println(setting.toString());
          SettingAttribute fixed = wingsPersistence.saveAndGet(SettingAttribute.class, setting);
          System.out.println(fixed.toString());
        }
      }
    });
  }

  @Test
  @Ignore
  public void encryptUnencryptedConfigValuesInServiceVariables() {
    List<ServiceVariable> variables = wingsPersistence.list(ServiceVariable.class);
    variables.forEach(setting -> {
      boolean variableNeedsFixing = false;
      char[] outputChars = "x".toCharArray();
      try {
        Field passwordField = setting.getClass().getDeclaredField("value");
        passwordField.setAccessible(true);
        if (null != passwordField) {
          try {
            SimpleEncryption encryption = new SimpleEncryption(setting.getAccountId());
            outputChars = encryption.decryptChars((char[]) passwordField.get(setting));
          } catch (WingsException we) {
            if (we.getCause() instanceof BadPaddingException) {
              // try it with AES 128
              try {
                SimpleEncryptionAES128 e128 = new SimpleEncryptionAES128(setting.getAccountId());
                outputChars = e128.decryptChars((char[]) passwordField.get(setting));
                variableNeedsFixing = true;
              } catch (IllegalAccessException iae) {
              }
            }
          } catch (Exception e) {
            variableNeedsFixing = true;
          }
        }
      } catch (NoSuchFieldException nsfe) {
        System.out.println("No password for this Encryptable, can't encrypt it: " + setting.getValue().toString());
      }
      System.out.println(new String(outputChars));
      if (variableNeedsFixing) {
        ServiceVariable unencryptedSetting =
            wingsPersistence.getWithoutDecryptingTestOnly(ServiceVariable.class, setting.getUuid());
        unencryptedSetting.setValue(outputChars);
        ServiceVariable fixed = wingsPersistence.saveAndGet(ServiceVariable.class, unencryptedSetting);
        System.out.println(fixed.getValue());
      }
    });
  }

  @Test
  @Ignore
  public void encryptUnencryptedConfigFiles() {
    List<ConfigFile> configFiles = wingsPersistence.list(ConfigFile.class);
    configFiles.forEach(file -> {
      if (file.isEncrypted()) {
      }
    });
  }

  @Test
  @Ignore
  public void checkAllSettingAttributeEncryptions() {
    List<SettingAttribute> settingAttributes = wingsPersistence.list(SettingAttribute.class);
    settingAttributes.forEach(setting -> {
      SettingAttribute encryptedSetting =
          wingsPersistence.getWithoutDecryptingTestOnly(SettingAttribute.class, setting.getUuid());
      System.out.println(encryptedSetting);
      SettingAttribute unencryptedSetting = wingsPersistence.get(SettingAttribute.class, setting.getUuid());
      System.out.println(unencryptedSetting);

      //      try {
      //        Field passwordField = setting.getValue().getClass().getDeclaredField("password");
      //        System.out.println("sett")
      //      System.out.println()
    });
  }

  public static class SimpleEncryptionAES128 implements EncryptionInterface {
    private final int AES_128_KEY_LENGTH = 16;

    // IV and KEY both need to be AES_128_KEY_LENGTH characters long.
    private final byte[] IV = "EncryptionIV0*d&".getBytes(Charsets.ISO_8859_1);
    private final char[] DEFAULT_KEY = "EncryptionKey2a@".toCharArray();
    private SecretKeyFactory FACTORY;

    private final EncryptionType encryptionType = EncryptionType.SIMPLE;
    private char[] key;
    private byte[] salt;
    private SecretKey secretKey;

    public SimpleEncryptionAES128(String keySource) {
      this(BaseEncoding.base64()
               .encode(Hashing.sha256().hashString(keySource, Charsets.ISO_8859_1).asBytes())
               .toCharArray(),
          EncryptionUtils.generateSalt());
    }

    public SimpleEncryptionAES128(char[] key, byte[] salt) {
      if (key.length > AES_128_KEY_LENGTH) {
        key = Arrays.copyOf(key, AES_128_KEY_LENGTH);
      }
      if (key.length != AES_128_KEY_LENGTH) {
        throw new WingsException("Key must be " + AES_128_KEY_LENGTH + " characters. Key is " + key.length);
      }
      this.key = key;
      this.salt = salt;
      this.secretKey = generateSecretKey(key, salt);
    }

    public EncryptionType getEncryptionType() {
      return this.encryptionType;
    }

    @JsonIgnore
    public SecretKey getSecretKey() {
      return this.secretKey;
    }

    public byte[] getSalt() {
      return this.salt;
    }

    public void setSalt(byte[] salt) {
      this.salt = salt;
    }

    public byte[] encrypt(byte[] content) {
      try {
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        c.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(IV));
        byte[] encrypted = c.doFinal(content);
        byte[] combined = new byte[salt.length + encrypted.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(encrypted, 0, combined, salt.length, encrypted.length);
        return combined;
      } catch (InvalidKeyException e) {
        // Key must be AES_256_KEY_LENGTH ASCII characters. If the JCE Unlimited Strength jars aren't installed, this
        // won't work.
        throw new WingsException(ErrorCode.ENCRYPTION_NOT_CONFIGURED, "Encryption failed.", e);
      } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException
          | IllegalBlockSizeException | BadPaddingException e) {
        throw new WingsException(ErrorCode.ENCRYPTION_NOT_CONFIGURED, "Encryption failed: ", e);
      }
    }

    public char[] encryptChars(char[] content) {
      byte[] encrypted = this.encrypt(Charsets.ISO_8859_1.encode(CharBuffer.wrap(content)).array());
      return Charsets.ISO_8859_1.decode(ByteBuffer.wrap(encrypted)).array();
    }

    public byte[] decrypt(byte[] encrypted) {
      try {
        byte[] newSalt = new byte[EncryptionUtils.DEFAULT_SALT_SIZE];
        byte[] inputBytes = new byte[encrypted.length - EncryptionUtils.DEFAULT_SALT_SIZE];
        System.arraycopy(encrypted, 0, newSalt, 0, newSalt.length);
        System.arraycopy(encrypted, newSalt.length, inputBytes, 0, inputBytes.length);
        this.secretKey = generateSecretKey(key, newSalt);
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        c.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(IV));
        return c.doFinal(inputBytes);
      } catch (InvalidKeyException e) {
        // Key must be AES_128_KEY_LENGTH ASCII characters. If the JCE Unlimited Strength jars aren't installed, this
        // won't work.

        throw new WingsException(ErrorCode.ENCRYPTION_NOT_CONFIGURED, "Decryption failed.", e);
      } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException
          | IllegalBlockSizeException | BadPaddingException e) {
        throw new WingsException(ErrorCode.ENCRYPTION_NOT_CONFIGURED, "Decryption failed: ", e);
      }
    }

    public char[] decryptChars(char[] encrypted) {
      byte[] decrypted = this.decrypt(Charsets.ISO_8859_1.encode(CharBuffer.wrap(encrypted)).array());
      return Charsets.ISO_8859_1.decode(ByteBuffer.wrap(decrypted)).array();
    }

    private SecretKey generateSecretKey(char[] key, byte[] salt) {
      try {
        FACTORY = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
        KeySpec spec = new PBEKeySpec(key, salt, 65536, 128);
        SecretKey tmp = FACTORY.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
      } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
        throw new WingsException("Encryption secret key generation failed: ", e);
      }
    }
  }
}
