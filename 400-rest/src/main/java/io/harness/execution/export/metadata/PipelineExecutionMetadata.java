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
public class PipelineExecutionMetadata implements ExecutionMetadata {
  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore String id;
  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore String appId;

  String executionType;
  String application;
  @JsonProperty("pipeline") String entityName;
  List<ArtifactMetadata> inputArtifacts;
  ExecutionStatus status;

  List<PipelineStageExecutionMetadata> stages;

  List<NameValuePair> tags;
  TimingMetadata timing;
  TriggeredByMetadata triggeredBy;

  public void accept(GraphNodeVisitor visitor) {
    MetadataUtils.acceptMultiple(visitor, stages);
  }

  public static PipelineExecutionMetadata fromWorkflowExecution(WorkflowExecution workflowExecution) {
    if (workflowExecution == null || workflowExecution.getWorkflowType() != WorkflowType.PIPELINE) {
      return null;
    }

    return PipelineExecutionMetadata.builder()
        .id(workflowExecution.getUuid())
        .appId(workflowExecution.getAppId())
        .executionType("Pipeline")
        .application(workflowExecution.getAppName())
        .entityName(workflowExecution.getPipelineSummary() == null
                ? workflowExecution.getName()
                : workflowExecution.getPipelineSummary().getPipelineName())
        .inputArtifacts(ArtifactMetadata.fromArtifacts(
            workflowExecution.getExecutionArgs() == null ? null : workflowExecution.getExecutionArgs().getArtifacts()))
        .status(workflowExecution.getStatus())
        .stages(PipelineStageExecutionMetadata.fromPipelineExecution(workflowExecution.getPipelineExecution()))
        .tags(workflowExecution.getTags())
        .timing(TimingMetadata.fromStartAndEndTimeObjects(workflowExecution.getStartTs(), workflowExecution.getEndTs()))
        .triggeredBy(TriggeredByMetadata.fromWorkflowExecution(workflowExecution))
        .build();
  }
}
