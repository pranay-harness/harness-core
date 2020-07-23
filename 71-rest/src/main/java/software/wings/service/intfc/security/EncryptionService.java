package software.wings.service.intfc.security;

import io.harness.security.encryption.EncryptableSettingWithEncryptionDetails;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;

import java.io.IOException;
import java.util.List;

/**
 * Created by rsingh on 10/18/17.
 */
public interface EncryptionService extends SecretDecryptionService {
  @DelegateTaskType(TaskType.SECRET_DECRYPT)
  EncryptableSetting decrypt(EncryptableSetting object, List<EncryptedDataDetail> encryptedDataDetails);

  @DelegateTaskType(TaskType.BATCH_SECRET_DECRYPT)
  List<EncryptableSettingWithEncryptionDetails> decrypt(
      List<EncryptableSettingWithEncryptionDetails> encryptableSettingWithEncryptionDetailsList);

  @DelegateTaskType(TaskType.SECRET_DECRYPT_REF)
  char[] getDecryptedValue(EncryptedDataDetail encryptedDataDetail) throws IOException;
}
