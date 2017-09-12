package software.wings.service.intfc.sumo;

import software.wings.beans.NewRelicConfig;
import software.wings.beans.SumoConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;

import java.io.IOException;
import javax.validation.constraints.NotNull;

/**
 * Created by sriram_parthasarathy on 9/11/17.
 */
public interface SumoDelegateService {
  @DelegateTaskType(TaskType.NEWRELIC_VALIDATE_CONFIGURATION_TASK)
  void validateConfig(@NotNull SumoConfig sumoConfig) throws IOException;
}
