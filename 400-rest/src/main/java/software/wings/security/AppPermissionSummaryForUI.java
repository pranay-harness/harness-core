/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.PermissionAttribute.Action;

import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@NoArgsConstructor
public class AppPermissionSummaryForUI {
  private boolean canCreateService;
  private boolean canCreateProvisioner;
  private boolean canCreateEnvironment;
  private boolean canCreateWorkflow;
  private boolean canCreatePipeline;
  private boolean canCreateTemplate;

  // Key - entityId, Value - set of actions
  private Map<String, Set<Action>> servicePermissions;
  private Map<String, Set<Action>> provisionerPermissions;
  private Map<String, Set<Action>> envPermissions;
  private Map<String, Set<Action>> workflowPermissions;
  private Map<String, Set<Action>> deploymentPermissions;
  private Map<String, Set<Action>> pipelinePermissions;
  private Map<String, Set<Action>> templatePermissions;

  @Builder
  public AppPermissionSummaryForUI(boolean canCreateService, boolean canCreateProvisioner, boolean canCreateEnvironment,
      boolean canCreateWorkflow, boolean canCreatePipeline, Map<String, Set<Action>> servicePermissions,
      Map<String, Set<Action>> provisionerPermissions, Map<String, Set<Action>> envPermissions,
      Map<String, Set<Action>> workflowPermissions, Map<String, Set<Action>> deploymentPermissions,
      Map<String, Set<Action>> pipelinePermissions, boolean canCreateTemplate,
      Map<String, Set<Action>> templatePermissions) {
    this.canCreateService = canCreateService;
    this.canCreateProvisioner = canCreateProvisioner;
    this.canCreateEnvironment = canCreateEnvironment;
    this.canCreateWorkflow = canCreateWorkflow;
    this.canCreatePipeline = canCreatePipeline;
    this.canCreateTemplate = canCreateTemplate;
    this.servicePermissions = servicePermissions;
    this.provisionerPermissions = provisionerPermissions;
    this.envPermissions = envPermissions;
    this.workflowPermissions = workflowPermissions;
    this.deploymentPermissions = deploymentPermissions;
    this.pipelinePermissions = pipelinePermissions;
    this.templatePermissions = templatePermissions;
  }
}
