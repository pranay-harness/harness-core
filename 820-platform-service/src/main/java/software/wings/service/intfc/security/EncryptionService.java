package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.encryption.EncryptableSettingWithEncryptionDetails;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.annotation.EncryptableSetting;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;

import java.util.List;

/**
 * Created by rsingh on 10/18/17.
 */
@OwnedBy(PL)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public interface EncryptionService {
  @DelegateTaskType(TaskType.SECRET_DECRYPT)
  EncryptableSetting decrypt(
      EncryptableSetting object, List<EncryptedDataDetail> encryptedDataDetails, boolean fromCache);

  @DelegateTaskType(TaskType.BATCH_SECRET_DECRYPT)
  List<EncryptableSettingWithEncryptionDetails> decrypt(
      List<EncryptableSettingWithEncryptionDetails> encryptableSettingWithEncryptionDetailsList, boolean fromCache);

  @DelegateTaskType(TaskType.SECRET_DECRYPT_REF)
  char[] getDecryptedValue(EncryptedDataDetail encryptedDataDetail, boolean fromCache);
}
