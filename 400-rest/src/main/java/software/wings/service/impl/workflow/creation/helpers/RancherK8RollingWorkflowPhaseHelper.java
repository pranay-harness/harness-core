/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.workflow.creation.helpers;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.K8S_PHASE_STEP;
import static software.wings.sm.StateType.RANCHER_K8S_DEPLOYMENT_ROLLING;
import static software.wings.sm.StateType.RANCHER_K8S_DEPLOYMENT_ROLLING_ROLLBACK;
import static software.wings.sm.StateType.RANCHER_RESOLVE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;

import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.common.WorkflowConstants;
import software.wings.service.impl.workflow.WorkflowServiceHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@OwnedBy(CDP)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class RancherK8RollingWorkflowPhaseHelper extends K8RollingWorkflowPhaseHelper {
  @Override
  public List<PhaseStep> getWorkflowPhaseSteps() {
    List<PhaseStep> phaseSteps = new ArrayList<>();
    phaseSteps.add(getRancherRollingDeployPhaseStep());
    phaseSteps.add(getRollingVerifyPhaseStep());
    phaseSteps.add(getRollingWrapUpPhaseStep());
    return phaseSteps;
  }

  private PhaseStep getRancherRollingDeployPhaseStep() {
    List<GraphNode> steps = new ArrayList<>();
    steps.add(GraphNode.builder()
                  .id(generateUuid())
                  .type(RANCHER_RESOLVE.name())
                  .name(WorkflowConstants.RANCHER_RESOLVE_CLUSTERS)
                  .properties(new HashMap<>())
                  .build());
    steps.add(GraphNode.builder()
                  .id(generateUuid())
                  .type(RANCHER_K8S_DEPLOYMENT_ROLLING.name())
                  .name(WorkflowConstants.RANCHER_K8S_DEPLOYMENT_ROLLING)
                  .properties(new HashMap<>())
                  .build());

    return aPhaseStep(K8S_PHASE_STEP, WorkflowServiceHelper.DEPLOY).addAllSteps(steps).build();
  }

  @Override
  public List<PhaseStep> getRollbackPhaseSteps() {
    List<PhaseStep> phaseSteps = new ArrayList<>();
    phaseSteps.add(getRancherRollingRollbackDeployPhaseStep());
    phaseSteps.add(getRollingRollbackWrapUpPhaseStep());
    return phaseSteps;
  }

  private PhaseStep getRancherRollingRollbackDeployPhaseStep() {
    return aPhaseStep(K8S_PHASE_STEP, WorkflowServiceHelper.DEPLOY)
        .addStep(GraphNode.builder()
                     .id(generateUuid())
                     .type(RANCHER_K8S_DEPLOYMENT_ROLLING_ROLLBACK.name())
                     .name(WorkflowConstants.RANCHER_K8S_DEPLOYMENT_ROLLING_ROLLBACK)
                     .rollback(true)
                     .build())
        .withPhaseStepNameForRollback(WorkflowServiceHelper.DEPLOY)
        .withStatusForRollback(ExecutionStatus.SUCCESS)
        .withRollback(true)
        .build();
  }
}
