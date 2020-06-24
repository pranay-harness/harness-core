package io.harness.engine;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.execution.status.Status.RUNNING;
import static io.harness.execution.status.Status.resumableStatuses;
import static io.harness.ng.SpringDataMongoUtils.setUnset;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserObtainment;
import io.harness.adviser.AdvisingEvent;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.Outcome;
import io.harness.delay.DelayEventHelper;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.advise.AdviseHandler;
import io.harness.engine.advise.AdviseHandlerFactory;
import io.harness.engine.executables.ExecutableInvoker;
import io.harness.engine.executables.ExecutableInvokerFactory;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.engine.interrupts.InterruptCheck;
import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.resume.EngineResumeExecutor;
import io.harness.engine.resume.EngineWaitResumeCallback;
import io.harness.exception.ExceptionUtils;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.status.Status;
import io.harness.facilitator.Facilitator;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorResponse;
import io.harness.facilitator.PassThroughData;
import io.harness.logging.AutoLogContext;
import io.harness.plan.PlanNode;
import io.harness.registries.adviser.AdviserRegistry;
import io.harness.registries.facilitator.FacilitatorRegistry;
import io.harness.registries.resolver.ResolverRegistry;
import io.harness.registries.state.StepRegistry;
import io.harness.resolvers.Resolver;
import io.harness.state.Step;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepOutcomeRef;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepOutcome;
import io.harness.state.io.StepResponseNotifyData;
import io.harness.waiter.WaitNotifyEngine;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.validation.constraints.NotNull;

/**
 * Please do not use this class outside of orchestration module. All the interactions with engine must be done via
 * {@link EngineService}. This is for the internal workings of the engine
 */
@Slf4j
@Redesign
@OwnedBy(CDC)
public class ExecutionEngine {
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private Injector injector;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject private StepRegistry stepRegistry;
  @Inject private AdviserRegistry adviserRegistry;
  @Inject private FacilitatorRegistry facilitatorRegistry;
  @Inject private ResolverRegistry resolverRegistry;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;
  @Inject private ExecutableInvokerFactory executableInvokerFactory;
  @Inject private AdviseHandlerFactory adviseHandlerFactory;
  @Inject private DelayEventHelper delayEventHelper;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private EngineExpressionService engineExpressionService;
  @Inject private InterruptService interruptService;

  public void startNodeExecution(String nodeExecutionId) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    facilitateAndStartStep(nodeExecution.getAmbiance(), nodeExecution);
  }

  public void startNodeExecution(Ambiance ambiance) {
    NodeExecution nodeExecution = nodeExecutionService.get(ambiance.obtainCurrentRuntimeId());
    facilitateAndStartStep(ambiance, nodeExecution);
  }

  private void facilitateAndStartStep(Ambiance ambiance, NodeExecution nodeExecution) {
    try (AutoLogContext ignore = ambiance.autoLogContext()) {
      logger.info("Checking Interrupts before Node Start");
      InterruptCheck check = interruptService.checkAndHandleInterruptsBeforeNodeStart(
          ambiance.getPlanExecutionId(), ambiance.obtainCurrentRuntimeId());
      if (!check.isProceed()) {
        logger.info("Suspending Execution. Reason : {}", check.getReason());
        return;
      }
      logger.info("Proceeding with  Execution. Reason : {}", check.getReason());

      PlanNode node = nodeExecution.getNode();
      // Facilitate and execute
      StepInputPackage inputPackage = engineObtainmentHelper.obtainInputPackage(
          ambiance, node.getRefObjects(), nodeExecution.getAdditionalInputs());
      StepParameters resolvedStepParameters =
          (StepParameters) engineExpressionService.resolve(ambiance, node.getStepParameters());
      if (resolvedStepParameters != null) {
        nodeExecution = Preconditions.checkNotNull(nodeExecutionService.update(nodeExecution.getUuid(),
            ops -> setUnset(ops, NodeExecutionKeys.resolvedStepParameters, resolvedStepParameters)));
      }

      facilitateExecution(ambiance, nodeExecution, inputPackage);
    } catch (Exception exception) {
      handleError(ambiance, exception);
    }
  }

  public void triggerExecution(Ambiance ambiance, PlanNode node) {
    String uuid = generateUuid();
    NodeExecution previousNodeExecution = null;
    if (ambiance.obtainCurrentRuntimeId() != null) {
      previousNodeExecution = nodeExecutionService.update(
          ambiance.obtainCurrentRuntimeId(), ops -> ops.set(NodeExecutionKeys.nextId, uuid));
    }

    Ambiance cloned = reBuildAmbiance(ambiance, node, uuid);

    NodeExecution nodeExecution =
        NodeExecution.builder()
            .uuid(uuid)
            .node(node)
            .ambiance(cloned)
            .startTs(System.currentTimeMillis())
            .status(Status.QUEUED)
            .notifyId(previousNodeExecution == null ? null : previousNodeExecution.getNotifyId())
            .parentId(previousNodeExecution == null ? null : previousNodeExecution.getParentId())
            .previousId(previousNodeExecution == null ? null : previousNodeExecution.getUuid())
            .build();
    nodeExecutionService.save(nodeExecution);
    executorService.submit(ExecutionEngineDispatcher.builder().ambiance(cloned).executionEngine(this).build());
  }

  private Ambiance reBuildAmbiance(Ambiance ambiance, PlanNode node, String uuid) {
    Ambiance cloned = ambiance.obtainCurrentRuntimeId() == null ? ambiance : ambiance.cloneForFinish();
    cloned.addLevel(Level.fromPlanNode(uuid, node));
    return cloned;
  }

  private void facilitateExecution(Ambiance ambiance, NodeExecution nodeExecution, StepInputPackage inputPackage) {
    PlanNode node = nodeExecution.getNode();
    FacilitatorResponse facilitatorResponse = null;
    for (FacilitatorObtainment obtainment : node.getFacilitatorObtainments()) {
      Facilitator facilitator = facilitatorRegistry.obtain(obtainment.getType());
      injector.injectMembers(facilitator);
      facilitatorResponse = facilitator.facilitate(
          ambiance, nodeExecution.getResolvedStepParameters(), obtainment.getParameters(), inputPackage);
      if (facilitatorResponse != null) {
        break;
      }
    }
    Preconditions.checkNotNull(facilitatorResponse,
        "No execution mode detected for State. Name: " + node.getName() + "Type : " + node.getStepType());
    if (facilitatorResponse.getInitialWait() != null && facilitatorResponse.getInitialWait().getSeconds() != 0) {
      FacilitatorResponse finalFacilitatorResponse = facilitatorResponse;
      // Update Status
      Preconditions.checkNotNull(
          nodeExecutionService.updateStatusWithOps(ambiance.obtainCurrentRuntimeId(), Status.WAITING,
              ops -> ops.set(NodeExecutionKeys.initialWaitDuration, finalFacilitatorResponse.getInitialWait())));
      String resumeId =
          delayEventHelper.delay(finalFacilitatorResponse.getInitialWait().getSeconds(), Collections.emptyMap());
      waitNotifyEngine.waitForAllOn(ORCHESTRATION,
          EngineWaitResumeCallback.builder()
              .ambiance(ambiance)
              .facilitatorResponse(finalFacilitatorResponse)
              .inputPackage(inputPackage)
              .build(),
          resumeId);
      return;
    }
    invokeState(ambiance, facilitatorResponse, inputPackage);
  }

  public void invokeState(Ambiance ambiance, FacilitatorResponse facilitatorResponse, StepInputPackage inputPackage) {
    NodeExecution nodeExecution =
        Preconditions.checkNotNull(nodeExecutionService.updateStatusWithOps(ambiance.obtainCurrentRuntimeId(),
            Status.RUNNING, ops -> ops.set(NodeExecutionKeys.mode, facilitatorResponse.getExecutionMode())));
    PlanNode node = nodeExecution.getNode();
    Step currentStep = stepRegistry.obtain(node.getStepType());
    ExecutableInvoker invoker = executableInvokerFactory.obtainInvoker(facilitatorResponse.getExecutionMode());
    invoker.invokeExecutable(InvokerPackage.builder()
                                 .step(currentStep)
                                 .ambiance(ambiance)
                                 .inputPackage(inputPackage)
                                 .parameters(nodeExecution.getResolvedStepParameters())
                                 .passThroughData(facilitatorResponse.getPassThroughData())
                                 .start(true)
                                 .build());
  }

  public void handleStepResponse(@NonNull String nodeExecutionId, @NonNull StepResponse stepResponse) {
    NodeExecution nodeExecution =
        nodeExecutionService.updateStatusWithOps(nodeExecutionId, stepResponse.getStatus(), ops -> {
          setUnset(ops, NodeExecutionKeys.endTs, System.currentTimeMillis());
          setUnset(ops, NodeExecutionKeys.failureInfo, stepResponse.getFailureInfo());
        });
    // TODO => handle before node execution update
    Ambiance ambiance = nodeExecution.getAmbiance();
    List<StepOutcomeRef> outcomeRefs = handleOutcomes(ambiance, stepResponse.stepOutcomeMap());

    PlanNode node = nodeExecution.getNode();
    if (isEmpty(node.getAdviserObtainments())) {
      endTransition(nodeExecution, nodeExecution.getStatus(), stepResponse.getFailureInfo(), outcomeRefs);
      return;
    }
    Advise advise = null;
    for (AdviserObtainment obtainment : node.getAdviserObtainments()) {
      Adviser adviser = adviserRegistry.obtain(obtainment.getType());
      injector.injectMembers(adviser);
      advise = adviser.onAdviseEvent(AdvisingEvent.builder()
                                         .ambiance(ambiance)
                                         .outcomes(stepResponse.outcomeMap())
                                         .status(stepResponse.getStatus())
                                         .adviserParameters(obtainment.getParameters())
                                         .failureInfo(stepResponse.getFailureInfo())
                                         .build());
      if (advise != null) {
        break;
      }
    }
    if (advise == null) {
      endTransition(nodeExecution, nodeExecution.getStatus(), stepResponse.getFailureInfo(), outcomeRefs);
      return;
    }
    handleAdvise(ambiance, advise);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private List<StepOutcomeRef> handleOutcomes(Ambiance ambiance, Map<String, StepOutcome> stepOutcomes) {
    List<StepOutcomeRef> outcomeRefs = new ArrayList<>();
    if (stepOutcomes == null) {
      return outcomeRefs;
    }
    stepOutcomes.forEach((name, stepOutcome) -> {
      Outcome outcome = stepOutcome.getOutcome();
      if (outcome != null) {
        Resolver resolver = resolverRegistry.obtain(outcome.getRefType());
        String instanceId = resolver.consume(ambiance, name, outcome, stepOutcome.getGroup());
        outcomeRefs.add(StepOutcomeRef.builder().name(name).instanceId(instanceId).build());
      }
    });
    return outcomeRefs;
  }

  public void endTransition(
      NodeExecution nodeExecution, Status status, FailureInfo failureInfo, List<StepOutcomeRef> outcomeRefs) {
    if (isNotEmpty(nodeExecution.getNotifyId())) {
      PlanNode planNode = nodeExecution.getNode();
      StepResponseNotifyData responseData = StepResponseNotifyData.builder()
                                                .nodeUuid(planNode.getUuid())
                                                .stepOutcomesRefs(outcomeRefs)
                                                .failureInfo(failureInfo)
                                                .identifier(planNode.getIdentifier())
                                                .group(planNode.getGroup())
                                                .status(status)
                                                .build();
      waitNotifyEngine.doneWith(nodeExecution.getNotifyId(), responseData);
    } else {
      logger.info("Ending Execution");
      planExecutionService.updateStatus(nodeExecution.getAmbiance().getPlanExecutionId(), nodeExecution.getStatus());
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void handleAdvise(@NotNull Ambiance ambiance, @NotNull Advise advise) {
    AdviseHandler adviseHandler = adviseHandlerFactory.obtainHandler(advise.getType());
    adviseHandler.handleAdvise(ambiance, advise);
  }

  public void resume(String nodeExecutionId, Map<String, ResponseData> response, boolean asyncError) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    Ambiance ambiance = nodeExecution.getAmbiance();
    try (AutoLogContext ignore = ambiance.autoLogContext()) {
      if (!resumableStatuses().contains(nodeExecution.getStatus())) {
        logger.warn("NodeExecution is no longer in RESUMABLE state Uuid: {} Status {} ", nodeExecution.getUuid(),
            nodeExecution.getStatus());
        return;
      }
      if (nodeExecution.getStatus() != RUNNING) {
        nodeExecution = Preconditions.checkNotNull(nodeExecutionService.updateStatus(nodeExecutionId, RUNNING));
      }
      executorService.execute(EngineResumeExecutor.builder()
                                  .nodeExecution(nodeExecution)
                                  .ambiance(ambiance)
                                  .response(response)
                                  .asyncError(asyncError)
                                  .executionEngine(this)
                                  .stepRegistry(stepRegistry)
                                  .injector(injector)
                                  .build());
    } catch (Exception exception) {
      handleError(ambiance, exception);
    }
  }

  public void triggerLink(Step step, Ambiance ambiance, NodeExecution nodeExecution, PassThroughData passThroughData,
      Map<String, ResponseData> response) {
    PlanNode node = nodeExecution.getNode();
    StepInputPackage inputPackage =
        engineObtainmentHelper.obtainInputPackage(ambiance, node.getRefObjects(), nodeExecution.getAdditionalInputs());
    ExecutableInvoker invoker = executableInvokerFactory.obtainInvoker(nodeExecution.getMode());
    invoker.invokeExecutable(InvokerPackage.builder()
                                 .step(step)
                                 .ambiance(ambiance)
                                 .inputPackage(inputPackage)
                                 .parameters(nodeExecution.getResolvedStepParameters())
                                 .responseDataMap(response)
                                 .passThroughData(passThroughData)
                                 .start(false)
                                 .build());
  }

  public void handleError(Ambiance ambiance, Exception exception) {
    try {
      StepResponse response = StepResponse.builder()
                                  .status(Status.FAILED)
                                  .failureInfo(FailureInfo.builder()
                                                   .errorMessage(ExceptionUtils.getMessage(exception))
                                                   .failureTypes(ExceptionUtils.getFailureTypes(exception))
                                                   .build())
                                  .build();
      handleStepResponse(ambiance.obtainCurrentRuntimeId(), response);
    } catch (RuntimeException ex) {
      logger.error("Error when trying to obtain the advice ", ex);
    }
  }
}