/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EnvironmentType;

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
public class AppPermissionSummary {
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

  // Set of Workflows given Update permission by entity
  private Set<String> workflowUpdatePermissionsByEntity;
  // Set of Pipelines given Update permission by entity
  private Set<String> pipelineUpdatePermissionsByEntity;

  /**
   * The environments that the user can rollback workflow to.
   */
  private Set<String> rollbackWorkflowExecutePermissionsForEnvs;

  // Key - action, Value - set of entity ids
  private Map<Action, Set<String>> servicePermissions;
  private Map<Action, Set<String>> provisionerPermissions;
  private Map<Action, Set<EnvInfo>> envPermissions;
  private Map<Action, Set<String>> workflowPermissions;
  // Key - action, Value - set of workflow ids / pipeline ids
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
      boolean canCreateWorkflow, boolean canCreateTemplatizedWorkflow, boolean canCreatePipeline,
      Set<EnvironmentType> envCreatePermissionsForEnvTypes, Set<String> workflowCreatePermissionsForEnvs,
      Set<String> workflowUpdatePermissionsForEnvs, Set<String> pipelineCreatePermissionsForEnvs,
      Set<String> pipelineUpdatePermissionsForEnvs, Set<String> deploymentExecutePermissionsForEnvs,
      Set<String> pipelineExecutePermissionsForEnvs, Set<String> workflowExecutePermissionsForEnvs,
      Set<String> rollbackWorkflowExecutePermissionsForEnvs, Set<String> pipelineUpdatePermissionsByEntity,
      Set<String> workflowUpdatePermissionsByEntity, Map<Action, Set<String>> servicePermissions,
      Map<Action, Set<String>> provisionerPermissions, Map<Action, Set<EnvInfo>> envPermissions,
      Map<Action, Set<String>> workflowPermissions, Map<Action, Set<String>> deploymentPermissions,
      Map<Action, Set<String>> pipelinePermissions) {
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
    this.workflowUpdatePermissionsByEntity = workflowUpdatePermissionsByEntity;
    this.pipelineUpdatePermissionsByEntity = pipelineUpdatePermissionsByEntity;
  }
}
