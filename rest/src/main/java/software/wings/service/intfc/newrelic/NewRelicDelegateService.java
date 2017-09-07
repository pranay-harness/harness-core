package software.wings.service.intfc.newrelic;

import software.wings.beans.NewRelicConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.impl.newrelic.NewRelicApplicationInstance;
import software.wings.service.impl.newrelic.NewRelicMetricData;

import java.io.IOException;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 8/28/17.
 */
public interface NewRelicDelegateService {
  @DelegateTaskType(TaskType.NEWRELIC_VALIDATE_CONFIGURATION_TASK)
  void validateConfig(@NotNull NewRelicConfig newRelicConfig) throws IOException;

  @DelegateTaskType(TaskType.NEWRELIC_GET_APP_TASK)
  List<NewRelicApplication> getAllApplications(@NotNull NewRelicConfig newRelicConfig) throws IOException;

  @DelegateTaskType(TaskType.NEWRELIC_GET_APP_INSTANCES_TASK)
  List<NewRelicApplicationInstance> getApplicationInstances(NewRelicConfig newRelicConfig, long newRelicApplicationId)
      throws IOException;

  @DelegateTaskType(TaskType.NEWRELIC_GET_METRICES_DATA)
  NewRelicMetricData getMetricData(NewRelicConfig newRelicConfig, long newRelicApplicationId, long instanceId,
      String metricName, List<String> valuesToCollect, long fromTime, long toTime) throws IOException;
}
