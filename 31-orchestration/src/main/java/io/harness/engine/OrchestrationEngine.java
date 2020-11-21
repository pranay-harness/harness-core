package io.harness.engine;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.execution.Status.ABORTED;
import static io.harness.pms.execution.Status.ERRORED;
import static io.harness.pms.execution.Status.EXPIRED;
import static io.harness.pms.execution.Status.FAILED;
import static io.harness.pms.execution.Status.RUNNING;
import static io.harness.pms.execution.Status.SUCCEEDED;
import static io.harness.springdata.SpringDataMongoUtils.setUnset;

import static java.lang.String.format;

import io.harness.LevelUtils;
import io.harness.OrchestrationPublisherName;
import io.harness.StatusUtils;
import io.harness.adviser.Advise;
import io.harness.adviser.AdviseType;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdvisingEvent;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.AmbianceUtils;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.Outcome;
import io.harness.delay.DelayEventHelper;
import io.harness.engine.advise.AdviseHandler;
import io.harness.engine.advise.AdviseHandlerFactory;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.executables.ExecutableProcessor;
import io.harness.engine.executables.ExecutableProcessorFactory;
import io.harness.engine.executables.InvocationHelper;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.node.NodeExecutionTimeoutCallback;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.engine.interrupts.InterruptCheck;
import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.resume.EngineResumeExecutor;
import io.harness.engine.resume.EngineWaitResumeCallback;
import io.harness.exception.ExceptionUtils;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.OrchestrationEventType;
import io.harness.facilitator.Facilitator;
import io.harness.facilitator.FacilitatorResponse;
import io.harness.logging.AutoLogContext;
import io.harness.plan.PlanNode;
import io.harness.pms.advisers.AdviserObtainment;
import io.harness.pms.execution.Status;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.registries.adviser.AdviserRegistry;
import io.harness.registries.facilitator.FacilitatorRegistry;
import io.harness.registries.resolver.ResolverRegistry;
import io.harness.registries.timeout.TimeoutRegistry;
import io.harness.resolvers.Resolver;
import io.harness.serializer.KryoSerializer;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepOutcomeRef;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepOutcome;
import io.harness.state.io.StepResponseNotifyData;
import io.harness.tasks.ResponseData;
import io.harness.timeout.TimeoutCallback;
import io.harness.timeout.TimeoutEngine;
import io.harness.timeout.TimeoutInstance;
import io.harness.timeout.TimeoutObtainment;
import io.harness.timeout.TimeoutTracker;
import io.harness.timeout.TimeoutTrackerFactory;
import io.harness.waiter.WaitNotifyEngine;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Please do not use this class outside of orchestration module. All the interactions with engine must be done via
 * {@link OrchestrationService}. This is for the internal workings of the engine
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Slf4j
@Redesign
@OwnedBy(CDC)
public class OrchestrationEngine {
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private AdviserRegistry adviserRegistry;
  @Inject private FacilitatorRegistry facilitatorRegistry;
  @Inject private ResolverRegistry resolverRegistry;
  @Inject private TimeoutRegistry timeoutRegistry;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;
  @Inject private ExecutableProcessorFactory executableProcessorFactory;
  @Inject private AdviseHandlerFactory adviseHandlerFactory;
  @Inject private DelayEventHelper delayEventHelper;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private EngineExpressionService engineExpressionService;
  @Inject private InterruptService interruptService;
  @Inject private AmbianceUtils ambianceUtils;
  @Inject private InvocationHelper invocationHelper;
  @Inject private TimeoutEngine timeoutEngine;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;
  @Inject private OrchestrationEventEmitter eventEmitter;

  public void startNodeExecution(String nodeExecutionId) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    facilitateAndStartStep(nodeExecution.getAmbiance(), nodeExecution);
  }

  public void startNodeExecution(Ambiance ambiance) {
    NodeExecution nodeExecution = nodeExecutionService.get(ambiance.obtainCurrentRuntimeId());
    facilitateAndStartStep(ambiance, nodeExecution);
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
            .status(Status.QUEUED)
            .notifyId(previousNodeExecution == null ? null : previousNodeExecution.getNotifyId())
            .parentId(previousNodeExecution == null ? null : previousNodeExecution.getParentId())
            .previousId(previousNodeExecution == null ? null : previousNodeExecution.getUuid())
            .build();
    nodeExecutionService.save(nodeExecution);
    executorService.submit(ExecutionEngineDispatcher.builder().ambiance(cloned).orchestrationEngine(this).build());
  }

  private Ambiance reBuildAmbiance(Ambiance ambiance, PlanNode node, String uuid) {
    Ambiance cloned = ambiance.obtainCurrentRuntimeId() == null ? ambiance : ambianceUtils.cloneForFinish(ambiance);
    cloned.addLevel(LevelUtils.buildLevelFromPlanNode(uuid, node));
    return cloned;
  }

  // Start to Facilitators
  private void facilitateAndStartStep(Ambiance ambiance, NodeExecution nodeExecution) {
    try (AutoLogContext ignore = ambiance.autoLogContext()) {
      log.info("Checking Interrupts before Node Start");
      InterruptCheck check = interruptService.checkAndHandleInterruptsBeforeNodeStart(
          ambiance.getPlanExecutionId(), ambiance.obtainCurrentRuntimeId());
      if (!check.isProceed()) {
        log.info("Suspending Execution. Reason : {}", check.getReason());
        return;
      }
      log.info("Proceeding with  Execution. Reason : {}", check.getReason());

      PlanNode node = nodeExecution.getNode();
      // Facilitate and execute
      StepInputPackage inputPackage = engineObtainmentHelper.obtainInputPackage(
          ambiance, node.getRefObjects(), nodeExecution.getAdditionalInputs());
      StepParameters resolvedStepParameters =
          (StepParameters) engineExpressionService.resolve(ambiance, node.getStepParameters());
      NodeExecution updatedNodeExecution =
          Preconditions.checkNotNull(nodeExecutionService.update(nodeExecution.getUuid(),
              ops -> setUnset(ops, NodeExecutionKeys.resolvedStepParameters, resolvedStepParameters)));
      facilitateExecution(ambiance, updatedNodeExecution, inputPackage);
    } catch (Exception exception) {
      handleError(ambiance, exception);
    }
  }

  private void facilitateExecution(Ambiance ambiance, NodeExecution nodeExecution, StepInputPackage inputPackage) {
    PlanNode node = nodeExecution.getNode();
    FacilitatorResponse facilitatorResponse = null;
    for (FacilitatorObtainment obtainment : node.getFacilitatorObtainments()) {
      Facilitator facilitator = facilitatorRegistry.obtain(obtainment.getType());
      facilitatorResponse = facilitator.facilitate(
          ambiance, nodeExecution.getResolvedStepParameters(), obtainment.getParameters().toByteArray(), inputPackage);
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
          nodeExecutionService.updateStatusWithOps(ambiance.obtainCurrentRuntimeId(), Status.TIMED_WAITING,
              ops -> ops.set(NodeExecutionKeys.initialWaitDuration, finalFacilitatorResponse.getInitialWait())));
      String resumeId =
          delayEventHelper.delay(finalFacilitatorResponse.getInitialWait().getSeconds(), Collections.emptyMap());
      waitNotifyEngine.waitForAllOn(publisherName,
          EngineWaitResumeCallback.builder()
              .ambiance(ambiance)
              .facilitatorResponse(finalFacilitatorResponse)
              .inputPackage(inputPackage)
              .build(),
          resumeId);
      return;
    }
    invokeExecutable(ambiance, facilitatorResponse, inputPackage);
  }

  public void invokeExecutable(
      Ambiance ambiance, FacilitatorResponse facilitatorResponse, StepInputPackage inputPackage) {
    NodeExecution nodeExecution = prepareNodeExecutionForInvocation(ambiance, facilitatorResponse);
    ExecutableProcessor invoker = executableProcessorFactory.obtainProcessor(facilitatorResponse.getExecutionMode());
    invoker.handleStart(InvokerPackage.builder()
                            .inputPackage(inputPackage)
                            .passThroughData(facilitatorResponse.getPassThroughData())
                            .nodeExecution(nodeExecution)
                            .build());
  }

  private List<String> registerTimeouts(NodeExecution nodeExecution) {
    StepParameters resolvedStepParameters = nodeExecution.getResolvedStepParameters();
    List<TimeoutObtainment> timeoutObtainmentList;
    if (resolvedStepParameters != null) {
      timeoutObtainmentList = resolvedStepParameters.fetchTimeouts();
    } else {
      timeoutObtainmentList = new StepParameters() {}.fetchTimeouts();
    }

    List<String> timeoutInstanceIds = new ArrayList<>();
    if (isEmpty(timeoutObtainmentList)) {
      return timeoutInstanceIds;
    }

    TimeoutCallback timeoutCallback =
        new NodeExecutionTimeoutCallback(nodeExecution.getAmbiance().getPlanExecutionId(), nodeExecution.getUuid());
    for (TimeoutObtainment timeoutObtainment : timeoutObtainmentList) {
      TimeoutTrackerFactory timeoutTrackerFactory = timeoutRegistry.obtain(timeoutObtainment.getType());
      TimeoutTracker timeoutTracker = timeoutTrackerFactory.create(timeoutObtainment.getParameters());
      TimeoutInstance instance = timeoutEngine.registerTimeout(timeoutTracker, timeoutCallback);
      timeoutInstanceIds.add(instance.getUuid());
    }
    log.info(format("Registered node execution timeouts: %s", timeoutInstanceIds.toString()));
    return timeoutInstanceIds;
  }

  private NodeExecution prepareNodeExecutionForInvocation(Ambiance ambiance, FacilitatorResponse facilitatorResponse) {
    NodeExecution nodeExecution = nodeExecutionService.get(ambiance.obtainCurrentRuntimeId());
    return Preconditions.checkNotNull(
        nodeExecutionService.updateStatusWithOps(ambiance.obtainCurrentRuntimeId(), Status.RUNNING, ops -> {
          ops.set(NodeExecutionKeys.mode, facilitatorResponse.getExecutionMode());
          ops.set(NodeExecutionKeys.startTs, System.currentTimeMillis());
          setUnset(ops, NodeExecutionKeys.timeoutInstanceIds, registerTimeouts(nodeExecution));
        }));
  }

  public void handleStepResponse(@NonNull String nodeExecutionId, @NonNull StepResponse stepResponse) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    Ambiance ambiance = nodeExecution.getAmbiance();
    List<StepOutcomeRef> outcomeRefs = handleOutcomes(ambiance, stepResponse.stepOutcomeMap());

    NodeExecution updatedNodeExecution = nodeExecutionService.update(nodeExecutionId, ops -> {
      setUnset(ops, NodeExecutionKeys.failureInfo, stepResponse.getFailureInfo());
      setUnset(ops, NodeExecutionKeys.outcomeRefs, outcomeRefs);
    });
    concludeNodeExecution(updatedNodeExecution, stepResponse.getStatus());
  }

  public void concludeNodeExecution(NodeExecution nodeExecution, Status status) {
    PlanNode node = nodeExecution.getNode();

    if (isEmpty(node.getAdviserObtainments())) {
      NodeExecution updatedNodeExecution = nodeExecutionService.updateStatusWithOps(
          nodeExecution.getUuid(), status, ops -> setUnset(ops, NodeExecutionKeys.endTs, System.currentTimeMillis()));
      endTransition(updatedNodeExecution);
      return;
    }
    for (AdviserObtainment obtainment : node.getAdviserObtainments()) {
      Adviser adviser = adviserRegistry.obtain(obtainment.getType());
      AdvisingEvent advisingEvent = AdvisingEvent.builder()
                                        .ambiance(nodeExecution.getAmbiance())
                                        .stepOutcomeRef(nodeExecution.getOutcomeRefs())
                                        .toStatus(status)
                                        .fromStatus(nodeExecution.getStatus())
                                        .adviserParameters(obtainment.getParameters().toByteArray())
                                        .failureInfo(nodeExecution.getFailureInfo())
                                        .build();
      if (adviser.canAdvise(advisingEvent)) {
        Advise advise = adviser.onAdviseEvent(advisingEvent);
        NodeExecution updatedNodeExecution =
            nodeExecutionService.updateStatusWithOps(nodeExecution.getUuid(), status, ops -> {
              if (AdviseType.isWaitingAdviseType(advise.getType())) {
                ops.set(NodeExecutionKeys.endTs, System.currentTimeMillis());
              }
            });
        handleAdvise(updatedNodeExecution.getAmbiance(), advise);
        return;
      }
    }
    NodeExecution updatedNodeExecution = nodeExecutionService.updateStatusWithOps(
        nodeExecution.getUuid(), status, ops -> setUnset(ops, NodeExecutionKeys.endTs, System.currentTimeMillis()));
    endTransition(updatedNodeExecution);
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
        Resolver resolver = resolverRegistry.obtain(Outcome.REF_TYPE);
        String instanceId = resolver.consume(ambiance, name, outcome, stepOutcome.getGroup());
        outcomeRefs.add(StepOutcomeRef.builder().name(name).instanceId(instanceId).build());
      }
    });
    return outcomeRefs;
  }

  public void endTransition(NodeExecution nodeExecution) {
    if (isNotEmpty(nodeExecution.getNotifyId())) {
      PlanNode planNode = nodeExecution.getNode();
      StepResponseNotifyData responseData = StepResponseNotifyData.builder()
                                                .nodeUuid(planNode.getUuid())
                                                .stepOutcomeRefs(nodeExecution.getOutcomeRefs())
                                                .failureInfo(nodeExecution.getFailureInfo())
                                                .identifier(planNode.getIdentifier())
                                                .group(planNode.getGroup())
                                                .status(nodeExecution.getStatus())
                                                .build();
      waitNotifyEngine.doneWith(nodeExecution.getNotifyId(), responseData);
    } else {
      log.info("Ending Execution");
      concludePlanExecution(nodeExecution.getAmbiance());
    }
  }

  private void concludePlanExecution(Ambiance ambiance) {
    Status status = calculateEndStatus(ambiance.getPlanExecutionId());
    PlanExecution planExecution = planExecutionService.updateStatus(
        ambiance.getPlanExecutionId(), status, ops -> ops.set(PlanExecutionKeys.endTs, System.currentTimeMillis()));
    eventEmitter.emitEvent(OrchestrationEvent.builder()
                               .ambiance(Ambiance.builder()
                                             .planExecutionId(planExecution.getUuid())
                                             .setupAbstractions(planExecution.getSetupAbstractions())
                                             .build())
                               .eventType(OrchestrationEventType.ORCHESTRATION_END)
                               .build());
  }

  // TODO (prashant) => Improve this with more clarity.
  private Status calculateEndStatus(String planExecutionId) {
    List<NodeExecution> nodeExecutions = nodeExecutionService.fetchNodeExecutionsWithoutOldRetries(planExecutionId);
    List<Status> statuses = nodeExecutions.stream().map(NodeExecution::getStatus).collect(Collectors.toList());
    if (StatusUtils.positiveStatuses().containsAll(statuses)) {
      return SUCCEEDED;
    } else if (statuses.stream().anyMatch(status -> status == ABORTED)) {
      return ABORTED;
    } else if (statuses.stream().anyMatch(status -> status == ERRORED)) {
      return ERRORED;
    } else if (statuses.stream().anyMatch(status -> status == FAILED)) {
      return FAILED;
    } else if (statuses.stream().anyMatch(status -> status == EXPIRED)) {
      return EXPIRED;
    } else {
      log.error("This should not Happen. PlanExecutionId : {}", planExecutionId);
      return ERRORED;
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
      if (!StatusUtils.resumableStatuses().contains(nodeExecution.getStatus())) {
        log.warn("NodeExecution is no longer in RESUMABLE state Uuid: {} Status {} ", nodeExecution.getUuid(),
            nodeExecution.getStatus());
        return;
      }
      if (nodeExecution.getStatus() != RUNNING) {
        nodeExecution = Preconditions.checkNotNull(nodeExecutionService.updateStatus(nodeExecutionId, RUNNING));
      }
      executorService.execute(EngineResumeExecutor.builder()
                                  .nodeExecution(nodeExecution)
                                  .response(response)
                                  .asyncError(asyncError)
                                  .orchestrationEngine(this)
                                  .processor(executableProcessorFactory.obtainProcessor(nodeExecution.getMode()))
                                  .build());
    } catch (Exception exception) {
      handleError(ambiance, exception);
    }
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
      log.error("Error when trying to obtain the advice ", ex);
    }
  }
}
