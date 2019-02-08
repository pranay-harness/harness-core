package software.wings.security;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.security.PermissionAttribute.Action;

import java.util.Map;
import java.util.Set;

/**
 * This class has the derived app permission summary.
 * The data is organized so that the lookup by AuthRuleFilter is faster.
 * @author rktummala on 3/8/18
 */
@Data
@NoArgsConstructor
public class AppPermissionSummary {
  private boolean canCreateService;
  private boolean canCreateProvisioner;
  private boolean canCreateEnvironment;
  private boolean canCreateWorkflow;
  private boolean canCreatePipeline;

  private Set<EnvironmentType> envCreatePermissions;
  private Set<String> workflowCreatePermissions;
  private Set<String> pipelineCreatePermissions;
  private Set<String> deploymentEnvPermissions;

  // Key - action, Value - set of entity ids
  private Map<Action, Set<String>> servicePermissions;
  private Map<Action, Set<String>> provisionerPermissions;
  private Map<Action, Set<EnvInfo>> envPermissions;
  private Map<Action, Set<String>> workflowPermissions;
  private Map<Action, Set<String>> deploymentPermissions;
  private Map<Action, Set<String>> pipelinePermissions;

  @Data
  @Builder
  public static class EnvInfo {
    private String envId;
    private String envType;
  }

  @Builder
  public AppPermissionSummary(boolean canCreateService, boolean canCreateProvisioner, boolean canCreateEnvironment,
      boolean canCreateWorkflow, boolean canCreatePipeline, Set<EnvironmentType> envCreatePermissions,
      Set<String> workflowCreatePermissions, Set<String> pipelineCreatePermissions,
      Map<Action, Set<String>> servicePermissions, Map<Action, Set<String>> provisionerPermissions,
      Map<Action, Set<EnvInfo>> envPermissions, Map<Action, Set<String>> workflowPermissions,
      Map<Action, Set<String>> deploymentPermissions, Map<Action, Set<String>> pipelinePermissions,
      Set<String> deploymentEnvPermissions) {
    this.canCreateService = canCreateService;
    this.canCreateProvisioner = canCreateProvisioner;
    this.canCreateEnvironment = canCreateEnvironment;
    this.canCreateWorkflow = canCreateWorkflow;
    this.canCreatePipeline = canCreatePipeline;
    this.envCreatePermissions = envCreatePermissions;
    this.workflowCreatePermissions = workflowCreatePermissions;
    this.pipelineCreatePermissions = pipelineCreatePermissions;
    this.servicePermissions = servicePermissions;
    this.provisionerPermissions = provisionerPermissions;
    this.envPermissions = envPermissions;
    this.workflowPermissions = workflowPermissions;
    this.deploymentPermissions = deploymentPermissions;
    this.pipelinePermissions = pipelinePermissions;
    this.deploymentEnvPermissions = deploymentEnvPermissions;
  }
}
