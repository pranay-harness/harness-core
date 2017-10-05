package software.wings.service.intfc.kms;

import software.wings.beans.KmsConfig;
import software.wings.security.encryption.EncryptedData;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by rsingh on 9/29/17.
 */
public interface KmsService {
  EncryptedData encrypt(char[] value, KmsConfig kmsConfig);

  char[] decrypt(EncryptedData data, KmsConfig kmsConfig);

  KmsConfig getKmsConfig(String accountId);

  boolean saveKmsConfig(String accountId, String name, KmsConfig kmsConfig);
}
