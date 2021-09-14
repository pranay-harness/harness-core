/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.search.entities.related.deployment;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;

import software.wings.beans.WorkflowExecution;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(PL)
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
