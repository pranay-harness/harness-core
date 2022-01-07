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
import static software.wings.common.WorkflowConstants.K8S_CANARY_PHASE_NAME;
import static software.wings.common.WorkflowConstants.K8S_PRIMARY_PHASE_NAME;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ff.FeatureFlagService;
import io.harness.serializer.MapperUtils;

import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.impl.workflow.WorkflowServiceTemplateHelper;
import software.wings.service.impl.workflow.creation.helpers.K8CanaryWorkflowPhaseHelper;
import software.wings.service.impl.workflow.creation.helpers.K8RollingWorkflowPhaseHelper;
import software.wings.service.impl.workflow.creation.helpers.WorkflowPhaseHelper;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
@Slf4j
public class K8V2CanaryWorkflowCreator extends WorkflowCreator {
  private static final String PHASE_NAME = "CANARY";
  @Inject private K8CanaryWorkflowPhaseHelper k8CanaryWorkflowPhaseHelper;
  @Inject private K8RollingWorkflowPhaseHelper k8RollingWorkflowPhaseHelper;
  @Inject private WorkflowServiceHelper workflowServiceHelper;
  @Inject private WorkflowServiceTemplateHelper workflowServiceTemplateHelper;
  @Inject private WorkflowPhaseHelper workflowPhaseHelper;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public Workflow createWorkflow(Workflow clientWorkflow) {
    Workflow workflow = aWorkflow().build();
    MapperUtils.mapObject(clientWorkflow, workflow);
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
    notNullCheck("orchestrationWorkflow", canaryOrchestrationWorkflow);
    if (k8CanaryWorkflowPhaseHelper.isCreationRequired(canaryOrchestrationWorkflow)) {
      addLinkedPreOrPostDeploymentSteps(canaryOrchestrationWorkflow);
      addWorkflowPhases(workflow);
    }
    return workflow;
  }

  private void addWorkflowPhases(Workflow workflow) {
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    WorkflowPhase workflowCanaryPhase = k8CanaryWorkflowPhaseHelper.getWorkflowPhase(workflow, PHASE_NAME);
    orchestrationWorkflow.getWorkflowPhases().add(workflowCanaryPhase);
    orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(workflowCanaryPhase.getUuid(),
        k8CanaryWorkflowPhaseHelper.getRollbackPhaseForWorkflowPhase(workflowCanaryPhase));

    WorkflowPhase workflowRollingPhase = k8RollingWorkflowPhaseHelper.getWorkflowPhase(workflow, "Primary");
    orchestrationWorkflow.getWorkflowPhases().add(workflowRollingPhase);
    orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(workflowRollingPhase.getUuid(),
        k8RollingWorkflowPhaseHelper.getRollbackPhaseForWorkflowPhase(workflowRollingPhase));
  }

  @Override
  public void attachWorkflowPhase(Workflow workflow, WorkflowPhase workflowPhase) {
    workflowPhaseHelper.setCloudProviderIfNeeded(workflow, workflowPhase);
    boolean stepsGenerated = workflowPhaseHelper.addPhaseIfStepsGenerated(workflow, workflowPhase);
    if (stepsGenerated) {
      return;
    }

    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
    boolean serviceRepeat = canaryOrchestrationWorkflow.serviceRepeat(workflowPhase);
    boolean createCanaryPhase = !serviceRepeat;
    boolean createPrimaryPhase =
        serviceRepeat && !canaryOrchestrationWorkflow.containsPhaseWithName(K8S_PRIMARY_PHASE_NAME);

    if (createPrimaryPhase) {
      if (isBlank(workflowPhase.getName())) {
        workflowPhase.setName(K8S_PRIMARY_PHASE_NAME);
      }
      workflowPhase.getPhaseSteps().addAll(k8RollingWorkflowPhaseHelper.getWorkflowPhaseSteps());
    } else if (createCanaryPhase) {
      if (isBlank(workflowPhase.getName())) {
        workflowPhase.setName(K8S_CANARY_PHASE_NAME);
      }
      workflowPhase.getPhaseSteps().addAll(k8CanaryWorkflowPhaseHelper.getWorkflowPhaseSteps());
    } else {
      workflowPhaseHelper.addK8sEmptyPhaseStep(workflowPhase);
    }

    workflowServiceTemplateHelper.addLinkedWorkflowPhaseTemplate(workflowPhase);
    canaryOrchestrationWorkflow.getWorkflowPhases().add(workflowPhase);
    WorkflowPhase rollbackWorkflowPhase = workflowPhaseHelper.createRollbackPhase(workflowPhase);

    if (createPrimaryPhase) {
      rollbackWorkflowPhase.getPhaseSteps().addAll(k8RollingWorkflowPhaseHelper.getRollbackPhaseSteps());
    } else if (createCanaryPhase) {
      rollbackWorkflowPhase.getPhaseSteps().addAll(k8CanaryWorkflowPhaseHelper.getRollbackPhaseSteps());
    } else {
      workflowPhaseHelper.addK8sEmptyRollbackPhaseStep(rollbackWorkflowPhase);
    }

    workflowServiceTemplateHelper.addLinkedWorkflowPhaseTemplate(rollbackWorkflowPhase);
    canaryOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(workflowPhase.getUuid(), rollbackWorkflowPhase);
  }
}
