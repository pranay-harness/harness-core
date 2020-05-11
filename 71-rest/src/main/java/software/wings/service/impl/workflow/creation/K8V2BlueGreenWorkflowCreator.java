package software.wings.service.impl.workflow.creation;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.validation.Validator.notNullCheck;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.MapperUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.service.impl.workflow.creation.helpers.K8BlueGreenWorkflowPhaseHelper;

@OwnedBy(CDC)
@Slf4j
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
