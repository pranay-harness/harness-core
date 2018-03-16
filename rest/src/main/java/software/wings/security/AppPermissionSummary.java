package software.wings.security;

import com.google.common.collect.Multimap;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.security.PermissionAttribute.Action;

/**
 * This class has the derived app permission summary.
 * The data is organized so that the lookup by AuthRuleFilter is faster.
 * @author rktummala on 3/8/18
 */
@Data
@NoArgsConstructor
public class AppPermissionSummary {
  private boolean canCreateService;
  private boolean canCreateEnvironment;
  private boolean canCreateWorkflow;
  private boolean canCreatePipeline;

  // Key - entityId, Value - set of actions
  private Multimap<Action, String> servicePermissions;
  private Multimap<Action, String> envPermissions;
  private Multimap<Action, String> workflowPermissions;
  private Multimap<Action, String> deploymentPermissions;
  private Multimap<Action, String> pipelinePermissions;

  @Builder
  public AppPermissionSummary(boolean canCreateService, boolean canCreateEnvironment, boolean canCreateWorkflow,
      boolean canCreatePipeline, Multimap<Action, String> servicePermissions, Multimap<Action, String> envPermissions,
      Multimap<Action, String> workflowPermissions, Multimap<Action, String> deploymentPermissions,
      Multimap<Action, String> pipelinePermissions) {
    this.canCreateService = canCreateService;
    this.canCreateEnvironment = canCreateEnvironment;
    this.canCreateWorkflow = canCreateWorkflow;
    this.canCreatePipeline = canCreatePipeline;
    this.servicePermissions = servicePermissions;
    this.envPermissions = envPermissions;
    this.workflowPermissions = workflowPermissions;
    this.deploymentPermissions = deploymentPermissions;
    this.pipelinePermissions = pipelinePermissions;
  }
}
