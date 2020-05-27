package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.data.structure.EmptyPredicate;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import software.wings.beans.NameValuePair;
import software.wings.beans.WorkflowExecution;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
public class WorkflowExecutionMetadata implements ExecutionMetadata {
  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore String id;
  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore String appId;

  String executionType;
  String application;
  @JsonProperty("workflow") String entityName;
  EnvMetadata environment;
  List<ServiceInfraSummaryMetadata> serviceInfrastructures;
  List<ArtifactMetadata> inputArtifacts;
  List<ArtifactMetadata> collectedArtifacts;
  ExecutionStatus status;

  List<GraphNodeMetadata> executionGraph;

  boolean onDemandRollback;

  List<NameValuePair> tags;
  TimingMetadata timing;
  TriggeredByMetadata triggeredBy;

  public void accept(GraphNodeVisitor visitor) {
    MetadataUtils.acceptMultiple(visitor, executionGraph);
  }

  static List<WorkflowExecutionMetadata> fromWorkflowExecutions(
      List<WorkflowExecution> workflowExecutions, boolean infraRefactor) {
    return MetadataUtils.map(
        workflowExecutions, workflowExecution -> fromWorkflowExecution(workflowExecution, infraRefactor, false));
  }

  public static WorkflowExecutionMetadata fromWorkflowExecution(
      WorkflowExecution workflowExecution, boolean infraRefactor) {
    return fromWorkflowExecution(workflowExecution, infraRefactor, true);
  }

  private static WorkflowExecutionMetadata fromWorkflowExecution(
      WorkflowExecution workflowExecution, boolean infraRefactor, boolean withTriggeredBy) {
    if (workflowExecution == null || workflowExecution.getWorkflowType() != WorkflowType.ORCHESTRATION) {
      return null;
    }

    return WorkflowExecutionMetadata.builder()
        .id(workflowExecution.getUuid())
        .appId(workflowExecution.getAppId())
        .executionType("Workflow")
        .application(workflowExecution.getAppName())
        .entityName(workflowExecution.getName())
        .environment(EnvMetadata.fromFirstEnvSummary(workflowExecution.getEnvironments()))
        .serviceInfrastructures(ServiceInfraSummaryMetadata.fromElementExecutionSummaries(
            workflowExecution.getServiceExecutionSummaries(), infraRefactor))
        .inputArtifacts(ArtifactMetadata.fromArtifacts(
            EmptyPredicate.isEmpty(workflowExecution.getArtifacts()) && workflowExecution.getExecutionArgs() != null
                ? workflowExecution.getExecutionArgs().getArtifacts()
                : workflowExecution.getArtifacts()))
        .collectedArtifacts(
            ArtifactMetadata.fromBuildExecutionSummaries(workflowExecution.getBuildExecutionSummaries()))
        .status(workflowExecution.getStatus())
        .executionGraph(GraphNodeMetadata.fromOriginGraphNode(workflowExecution.getExecutionNode()))
        .onDemandRollback(workflowExecution.isOnDemandRollback())
        .tags(workflowExecution.getTags())
        .timing(TimingMetadata.fromStartAndEndTimeObjects(workflowExecution.getStartTs(), workflowExecution.getEndTs()))
        .triggeredBy(withTriggeredBy ? TriggeredByMetadata.fromWorkflowExecution(workflowExecution) : null)
        .build();
  }
}
