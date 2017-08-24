package software.wings.service.intfc.logz;

import software.wings.beans.TaskType;
import software.wings.beans.config.LogzConfig;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.service.impl.elk.ElkLogFetchRequest;

import java.io.IOException;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 8/21/17.
 */
public interface LogzDelegateService {
  @DelegateTaskType(TaskType.LOGZ_CONFIGURATION_VALIDATE_TASK) void validateConfig(@NotNull LogzConfig logzConfig);

  @DelegateTaskType(TaskType.LOGZ_COLLECT_LOG_DATA)
  Object search(@NotNull LogzConfig logzConfig, ElkLogFetchRequest logFetchRequest) throws IOException;

  @DelegateTaskType(TaskType.LOGZ_GET_LOG_SAMPLE) Object getLogSample(LogzConfig logzConfig) throws IOException;
}
