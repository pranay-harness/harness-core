package software.wings.service.impl.security;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.GenerateDataKeyRequest;
import com.amazonaws.services.kms.model.GenerateDataKeyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KmsConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.security.KmsDelegateService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeSet;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by rsingh on 10/2/17.
 */
public class KmsDelegateServiceImpl implements KmsDelegateService {
  public static int NUM_OF_RETRIES = 3;
  private static final Logger logger = LoggerFactory.getLogger(KmsDelegateServiceImpl.class);

  @Override
  public EncryptedData encrypt(char[] value, KmsConfig kmsConfig) throws IOException {
    for (int retry = 1; retry <= NUM_OF_RETRIES; retry++) {
      try {
        final AWSKMS kmsClient = AWSKMSClientBuilder.standard()
                                     .withCredentials(new AWSStaticCredentialsProvider(
                                         new BasicAWSCredentials(kmsConfig.getAccessKey(), kmsConfig.getSecretKey())))
                                     .withRegion(Regions.US_EAST_1)
                                     .build();
        GenerateDataKeyRequest dataKeyRequest = new GenerateDataKeyRequest();
        dataKeyRequest.setKeyId(kmsConfig.getKmsArn());
        dataKeyRequest.setKeySpec("AES_128");
        GenerateDataKeyResult dataKeyResult = kmsClient.generateDataKey(dataKeyRequest);

        ByteBuffer plainTextKey = dataKeyResult.getPlaintext();

        char[] encryptedValue =
            value == null ? null : encrypt(new String(value), new SecretKeySpec(getByteArray(plainTextKey), "AES"));
        String encryptedKeyString = StandardCharsets.ISO_8859_1.decode(dataKeyResult.getCiphertextBlob()).toString();

        return EncryptedData.builder()
            .encryptionKey(encryptedKeyString)
            .encryptedValue(encryptedValue)
            .type(SettingVariableTypes.KMS)
            .kmsId(kmsConfig.getUuid())
            .build();
      } catch (Exception e) {
        if (retry < NUM_OF_RETRIES) {
          logger.warn("Encryption failed. trial num: {}", retry, e);
          try {
            Thread.sleep(100);
          } catch (InterruptedException in) {
            throw new IOException(in);
          }
        } else {
          logger.error("Encryption failed after {} retries ", retry, e);
          throw new IOException("Encryption failed after " + NUM_OF_RETRIES + " retries", e);
        }
      }
    }

    throw new IllegalStateException("Encryption failed. This state should never have been reached");
  }

  @Override
  public char[] decrypt(EncryptedData data, KmsConfig kmsConfig) throws IOException {
    if (data.getEncryptedValue() == null) {
      return null;
    }

    for (int retry = 1; retry <= NUM_OF_RETRIES; retry++) {
      try {
        final AWSKMS kmsClient = AWSKMSClientBuilder.standard()
                                     .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                                         new String(kmsConfig.getAccessKey()), new String(kmsConfig.getSecretKey()))))
                                     .withRegion(Regions.US_EAST_1)
                                     .build();

        DecryptRequest decryptRequest =
            new DecryptRequest().withCiphertextBlob(StandardCharsets.ISO_8859_1.encode(data.getEncryptionKey()));
        ByteBuffer plainTextKey = kmsClient.decrypt(decryptRequest).getPlaintext();

        return decrypt(data.getEncryptedValue(), new SecretKeySpec(getByteArray(plainTextKey), "AES")).toCharArray();
      } catch (Exception e) {
        if (retry < NUM_OF_RETRIES) {
          logger.warn("Decryption failed. trial num: {}", retry, e);
          try {
            Thread.sleep(100);
          } catch (InterruptedException in) {
            throw new IOException(in);
          }
        } else {
          logger.error("Decryption failed after {} retries ", retry, e);
          throw new IOException("Decryption failed after " + NUM_OF_RETRIES + " retries", e);
        }
      }
    }
    throw new IllegalStateException("Decryption failed. This state should never have been reached");
  }

  private char[] encrypt(String src, Key key) throws NoSuchAlgorithmException, NoSuchPaddingException,
                                                     InvalidKeyException, IllegalBlockSizeException,
                                                     BadPaddingException, InvalidAlgorithmParameterException {
    Cipher cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.ENCRYPT_MODE, key);

    byte[] enc = cipher.doFinal(src.getBytes());
    return Base64.getEncoder().encodeToString(enc).toCharArray();
  }

  private String decrypt(char[] src, Key key) throws NoSuchAlgorithmException, NoSuchPaddingException,
                                                     InvalidKeyException, IllegalBlockSizeException,
                                                     BadPaddingException, InvalidAlgorithmParameterException {
    if (src == null) {
      return null;
    }

    byte[] decodeBase64src = Base64.getDecoder().decode(new String(src));
    Cipher cipher = Cipher.getInstance("AES");

    cipher.init(Cipher.DECRYPT_MODE, key);
    return new String(cipher.doFinal(decodeBase64src));
  }

  private byte[] getByteArray(ByteBuffer b) {
    byte[] byteArray = new byte[b.remaining()];
    b.get(byteArray);
    return byteArray;
  }
}
