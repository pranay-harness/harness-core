package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EnvironmentType;

import software.wings.beans.Base;
import software.wings.security.PermissionAttribute.Action;

import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class has the derived app permission summary.
 * The data is organized so that the lookup by AuthRuleFilter is faster.
 * @author rktummala on 3/8/18
 */
@OwnedBy(PL)
@Data
@NoArgsConstructor
public class AppPermissionSummaryWithName {
  String appName;
  private boolean canCreateService;
  private boolean canCreateProvisioner;
  private boolean canCreateEnvironment;
  private boolean canCreateWorkflow;
  private boolean canCreateTemplatizedWorkflow;
  private boolean canCreatePipeline;

  /**
   * The environment types that the user can create.
   * The Set contains Env Ids
   */
  private Set<EnvironmentType> envCreatePermissionsForEnvTypes;

  /**
   * The environments that the user can create workflows for.
   * The Set contains Env Ids
   */
  private Set<String> workflowCreatePermissionsForEnvs;

  /**
   * The environments that the user can update workflows for.
   * The Set contains Env Ids
   */
  private Set<String> workflowUpdatePermissionsForEnvs;

  /**
   * The environments that the user can create pipelines for.
   * The Set contains Env Ids
   */
  private Set<String> pipelineCreatePermissionsForEnvs;

  /**
   * The environments that the user can update pipelines for.
   * The Set contains Env Ids
   */
  private Set<String> pipelineUpdatePermissionsForEnvs;

  /**
   * The environments that the user can deploy to.
   * This mapping is required for handling of rbac for templates
   * The Set contains Env Ids
   */
  @Deprecated private Set<String> deploymentExecutePermissionsForEnvs;
  private Set<String> workflowExecutePermissionsForEnvs;
  private Set<String> pipelineExecutePermissionsForEnvs;

  /**
   * The environments that the user can rollback workflow to.
   */
  private Set<String> rollbackWorkflowExecutePermissionsForEnvs;

  // Key - action, Value - set of entity ids
  private Map<Action, Set<EntityInfo>> servicePermissions;
  private Map<Action, Set<EntityInfo>> provisionerPermissions;
  private Map<Action, Set<EnvInfo>> envPermissions;
  private Map<Action, Set<Base>> workflowPermissions;
  // Key - action, Value - set of workflow ids / pipeline ids
  private Map<Action, Set<Base>> deploymentPermissions;
  private Map<Action, Set<Base>> pipelinePermissions;

  @Data
  @Builder
  public static class EnvInfo {
    private String envName;
    private String envId;
    private String envType;
  }

  @Builder
  public AppPermissionSummaryWithName(boolean canCreateService, boolean canCreateProvisioner, boolean canCreateEnvironment,
                                      boolean canCreateWorkflow, boolean canCreateTemplatizedWorkflow, boolean canCreatePipeline,
                                      Set<EnvironmentType> envCreatePermissionsForEnvTypes, Set<String> workflowCreatePermissionsForEnvs,
                                      Set<String> workflowUpdatePermissionsForEnvs, Set<String> pipelineCreatePermissionsForEnvs,
                                      Set<String> pipelineUpdatePermissionsForEnvs, Set<String> deploymentExecutePermissionsForEnvs,
                                      Set<String> pipelineExecutePermissionsForEnvs, Set<String> workflowExecutePermissionsForEnvs,
                                      Set<String> rollbackWorkflowExecutePermissionsForEnvs, Map<Action, Set<EntityInfo>> servicePermissions,
                                      Map<Action, Set<EntityInfo>> provisionerPermissions, Map<Action, Set<EnvInfo>> envPermissions,
                                      Map<Action, Set<Base>> workflowPermissions, Map<Action, Set<Base>> deploymentPermissions,
                                      Map<Action, Set<Base>> pipelinePermissions) {
    this.canCreateService = canCreateService;
    this.canCreateProvisioner = canCreateProvisioner;
    this.canCreateEnvironment = canCreateEnvironment;
    this.canCreateWorkflow = canCreateWorkflow;
    this.canCreateTemplatizedWorkflow = canCreateTemplatizedWorkflow;
    this.canCreatePipeline = canCreatePipeline;
    this.envCreatePermissionsForEnvTypes = envCreatePermissionsForEnvTypes;
    this.workflowCreatePermissionsForEnvs = workflowCreatePermissionsForEnvs;
    this.workflowUpdatePermissionsForEnvs = workflowUpdatePermissionsForEnvs;
    this.pipelineCreatePermissionsForEnvs = pipelineCreatePermissionsForEnvs;
    this.pipelineUpdatePermissionsForEnvs = pipelineUpdatePermissionsForEnvs;
    this.deploymentExecutePermissionsForEnvs = deploymentExecutePermissionsForEnvs;
    this.pipelineExecutePermissionsForEnvs = pipelineExecutePermissionsForEnvs;
    this.workflowExecutePermissionsForEnvs = workflowExecutePermissionsForEnvs;
    this.rollbackWorkflowExecutePermissionsForEnvs = rollbackWorkflowExecutePermissionsForEnvs;
    this.servicePermissions = servicePermissions;
    this.provisionerPermissions = provisionerPermissions;
    this.envPermissions = envPermissions;
    this.workflowPermissions = workflowPermissions;
    this.deploymentPermissions = deploymentPermissions;
    this.pipelinePermissions = pipelinePermissions;
  }
}
