/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.workflow.creation;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.validation.Validator.notNullCheck;

import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.serializer.MapperUtils;

import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.service.impl.workflow.creation.helpers.K8BlueGreenWorkflowPhaseHelper;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class K8V2BlueGreenWorkflowCreator extends WorkflowCreator {
  private static final String PHASE_NAME = "Blue/Green";

  @Inject private K8BlueGreenWorkflowPhaseHelper k8BlueGreenWorkflowPhaseHelper;

  @Override
  public Workflow createWorkflow(Workflow clientWorkflow) {
    Workflow workflow = aWorkflow().build();
    MapperUtils.mapObject(clientWorkflow, workflow);
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
    notNullCheck("orchestrationWorkflow", canaryOrchestrationWorkflow);
    if (k8BlueGreenWorkflowPhaseHelper.isCreationRequired(canaryOrchestrationWorkflow)) {
      addLinkedPreOrPostDeploymentSteps(canaryOrchestrationWorkflow);
      addWorkflowPhases(workflow);
    }
    return workflow;
  }

  private void addWorkflowPhases(Workflow workflow) {
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    WorkflowPhase workflowPhase = k8BlueGreenWorkflowPhaseHelper.getWorkflowPhase(workflow, PHASE_NAME);
    orchestrationWorkflow.getWorkflowPhases().add(workflowPhase);
    orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(
        workflowPhase.getUuid(), k8BlueGreenWorkflowPhaseHelper.getRollbackPhaseForWorkflowPhase(workflowPhase));
    for (WorkflowPhase phase : orchestrationWorkflow.getWorkflowPhases()) {
      attachWorkflowPhase(workflow, phase);
    }
  }

  @Override
  public void attachWorkflowPhase(Workflow workflow, WorkflowPhase workflowPhase) {
    // No action needed in attaching Phase
  }
}
