package software.wings.sm.rollback;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.STAGE_EXECUTION;
import static software.wings.sm.StateType.STAGING_ORIGINAL_EXECUTION;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.eraro.ErrorCode;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.concurrency.ConcurrencyStrategy;
import software.wings.exception.InvalidRollbackException;
import software.wings.service.impl.workflow.queuing.WorkflowConcurrencyHelper;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateMachine;
import software.wings.sm.states.StagingOriginalExecution.StagingOriginalExecutionKeys;

import java.util.Collections;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
public class RollbackStateMachineGenerator {
  public static final String STAGING_PHASE_NAME = "Staging Execution";
  private static final String STAGING_PHASE_STEP_NAME = "Stage Rollback";
  private static final String STAGING_STEP_NAME = "Staging Original Execution";
  public static final String WHITE_SPACE = " ";

  @Inject private WorkflowService workflowService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private WorkflowConcurrencyHelper workflowConcurrencyHelper;

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
        modifyOrchestrationForRollback(appId, orchestrationWorkflow, successfulExecutionId);
    modifiedOrchestrationWorkflow.setGraph(modifiedOrchestrationWorkflow.generateGraph());
    return new StateMachine(workflow, workflow.getDefaultVersion(), modifiedOrchestrationWorkflow.getGraph(),
        workflowService.stencilMap(appId), infraRefactor, false);
  }

  private CanaryOrchestrationWorkflow modifyOrchestrationForRollback(
      String appId, OrchestrationWorkflow orchestrationWorkflow, String successfulExecutionId) {
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        (CanaryOrchestrationWorkflow) orchestrationWorkflow.cloneInternal();

    for (WorkflowPhase phase : canaryOrchestrationWorkflow.getWorkflowPhases()) {
      phase.setName(STAGING_PHASE_NAME + WHITE_SPACE + phase.getName());
      phase.setPhaseSteps(Collections.singletonList(
          getRollbackStagingPhaseStep(appId, canaryOrchestrationWorkflow, successfulExecutionId)));
      WorkflowPhase rollbackPhase = canaryOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(phase.getUuid());
      if (isNotEmpty(rollbackPhase.getPhaseSteps())) {
        PhaseStep rollbackPhaseStep = rollbackPhase.getPhaseSteps().get(0);
        rollbackPhaseStep.getSteps().add(
            0, getResourceConstraintStep(appId, canaryOrchestrationWorkflow.getConcurrencyStrategy()));
      }
    }
    return canaryOrchestrationWorkflow;
  }

  private PhaseStep getRollbackStagingPhaseStep(
      String appId, CanaryOrchestrationWorkflow canaryOrchestrationWorkflow, String successfulExecutionId) {
    PhaseStep phaseStep =
        aPhaseStep(STAGE_EXECUTION, STAGING_PHASE_STEP_NAME)
            .addStep(getResourceConstraintStep(appId, canaryOrchestrationWorkflow.getConcurrencyStrategy()))
            .addStep(getStagingExecutionStep(successfulExecutionId))
            .build();
    phaseStep.setStepsIds(phaseStep.getSteps().stream().map(GraphNode::getId).collect(toList()));
    return phaseStep;
  }

  private GraphNode getStagingExecutionStep(String successfulExecutionId) {
    return GraphNode.builder()
        .id(generateUuid())
        .type(STAGING_ORIGINAL_EXECUTION.name())
        .name(STAGING_STEP_NAME)
        .properties(ImmutableMap.<String, Object>builder()
                        .put(StagingOriginalExecutionKeys.successfulExecutionId, successfulExecutionId)
                        .build())
        .build();
  }

  private GraphNode getResourceConstraintStep(String appId, ConcurrencyStrategy concurrencyStrategy) {
    return workflowConcurrencyHelper.getResourceConstraintStep(appId, concurrencyStrategy);
  }

  private boolean validForRollback(WorkflowExecution successfulExecution) {
    return successfulExecution != null && ExecutionStatus.SUCCESS == successfulExecution.getStatus();
  }
}
