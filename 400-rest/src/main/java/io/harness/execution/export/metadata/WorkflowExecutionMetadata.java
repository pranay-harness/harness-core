/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.data.structure.EmptyPredicate;

import software.wings.beans.NameValuePair;
import software.wings.beans.WorkflowExecution;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

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

  static List<WorkflowExecutionMetadata> fromWorkflowExecutions(List<WorkflowExecution> workflowExecutions) {
    return MetadataUtils.map(workflowExecutions, workflowExecution -> fromWorkflowExecution(workflowExecution, false));
  }

  public static WorkflowExecutionMetadata fromWorkflowExecution(WorkflowExecution workflowExecution) {
    return fromWorkflowExecution(workflowExecution, true);
  }

  private static WorkflowExecutionMetadata fromWorkflowExecution(
      WorkflowExecution workflowExecution, boolean withTriggeredBy) {
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
        .serviceInfrastructures(
            ServiceInfraSummaryMetadata.fromElementExecutionSummaries(workflowExecution.getServiceExecutionSummaries()))
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
