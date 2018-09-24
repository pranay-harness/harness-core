package software.wings.utils;

import software.wings.beans.ExecutionCredential;
import software.wings.beans.HostValidationResponse;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

public interface HostValidationService {
  @DelegateTaskType(TaskType.HOST_VALIDATION)
  List<HostValidationResponse> validateHost(List<String> hostNames, SettingAttribute connectionSetting,
      List<EncryptedDataDetail> encryptionDetails, ExecutionCredential executionCredential);
}
