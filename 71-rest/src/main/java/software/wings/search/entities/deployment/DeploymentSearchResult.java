package software.wings.search.entities.deployment;

import io.harness.beans.ExecutionStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.search.framework.EntityInfo;
import software.wings.search.framework.SearchResult;

import java.util.Set;

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

  public DeploymentSearchResult(DeploymentView deploymentView) {
    super(deploymentView.getId(), deploymentView.getName(), deploymentView.getDescription(),
        deploymentView.getAccountId(), deploymentView.getCreatedAt(), deploymentView.getLastUpdatedAt(),
        deploymentView.getType(), deploymentView.getCreatedBy(), deploymentView.getLastUpdatedBy());
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
