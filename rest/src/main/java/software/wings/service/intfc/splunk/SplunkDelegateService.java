package software.wings.service.intfc.splunk;

import software.wings.beans.SplunkConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 4/17/17.
 */
public interface SplunkDelegateService {
  @DelegateTaskType(TaskType.SPLUNK_CONFIGURATION_VALIDATE_TASK)
  boolean validateConfig(@NotNull SplunkConfig splunkConfig, List<EncryptedDataDetail> encryptedDataDetails);
}
