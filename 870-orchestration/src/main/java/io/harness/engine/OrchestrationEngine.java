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

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delay.DelayEventHelper;
import io.harness.engine.advise.AdviseHandlerFactory;
import io.harness.engine.advise.AdviserResponseHandler;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.executables.ExecutableProcessor;
import io.harness.engine.executables.ExecutableProcessorFactory;
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
import io.harness.execution.NodeExecutionMapper;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.logging.AutoLogContext;
import io.harness.pms.advisers.AdviserObtainment;
import io.harness.pms.advisers.AdviserResponse;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.data.StepOutcomeRef;
import io.harness.pms.execution.Status;
import io.harness.pms.execution.failure.FailureInfo;
import io.harness.pms.execution.utils.AdviseTypeUtils;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.LevelUtils;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.pms.sdk.core.adviser.AdvisingEvent;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.facilitator.Facilitator;
import io.harness.pms.sdk.core.facilitator.FacilitatorResponse;
import io.harness.pms.sdk.core.resolver.Resolver;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.sdk.registries.AdviserRegistry;
import io.harness.pms.sdk.registries.FacilitatorRegistry;
import io.harness.pms.sdk.registries.ResolverRegistry;
import io.harness.pms.sdk.registries.StepRegistry;
import io.harness.pms.serializer.json.JsonOrchestrationUtils;
import io.harness.registries.timeout.TimeoutRegistry;
import io.harness.serializer.KryoSerializer;
import io.harness.state.io.StepResponseNotifyData;
import io.harness.tasks.ResponseData;
import io.harness.timeout.TimeoutCallback;
import io.harness.timeout.TimeoutEngine;
import io.harness.timeout.TimeoutInstance;
import io.harness.timeout.TimeoutObtainment;
import io.harness.timeout.TimeoutTracker;
import io.harness.timeout.TimeoutTrackerFactory;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutParameters;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutTrackerFactory;
import io.harness.waiter.WaitNotifyEngine;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

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
  @Inject private StepRegistry stepRegistry;
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
  @Inject private TimeoutEngine timeoutEngine;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;
  @Inject private OrchestrationEventEmitter eventEmitter;

  public void startNodeExecution(String nodeExecutionId) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    facilitateAndStartStep(nodeExecution.getAmbiance(), nodeExecution);
  }

  public void startNodeExecution(Ambiance ambiance) {
    NodeExecution nodeExecution = nodeExecutionService.get(AmbianceUtils.obtainCurrentRuntimeId(ambiance));
    facilitateAndStartStep(ambiance, nodeExecution);
  }

  public void triggerExecution(Ambiance ambiance, PlanNodeProto node) {
    String uuid = generateUuid();
    NodeExecution previousNodeExecution = null;
    if (AmbianceUtils.obtainCurrentRuntimeId(ambiance) != null) {
      previousNodeExecution = nodeExecutionService.update(
          AmbianceUtils.obtainCurrentRuntimeId(ambiance), ops -> ops.set(NodeExecutionKeys.nextId, uuid));
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
            .progressDataMap(new LinkedHashMap<>())
            .build();
    nodeExecutionService.save(nodeExecution);
    executorService.submit(ExecutionEngineDispatcher.builder().ambiance(cloned).orchestrationEngine(this).build());
  }

  private Ambiance reBuildAmbiance(Ambiance ambiance, PlanNodeProto node, String uuid) {
    Ambiance cloned =
        AmbianceUtils.obtainCurrentRuntimeId(ambiance) == null ? ambiance : AmbianceUtils.cloneForFinish(ambiance);
    cloned = cloned.toBuilder().addLevels(LevelUtils.buildLevelFromPlanNode(uuid, node)).build();
    return cloned;
  }

  // Start to Facilitators
  private void facilitateAndStartStep(Ambiance ambiance, NodeExecution nodeExecution) {
    try (AutoLogContext ignore = AmbianceUtils.autoLogContext(ambiance)) {
      log.info("Checking Interrupts before Node Start");
      InterruptCheck check = interruptService.checkAndHandleInterruptsBeforeNodeStart(
          ambiance.getPlanExecutionId(), AmbianceUtils.obtainCurrentRuntimeId(ambiance));
      if (!check.isProceed()) {
        log.info("Suspending Execution. Reason : {}", check.getReason());
        return;
      }
      log.info("Proceeding with  Execution. Reason : {}", check.getReason());

      PlanNodeProto node = nodeExecution.getNode();
      // Facilitate and execute
      StepInputPackage inputPackage = engineObtainmentHelper.obtainInputPackage(ambiance, node.getRebObjectsList());

      // Resolve step parameters
      String stepParameters = node.getStepParameters();
      Object obj = stepParameters == null
          ? null
          : engineExpressionService.resolve(ambiance, nodeExecutionService.extractStepParameters(nodeExecution));
      String json =
          obj instanceof StepParameters ? ((StepParameters) obj).toJson() : JsonOrchestrationUtils.asJson(obj);
      Document resolvedStepParameters = obj == null ? null : Document.parse(json);

      NodeExecution updatedNodeExecution =
          Preconditions.checkNotNull(nodeExecutionService.update(nodeExecution.getUuid(),
              ops -> setUnset(ops, NodeExecutionKeys.resolvedStepParameters, resolvedStepParameters)));
      facilitateExecution(ambiance, updatedNodeExecution, inputPackage);
    } catch (Exception exception) {
      handleError(ambiance, exception);
    }
  }

  private void facilitateExecution(Ambiance ambiance, NodeExecution nodeExecution, StepInputPackage inputPackage) {
    PlanNodeProto node = nodeExecution.getNode();
    FacilitatorResponse facilitatorResponse = null;
    for (FacilitatorObtainment obtainment : node.getFacilitatorObtainmentsList()) {
      Facilitator facilitator = facilitatorRegistry.obtain(obtainment.getType());
      facilitatorResponse =
          facilitator.facilitate(ambiance, nodeExecutionService.extractResolvedStepParameters(nodeExecution),
              obtainment.getParameters().toByteArray(), inputPackage);
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
          nodeExecutionService.updateStatusWithOps(AmbianceUtils.obtainCurrentRuntimeId(ambiance), Status.TIMED_WAITING,
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
    PlanExecution planExecution = Preconditions.checkNotNull(planExecutionService.get(ambiance.getPlanExecutionId()));
    NodeExecution nodeExecution = prepareNodeExecutionForInvocation(ambiance, facilitatorResponse);
    ExecutableProcessor invoker = executableProcessorFactory.obtainProcessor(facilitatorResponse.getExecutionMode());
    invoker.handleStart(InvokerPackage.builder()
                            .inputPackage(inputPackage)
                            .nodes(planExecution.getPlan().getNodes())
                            .passThroughData(facilitatorResponse.getPassThroughData())
                            .nodeExecution(NodeExecutionMapper.toNodeExecutionProto(nodeExecution))
                            .build());
  }

  private List<String> registerTimeouts(NodeExecution nodeExecution) {
    StepParameters resolvedStepParameters = nodeExecutionService.extractResolvedStepParameters(nodeExecution);
    List<TimeoutObtainment> timeoutObtainmentList =
        Collections.singletonList(TimeoutObtainment.builder()
                                      .type(AbsoluteTimeoutTrackerFactory.DIMENSION)
                                      .parameters(AbsoluteTimeoutParameters.builder()
                                                      .timeoutMillis(Duration.of(10, ChronoUnit.MINUTES).toMillis())
                                                      .build())
                                      .build());
    // TODO(gpahal): update later
    //    if (resolvedStepParameters != null) {
    //      timeoutObtainmentList = resolvedStepParameters.fetchTimeouts();
    //    } else {
    //      timeoutObtainmentList = new StepParameters() {}.fetchTimeouts();
    //    }

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
    NodeExecution nodeExecution = nodeExecutionService.get(AmbianceUtils.obtainCurrentRuntimeId(ambiance));
    return Preconditions.checkNotNull(nodeExecutionService.updateStatusWithOps(
        AmbianceUtils.obtainCurrentRuntimeId(ambiance), Status.RUNNING, ops -> {
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
    PlanNodeProto node = nodeExecution.getNode();

    if (isEmpty(node.getAdviserObtainmentsList())) {
      NodeExecution updatedNodeExecution = nodeExecutionService.updateStatusWithOps(
          nodeExecution.getUuid(), status, ops -> setUnset(ops, NodeExecutionKeys.endTs, System.currentTimeMillis()));
      endTransition(updatedNodeExecution);
      return;
    }
    for (AdviserObtainment obtainment : node.getAdviserObtainmentsList()) {
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
        AdviserResponse advise = adviser.onAdviseEvent(advisingEvent);
        NodeExecution updatedNodeExecution =
            nodeExecutionService.updateStatusWithOps(nodeExecution.getUuid(), status, ops -> {
              if (AdviseTypeUtils.isWaitingAdviseType(advise.getType())) {
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
        outcomeRefs.add(StepOutcomeRef.newBuilder().setName(name).setInstanceId(instanceId).build());
      }
    });
    return outcomeRefs;
  }

  public void endTransition(NodeExecution nodeExecution) {
    if (isNotEmpty(nodeExecution.getNotifyId())) {
      PlanNodeProto planNode = nodeExecution.getNode();
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
                               .ambiance(Ambiance.newBuilder()
                                             .setPlanExecutionId(planExecution.getUuid())
                                             .putAllSetupAbstractions(planExecution.getSetupAbstractions())
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
  private void handleAdvise(@NotNull Ambiance ambiance, @NotNull AdviserResponse adviserResponse) {
    AdviserResponseHandler adviserResponseHandler = adviseHandlerFactory.obtainHandler(adviserResponse.getType());
    adviserResponseHandler.handleAdvise(ambiance, adviserResponse);
  }

  public void resume(String nodeExecutionId, Map<String, ResponseData> response, boolean asyncError) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    Ambiance ambiance = nodeExecution.getAmbiance();
    try (AutoLogContext ignore = AmbianceUtils.autoLogContext(ambiance)) {
      if (!StatusUtils.resumableStatuses().contains(nodeExecution.getStatus())) {
        log.warn("NodeExecution is no longer in RESUMABLE state Uuid: {} Status {} ", nodeExecution.getUuid(),
            nodeExecution.getStatus());
        return;
      }

      PlanExecution planExecution = Preconditions.checkNotNull(planExecutionService.get(ambiance.getPlanExecutionId()));
      if (nodeExecution.getStatus() != RUNNING) {
        nodeExecution = Preconditions.checkNotNull(nodeExecutionService.updateStatus(nodeExecutionId, RUNNING));
      }
      executorService.execute(EngineResumeExecutor.builder()
                                  .nodeExecution(NodeExecutionMapper.toNodeExecutionProto(nodeExecution))
                                  .nodes(planExecution.getPlan().getNodes())
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
      StepResponse response =
          StepResponse.builder()
              .status(Status.FAILED)
              .failureInfo(FailureInfo.newBuilder()
                               .setErrorMessage(ExceptionUtils.getMessage(exception))
                               .addAllFailureTypes(EngineExceptionUtils.getOrchestrationFailureTypes(exception))
                               .build())
              .build();
      handleStepResponse(AmbianceUtils.obtainCurrentRuntimeId(ambiance), response);
    } catch (RuntimeException ex) {
      log.error("Error when trying to obtain the advice ", ex);
    }
  }
}
