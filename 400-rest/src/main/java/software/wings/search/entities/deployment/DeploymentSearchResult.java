/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.search.entities.deployment;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;

import software.wings.search.framework.EntityInfo;
import software.wings.search.framework.SearchResult;

import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class DeploymentSearchResult extends SearchResult {
  private String appId;
  private String appName;
  private ExecutionStatus status;
  private Set<EntityInfo> services;
  private Set<EntityInfo> workflows;
  private Set<EntityInfo> environments;
  private String workflowId;
  private String workflowName;
  private String pipelineId;
  private String pipelineName;
  private boolean workflowInPipeline;

  public DeploymentSearchResult(DeploymentView deploymentView, float searchScore) {
    super(deploymentView.getId(), deploymentView.getName(), deploymentView.getDescription(),
        deploymentView.getAccountId(), deploymentView.getCreatedAt(), deploymentView.getLastUpdatedAt(),
        deploymentView.getType(), deploymentView.getCreatedBy(), deploymentView.getLastUpdatedBy(), searchScore);
    this.appId = deploymentView.getAppId();
    this.appName = deploymentView.getAppName();
    this.status = deploymentView.getStatus();
    this.services = deploymentView.getServices();
    this.workflows = deploymentView.getWorkflows();
    this.environments = deploymentView.getEnvironments();
    this.workflowId = deploymentView.getWorkflowId();
    this.workflowName = deploymentView.getWorkflowName();
    this.pipelineId = deploymentView.getPipelineId();
    this.pipelineName = deploymentView.getPipelineName();
    this.workflowInPipeline = deploymentView.isWorkflowInPipeline();
  }
}
