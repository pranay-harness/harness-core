package software.wings.beans;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by anubhaw on 10/26/16.
 */

@Getter
@Setter
@Builder
public class PipelineStageExecution {
  private String stateUuid;
  private String stateName;
  private String stateType;
  private ExecutionStatus status;
  private Long startTs;
  private Long endTs;
  private Long estimatedTime;
  @Builder.Default private List<WorkflowExecution> workflowExecutions = new ArrayList<>();
  private StateExecutionData stateExecutionData;
  private String message;

  public List<WorkflowExecution> getWorkflowExecutions() {
    return Objects.isNull(workflowExecutions) ? new ArrayList<>() : workflowExecutions;
  }
}
