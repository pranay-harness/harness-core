/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.workflow.creation.helpers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.k8s.model.K8sExpressions.canaryWorkloadExpression;

import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.K8S_PHASE_STEP;
import static software.wings.sm.StateType.K8S_CANARY_DEPLOY;
import static software.wings.sm.StateType.K8S_DELETE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;

import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.common.WorkflowConstants;
import software.wings.service.impl.workflow.WorkflowServiceHelper;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class K8CanaryWorkflowPhaseHelper extends K8AbstractWorkflowHelper {
  @Override
  public List<PhaseStep> getWorkflowPhaseSteps() {
    List<PhaseStep> phaseSteps = new ArrayList<>();
    phaseSteps.add(getCanaryDeployPhaseStep());
    phaseSteps.add(getCanaryVerifyPhaseStep());
    phaseSteps.add(getCanaryWrapUpPhaseStep());
    return phaseSteps;
  }

  @Override
  public List<PhaseStep> getRollbackPhaseSteps() {
    List<PhaseStep> rollbackPhaseSteps = new ArrayList<>();
    rollbackPhaseSteps.add(getCanaryRollbackDeployPhaseStep());
    rollbackPhaseSteps.add(getCanaryRollbackWrapUpPhaseStep());
    return rollbackPhaseSteps;
  }

  // Steps for Canary
  private PhaseStep getCanaryDeployPhaseStep() {
    return aPhaseStep(K8S_PHASE_STEP, WorkflowServiceHelper.DEPLOY)
        .addStep(GraphNode.builder()
                     .id(generateUuid())
                     .type(K8S_CANARY_DEPLOY.name())
                     .name(WorkflowConstants.K8S_CANARY_DEPLOY)
                     .properties(ImmutableMap.<String, Object>builder()
                                     .put("instances", "1")
                                     .put("instanceUnitType", "COUNT")
                                     .build())
                     .build())
        .build();
  }

  private PhaseStep getCanaryVerifyPhaseStep() {
    return aPhaseStep(K8S_PHASE_STEP, "Verify").build();
  }

  private PhaseStep getCanaryWrapUpPhaseStep() {
    return aPhaseStep(K8S_PHASE_STEP, WorkflowServiceHelper.WRAP_UP)
        .addStep(GraphNode.builder()
                     .id(generateUuid())
                     .type(K8S_DELETE.name())
                     .name("Canary Delete")
                     .properties(ImmutableMap.<String, Object>builder()
                                     .put("resources", canaryWorkloadExpression)
                                     .put("instanceUnitType", "COUNT")
                                     .build())
                     .build())
        .build();
  }

  // Steps for Canary Rollback
  private PhaseStep getCanaryRollbackDeployPhaseStep() {
    return aPhaseStep(K8S_PHASE_STEP, WorkflowServiceHelper.DEPLOY)
        .withPhaseStepNameForRollback(WorkflowServiceHelper.DEPLOY)
        .withStatusForRollback(ExecutionStatus.SUCCESS)
        .withRollback(true)
        .build();
  }

  private PhaseStep getCanaryRollbackWrapUpPhaseStep() {
    return aPhaseStep(K8S_PHASE_STEP, WorkflowServiceHelper.WRAP_UP)
        .withPhaseStepNameForRollback(WorkflowServiceHelper.WRAP_UP)
        .withRollback(true)
        .build();
  }
}
