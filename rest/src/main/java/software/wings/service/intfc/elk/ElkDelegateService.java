package software.wings.service.intfc.elk;

import software.wings.beans.ElkConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.service.impl.elk.ElkIndexTemplate;
import software.wings.service.impl.elk.ElkLogFetchRequest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 08/01/17.
 */
public interface ElkDelegateService {
  @DelegateTaskType(TaskType.ELK_CONFIGURATION_VALIDATE_TASK) void validateConfig(@NotNull ElkConfig splunkConfig);

  @DelegateTaskType(TaskType.ELK_COLLECT_LOG_DATA)
  Object search(@NotNull ElkConfig elkConfig, ElkLogFetchRequest logFetchRequest) throws IOException;

  @DelegateTaskType(TaskType.ELK_COLLECT_INDICES)
  Map<String, ElkIndexTemplate> getIndices(ElkConfig elkConfig) throws IOException;

  @DelegateTaskType(TaskType.ELK_GET_LOG_SAMPLE)
  Object getLogSample(ElkConfig elkConfig, String index) throws IOException;
}
