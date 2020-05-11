package software.wings.service.impl.workflow.creation;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.validation.Validator.notNullCheck;
import static software.wings.beans.FeatureName.INFRA_MAPPING_REFACTOR;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.MapperUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.impl.workflow.WorkflowServiceTemplateHelper;
import software.wings.service.impl.workflow.creation.helpers.WorkflowPhaseHelper;
import software.wings.service.intfc.FeatureFlagService;

import java.util.ArrayList;
import java.util.List;

@OwnedBy(CDC)
@Slf4j
public class MultiPhaseWorkflowCreator extends WorkflowCreator {
  @Inject private FeatureFlagService featureFlagService;
  @Inject private WorkflowServiceHelper workflowServiceHelper;
  @Inject private WorkflowServiceTemplateHelper workflowServiceTemplateHelper;
  @Inject private WorkflowPhaseHelper workflowPhaseHelper;

  @Override
  public Workflow createWorkflow(Workflow clientWorkflow) {
    Workflow workflow = aWorkflow().build();
    MapperUtils.mapObject(clientWorkflow, workflow);
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck("orchestrationWorkflow", canaryOrchestrationWorkflow);
    addLinkedPreOrPostDeploymentSteps(canaryOrchestrationWorkflow);
    addWorkflowPhases(workflow);
    return workflow;
  }

  private void addWorkflowPhases(Workflow workflow) {
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    if (isNotEmpty(canaryOrchestrationWorkflow.getWorkflowPhases())) {
      List<WorkflowPhase> workflowPhases = canaryOrchestrationWorkflow.getWorkflowPhases();
      canaryOrchestrationWorkflow.setWorkflowPhases(new ArrayList<>());
      workflowPhases.forEach(workflowPhase -> attachWorkflowPhase(workflow, workflowPhase));
    }
  }

  @Override
  public void attachWorkflowPhase(Workflow workflow, WorkflowPhase workflowPhase) {
    workflowPhaseHelper.setCloudProviderIfNeeded(workflow, workflowPhase);
    boolean stepsGenerated = workflowPhaseHelper.addPhaseIfStepsGenerated(workflow, workflowPhase);
    if (stepsGenerated) {
      return;
    }

    boolean infraRefactor = featureFlagService.isEnabled(INFRA_MAPPING_REFACTOR, workflow.getAccountId());
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
    boolean serviceRepeat = canaryOrchestrationWorkflow.serviceRepeat(workflowPhase, infraRefactor);
    workflowServiceHelper.generateNewWorkflowPhaseSteps(workflow.getAppId(), workflow.getEnvId(), workflowPhase,
        serviceRepeat, orchestrationWorkflow.getOrchestrationWorkflowType(), workflow.getCreationFlags());
    workflowServiceTemplateHelper.addLinkedWorkflowPhaseTemplate(workflowPhase);
    canaryOrchestrationWorkflow.getWorkflowPhases().add(workflowPhase);

    WorkflowPhase rollbackWorkflowPhase =
        workflowServiceHelper.generateRollbackWorkflowPhase(workflow.getAppId(), workflowPhase, !serviceRepeat,
            orchestrationWorkflow.getOrchestrationWorkflowType(), workflow.getCreationFlags());
    workflowServiceTemplateHelper.addLinkedWorkflowPhaseTemplate(rollbackWorkflowPhase);
    canaryOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(workflowPhase.getUuid(), rollbackWorkflowPhase);
  }
}
