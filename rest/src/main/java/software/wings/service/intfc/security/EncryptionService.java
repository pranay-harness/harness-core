package software.wings.service.intfc.security;

import software.wings.annotation.Encryptable;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.security.encryption.EncryptedDataDetail;

import java.io.IOException;
import java.util.List;

/**
 * Created by rsingh on 10/18/17.
 */
public interface EncryptionService {
  @DelegateTaskType(TaskType.SECRET_DECRYPT)
  Encryptable decrypt(Encryptable object, List<EncryptedDataDetail> encryptedDataDetails);

  @DelegateTaskType(TaskType.SECRET_DECRYPT_REF)
  char[] getDecryptedValue(EncryptedDataDetail encryptedDataDetail) throws IOException;
}
