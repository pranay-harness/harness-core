package software.wings.service.intfc.security;

import software.wings.beans.KmsConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.security.encryption.EncryptedData;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by rsingh on 10/2/17.
 */
public interface KmsDelegateService {
  @DelegateTaskType(TaskType.KMS_ENCRYPT)
  EncryptedData encrypt(char[] value, KmsConfig kmsConfig)
      throws NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException,
             IllegalBlockSizeException, BadPaddingException, InvalidKeyException;

  @DelegateTaskType(TaskType.KMS_DECRYPT)
  char[] decrypt(EncryptedData data, KmsConfig kmsConfig)
      throws NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException,
             IllegalBlockSizeException, BadPaddingException, InvalidKeyException;
}
