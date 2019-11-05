package software.wings.sm.rollback;

import com.google.inject.Inject;

import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateMachine;

import java.util.Collections;
import javax.validation.constraints.NotNull;

public class RollbackStateMachineGenerator {
  @Inject private WorkflowService workflowService;

  public StateMachine generateForRollback(@NotNull String appId, @NotNull String workflowId, boolean infraRefactor) {
    Workflow workflow = workflowService.readWorkflow(appId, workflowId);
    return getStateMachine(appId, workflow, infraRefactor);
  }

  private StateMachine getStateMachine(@NotNull String appId, @NotNull Workflow workflow, boolean infraRefactor) {
    final OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    CanaryOrchestrationWorkflow modifiedOrchestrationWorkflow = modifyOrchestrationForRollback(orchestrationWorkflow);
    modifiedOrchestrationWorkflow.setGraph(modifiedOrchestrationWorkflow.generateGraph());
    return new StateMachine(workflow, workflow.getDefaultVersion(), modifiedOrchestrationWorkflow.getGraph(),
        workflowService.stencilMap(appId), infraRefactor);
  }

  private CanaryOrchestrationWorkflow modifyOrchestrationForRollback(OrchestrationWorkflow orchestrationWorkflow) {
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        (CanaryOrchestrationWorkflow) orchestrationWorkflow.cloneInternal();
    for (WorkflowPhase phase : canaryOrchestrationWorkflow.getWorkflowPhases()) {
      phase.setPhaseSteps(Collections.emptyList());
    }
    return canaryOrchestrationWorkflow;
  }
}
