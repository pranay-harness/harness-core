package software.wings.security;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.security.PermissionAttribute.Action;

import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
public class AppPermissionSummaryForUI {
  private boolean canCreateService;
  private boolean canCreateProvisioner;
  private boolean canCreateEnvironment;
  private boolean canCreateWorkflow;
  private boolean canCreatePipeline;

  // Key - entityId, Value - set of actions
  private Map<String, Set<Action>> servicePermissions;
  private Map<String, Set<Action>> provisionerPermissions;
  private Map<String, Set<Action>> envPermissions;
  private Map<String, Set<Action>> workflowPermissions;
  private Map<String, Set<Action>> deploymentPermissions;
  private Map<String, Set<Action>> pipelinePermissions;

  @Builder
  public AppPermissionSummaryForUI(boolean canCreateService, boolean canCreateProvisioner, boolean canCreateEnvironment,
      boolean canCreateWorkflow, boolean canCreatePipeline, Map<String, Set<Action>> servicePermissions,
      Map<String, Set<Action>> provisionerPermissions, Map<String, Set<Action>> envPermissions,
      Map<String, Set<Action>> workflowPermissions, Map<String, Set<Action>> deploymentPermissions,
      Map<String, Set<Action>> pipelinePermissions) {
    this.canCreateService = canCreateService;
    this.canCreateProvisioner = canCreateProvisioner;
    this.canCreateEnvironment = canCreateEnvironment;
    this.canCreateWorkflow = canCreateWorkflow;
    this.canCreatePipeline = canCreatePipeline;
    this.servicePermissions = servicePermissions;
    this.provisionerPermissions = provisionerPermissions;
    this.envPermissions = envPermissions;
    this.workflowPermissions = workflowPermissions;
    this.deploymentPermissions = deploymentPermissions;
    this.pipelinePermissions = pipelinePermissions;
  }
}
