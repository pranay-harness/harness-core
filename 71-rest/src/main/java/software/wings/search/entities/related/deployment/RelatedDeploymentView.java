package software.wings.search.entities.related.deployment;

import io.harness.beans.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.beans.WorkflowExecution;

@Value
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "DeploymentRelatedEntityViewKeys")
public class RelatedDeploymentView {
  private String id;
  private ExecutionStatus status;
  private String name;
  private long createdAt;
  private String pipelineExecutionId;
  private String workflowId;
  private String workflowType;
  private String envId;

  public RelatedDeploymentView(WorkflowExecution workflowExecution) {
    this.id = workflowExecution.getUuid();
    this.status = workflowExecution.getStatus();
    this.name = workflowExecution.getName();
    this.createdAt = workflowExecution.getCreatedAt();
    this.pipelineExecutionId = workflowExecution.getPipelineExecutionId();
    this.workflowType = workflowExecution.getWorkflowType().name();
    this.envId = workflowExecution.getEnvId();
    this.workflowId = workflowExecution.getWorkflowId();
  }
}
