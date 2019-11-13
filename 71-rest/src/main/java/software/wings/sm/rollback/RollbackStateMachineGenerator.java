package software.wings.sm.rollback;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.ROLLBACK_STAGED;
import static software.wings.sm.StateType.STAGING_ORIGINAL_EXECUTION;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.eraro.ErrorCode;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;
import software.wings.exception.InvalidRollbackException;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateMachine;
import software.wings.sm.states.StagingOriginalExecution.StagingOriginalExecutionKeys;

import java.util.Collections;
import javax.validation.constraints.NotNull;

public class RollbackStateMachineGenerator {
  public static final String ROLLBACK_STAGING_PHASE_NAME = "Rollback Staging";
  private static final String ROLLBACK_STAGING_PHASE_STEP_NAME = "Rollback Staged";
  private static final String ROLLBACK_STAGING_STEP_NAME = "Staging Original Execution";

  @Inject private WorkflowService workflowService;
  @Inject private WorkflowExecutionService workflowExecutionService;

  public StateMachine generateForRollbackExecution(@NotNull String appId, @NotNull String successfulExecutionId,
      boolean infraRefactor) throws InvalidRollbackException {
    WorkflowExecution successfulExecution = workflowExecutionService.getWorkflowExecution(appId, successfulExecutionId);
    if (!validForRollback(successfulExecution)) {
      throw new InvalidRollbackException("Execution Not Valid For Rollback", ErrorCode.INVALID_ROLLBACK);
    }
    return generateForRollback(appId, successfulExecution.getWorkflowId(), successfulExecutionId, infraRefactor);
  }

  private StateMachine generateForRollback(
      @NotNull String appId, @NotNull String workflowId, @NotNull String successfulExecutionId, boolean infraRefactor) {
    Workflow workflow = workflowService.readWorkflow(appId, workflowId);
    return getStateMachine(appId, workflow, successfulExecutionId, infraRefactor);
  }

  private StateMachine getStateMachine(
      @NotNull String appId, @NotNull Workflow workflow, @NotNull String successfulExecutionId, boolean infraRefactor) {
    final OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    CanaryOrchestrationWorkflow modifiedOrchestrationWorkflow =
        modifyOrchestrationForRollback(orchestrationWorkflow, successfulExecutionId);
    modifiedOrchestrationWorkflow.setGraph(modifiedOrchestrationWorkflow.generateGraph());
    return new StateMachine(workflow, workflow.getDefaultVersion(), modifiedOrchestrationWorkflow.getGraph(),
        workflowService.stencilMap(appId), infraRefactor);
  }

  private CanaryOrchestrationWorkflow modifyOrchestrationForRollback(
      OrchestrationWorkflow orchestrationWorkflow, String successfulExecutionId) {
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        (CanaryOrchestrationWorkflow) orchestrationWorkflow.cloneInternal();
    for (WorkflowPhase phase : canaryOrchestrationWorkflow.getWorkflowPhases()) {
      phase.setName(ROLLBACK_STAGING_PHASE_NAME);
      phase.setPhaseSteps(Collections.singletonList(getStagingStep(successfulExecutionId)));
      WorkflowPhase rollbackPhase = canaryOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(phase.getUuid());
      rollbackPhase.setPhaseNameForRollback(ROLLBACK_STAGING_PHASE_NAME);
    }
    return canaryOrchestrationWorkflow;
  }

  private PhaseStep getStagingStep(String successfulExecutionId) {
    PhaseStep phaseStep =
        aPhaseStep(ROLLBACK_STAGED, ROLLBACK_STAGING_PHASE_STEP_NAME)
            .addStep(GraphNode.builder()
                         .id(generateUuid())
                         .type(STAGING_ORIGINAL_EXECUTION.name())
                         .name(ROLLBACK_STAGING_STEP_NAME)
                         .properties(ImmutableMap.<String, Object>builder()
                                         .put(StagingOriginalExecutionKeys.successfulExecutionId, successfulExecutionId)
                                         .build())
                         .build())
            .build();
    phaseStep.setStepsIds(phaseStep.getSteps().stream().map(GraphNode::getId).collect(toList()));
    return phaseStep;
  }

  private boolean validForRollback(WorkflowExecution successfulExecution) {
    return successfulExecution != null && ExecutionStatus.SUCCESS.equals(successfulExecution.getStatus());
  }
}
