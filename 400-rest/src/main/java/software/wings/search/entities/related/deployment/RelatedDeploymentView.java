/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
