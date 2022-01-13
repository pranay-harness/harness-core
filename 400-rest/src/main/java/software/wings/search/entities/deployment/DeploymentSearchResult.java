/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
