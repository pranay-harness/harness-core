package software.wings.service.intfc.splunk;

import software.wings.beans.SplunkConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;

import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 4/17/17.
 */
public interface SplunkDelegateService {
  @DelegateTaskType(TaskType.SPLUNK_CONFIGURATION_VALIDATE_TASK)
  void validateConfig(@NotNull SplunkConfig splunkConfig);
}
