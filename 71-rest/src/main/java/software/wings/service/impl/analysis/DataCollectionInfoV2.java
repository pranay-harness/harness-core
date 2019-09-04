package software.wings.service.impl.analysis;

import io.harness.delegate.task.TaskParameters;
import lombok.AllArgsConstructor;
import lombok.Data;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.cv.DataCollector;
import software.wings.sm.StateType;

import java.time.Instant;
import java.util.Set;

@Data
@AllArgsConstructor
public abstract class DataCollectionInfoV2 implements TaskParameters {
  private final String accountId;
  private final String applicationId;
  private final String envId;
  private Instant startTime;
  private Instant endTime;
  private final Set<String> hosts;
  private final String cvConfigId;
  private final String stateExecutionId;
  private final String workflowId;
  private final String workflowExecutionId;
  private final String serviceId;

  public abstract TaskType getTaskType();

  public abstract StateType getStateType();

  public abstract Class<? extends DataCollector<? extends DataCollectionInfoV2>> getDataCollectorImplClass();
}
