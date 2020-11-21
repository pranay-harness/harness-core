package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;

import software.wings.sm.StateExecutionData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by anubhaw on 10/26/16.
 */

@OwnedBy(CDC)
@Getter
@Setter
@Builder
public class PipelineStageExecution {
  private String pipelineStageElementId;
  private String stateUuid;
  private String stateName;
  private String stateType;
  private ExecutionStatus status;
  private Long startTs;
  private Long expiryTs;
  private Long endTs;
  private Long estimatedTime;
  @Builder.Default private List<WorkflowExecution> workflowExecutions = new ArrayList<>();
  private StateExecutionData stateExecutionData;
  private String message;
  private boolean looped;
  private boolean waitingForInputs;
  private ParallelInfo parallelInfo;
  private EmbeddedUser triggeredBy;

  public List<WorkflowExecution> getWorkflowExecutions() {
    return Objects.isNull(workflowExecutions) ? new ArrayList<>() : workflowExecutions;
  }
}
