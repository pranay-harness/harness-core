package io.harness.engine;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.contracts.execution.Status.ABORTED;
import static io.harness.pms.contracts.execution.Status.ERRORED;
import static io.harness.pms.contracts.execution.Status.EXPIRED;
import static io.harness.pms.contracts.execution.Status.FAILED;
import static io.harness.pms.contracts.execution.Status.RUNNING;
import static io.harness.pms.contracts.execution.Status.SUCCEEDED;
import static io.harness.springdata.SpringDataMongoUtils.setUnset;

import static java.lang.String.format;

import io.harness.OrchestrationModuleConfig;
import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delay.DelayEventHelper;
import io.harness.engine.advise.AdviseHandlerFactory;
import io.harness.engine.advise.AdviserResponseHandler;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.node.NodeExecutionTimeoutCallback;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.interrupts.InterruptCheck;
import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.pms.EngineAdviseCallback;
import io.harness.engine.pms.EngineFacilitationCallback;
import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.engine.resume.EngineWaitResumeCallback;
import io.harness.exception.ExceptionUtils;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.NodeExecutionMapper;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.data.StepOutcomeRef;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.io.StepOutcomeProto;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.execution.AdviseNodeExecutionEventData;
import io.harness.pms.execution.NodeExecutionEvent;
import io.harness.pms.execution.NodeExecutionEventType;
import io.harness.pms.execution.ResumeNodeExecutionEventData;
import io.harness.pms.execution.StartNodeExecutionEventData;
import io.harness.pms.execution.utils.AdviseTypeUtils;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.EngineExceptionUtils;
import io.harness.pms.execution.utils.LevelUtils;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.expression.PmsEngineExpressionService;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.execution.EngineObtainmentHelper;
import io.harness.pms.sdk.core.execution.NodeExecutionUtils;
import io.harness.pms.sdk.core.registries.ResolverRegistry;
import io.harness.pms.sdk.core.resolver.Resolver;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepOutcomeMapper;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.sdk.core.steps.io.StepResponseNotifyData;
import io.harness.pms.serializer.json.JsonOrchestrationUtils;
import io.harness.registries.timeout.TimeoutRegistry;
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
import com.google.protobuf.ByteString;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
@OwnedBy(CDC)
public class OrchestrationEngine {
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject private ResolverRegistry resolverRegistry;
  @Inject private TimeoutRegistry timeoutRegistry;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;
  @Inject private AdviseHandlerFactory adviseHandlerFactory;
  @Inject private DelayEventHelper delayEventHelper;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private PmsEngineExpressionService pmsEngineExpressionService;
  @Inject private InterruptService interruptService;
  @Inject private TimeoutEngine timeoutEngine;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;
  @Inject private OrchestrationEventEmitter eventEmitter;
  @Inject private OrchestrationModuleConfig config;
  @Inject private NodeExecutionEventQueuePublisher nodeExecutionEventQueuePublisher;
  @Inject private PmsOutcomeService pmsOutcomeService;

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
      String stepParameters = node.getStepParameters();
      Object obj = stepParameters == null
          ? null
          : pmsEngineExpressionService.resolve(ambiance, NodeExecutionUtils.extractStepParameters(stepParameters));
      String json = JsonOrchestrationUtils.asJson(obj);
      Document resolvedStepParameters = obj == null ? null : Document.parse(json);

      NodeExecution updatedNodeExecution =
          Preconditions.checkNotNull(nodeExecutionService.update(nodeExecution.getUuid(),
              ops -> setUnset(ops, NodeExecutionKeys.resolvedStepParameters, resolvedStepParameters)));

      NodeExecutionEvent event = NodeExecutionEvent.builder()
                                     .nodeExecution(NodeExecutionMapper.toNodeExecutionProto(updatedNodeExecution))
                                     .eventType(NodeExecutionEventType.FACILITATE)
                                     .build();
      nodeExecutionEventQueuePublisher.send(event);
      waitNotifyEngine.waitForAllOn(publisherName,
          EngineFacilitationCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build(), event.getNotifyId());
    } catch (Exception exception) {
      handleError(ambiance, exception);
    }
  }

  public void facilitateExecution(String nodeExecutionId, FacilitatorResponseProto facilitatorResponse) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    Ambiance ambiance = nodeExecution.getAmbiance();
    PlanNodeProto node = nodeExecution.getNode();
    StepInputPackage inputPackage = engineObtainmentHelper.obtainInputPackage(ambiance, node.getRebObjectsList());
    if (facilitatorResponse.getInitialWait() != null && facilitatorResponse.getInitialWait().getSeconds() != 0) {
      // Update Status
      Preconditions.checkNotNull(
          nodeExecutionService.updateStatusWithOps(AmbianceUtils.obtainCurrentRuntimeId(ambiance), Status.TIMED_WAITING,
              ops -> ops.set(NodeExecutionKeys.initialWaitDuration, facilitatorResponse.getInitialWait())));
      String resumeId =
          delayEventHelper.delay(facilitatorResponse.getInitialWait().getSeconds(), Collections.emptyMap());
      waitNotifyEngine.waitForAllOn(publisherName,
          EngineWaitResumeCallback.builder()
              .ambiance(ambiance)
              .facilitatorResponse(facilitatorResponse)
              .inputPackage(inputPackage)
              .build(),
          resumeId);
      return;
    }
    invokeExecutable(ambiance, facilitatorResponse);
  }

  public void invokeExecutable(Ambiance ambiance, FacilitatorResponseProto facilitatorResponse) {
    PlanExecution planExecution = Preconditions.checkNotNull(planExecutionService.get(ambiance.getPlanExecutionId()));
    NodeExecution nodeExecution = prepareNodeExecutionForInvocation(ambiance, facilitatorResponse);

    StartNodeExecutionEventData startNodeExecutionEventData = StartNodeExecutionEventData.builder()
                                                                  .facilitatorResponse(facilitatorResponse)
                                                                  .nodes(planExecution.getPlan().getNodes())
                                                                  .build();
    NodeExecutionEvent startEvent = NodeExecutionEvent.builder()
                                        .eventType(NodeExecutionEventType.START)
                                        .nodeExecution(NodeExecutionMapper.toNodeExecutionProto(nodeExecution))
                                        .eventData(startNodeExecutionEventData)
                                        .build();
    nodeExecutionEventQueuePublisher.send(startEvent);
  }

  private List<String> registerTimeouts(NodeExecution nodeExecution) {
    // StepParameters resolvedStepParameters = nodeExecutionService.extractResolvedStepParameters(nodeExecution);
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

  private NodeExecution prepareNodeExecutionForInvocation(
      Ambiance ambiance, FacilitatorResponseProto facilitatorResponse) {
    NodeExecution nodeExecution = nodeExecutionService.get(AmbianceUtils.obtainCurrentRuntimeId(ambiance));
    return Preconditions.checkNotNull(nodeExecutionService.updateStatusWithOps(
        AmbianceUtils.obtainCurrentRuntimeId(ambiance), Status.RUNNING, ops -> {
          ops.set(NodeExecutionKeys.mode, facilitatorResponse.getExecutionMode());
          ops.set(NodeExecutionKeys.startTs, System.currentTimeMillis());
          setUnset(ops, NodeExecutionKeys.timeoutInstanceIds, registerTimeouts(nodeExecution));
        }));
  }

  public void handleStepResponse(@NonNull String nodeExecutionId, @NonNull StepResponseProto stepResponse) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    Ambiance ambiance = nodeExecution.getAmbiance();
    List<StepOutcomeRef> outcomeRefs = handleOutcomes(ambiance, stepResponse.getStepOutcomesList());

    NodeExecution updatedNodeExecution = nodeExecutionService.update(nodeExecutionId, ops -> {
      setUnset(ops, NodeExecutionKeys.failureInfo, stepResponse.getFailureInfo());
      setUnset(ops, NodeExecutionKeys.outcomeRefs, outcomeRefs);
    });
    concludeNodeExecution(updatedNodeExecution, stepResponse.getStatus());
  }

  public void concludeNodeExecution(NodeExecution nodeExecution, Status status) {
    PlanNodeProto node = nodeExecution.getNode();

    if (isEmpty(node.getAdviserObtainmentsList())) {
      endNodeExecution(nodeExecution, status);
      return;
    }

    NodeExecutionEvent adviseEvent =
        NodeExecutionEvent.builder()
            .eventType(NodeExecutionEventType.ADVISE)
            .nodeExecution(NodeExecutionMapper.toNodeExecutionProto(nodeExecution))
            .eventData(
                AdviseNodeExecutionEventData.builder().toStatus(status).fromStatus(nodeExecution.getStatus()).build())
            .build();

    nodeExecutionEventQueuePublisher.send(adviseEvent);
    waitNotifyEngine.waitForAllOn(publisherName,
        EngineAdviseCallback.builder().nodeExecutionId(nodeExecution.getUuid()).status(status).build(),
        adviseEvent.getNotifyId());
  }

  public void endNodeExecution(String nodeExecutionId, Status status) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    endNodeExecution(nodeExecution, status);
  }

  private void endNodeExecution(NodeExecution nodeExecution, Status status) {
    NodeExecution updatedNodeExecution = nodeExecutionService.updateStatusWithOps(
        nodeExecution.getUuid(), status, ops -> setUnset(ops, NodeExecutionKeys.endTs, System.currentTimeMillis()));
    endTransition(updatedNodeExecution);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private List<StepOutcomeRef> handleOutcomes(Ambiance ambiance, List<StepOutcomeProto> stepOutcomeProtos) {
    List<StepOutcomeRef> outcomeRefs = new ArrayList<>();
    if (isEmpty(stepOutcomeProtos)) {
      return outcomeRefs;
    }

    if (config.isWithPMS()) {
      stepOutcomeProtos.forEach(proto -> {
        if (isNotEmpty(proto.getOutcome())) {
          String instanceId =
              pmsOutcomeService.consume(ambiance, proto.getName(), proto.getOutcome(), proto.getGroup());
          outcomeRefs.add(StepOutcomeRef.newBuilder().setName(proto.getName()).setInstanceId(instanceId).build());
        }
      });
    } else {
      Map<String, StepOutcome> stepOutcomes = new HashMap<>();
      stepOutcomeProtos.forEach(
          proto -> stepOutcomes.put(proto.getName(), StepOutcomeMapper.fromStepOutcomeProto(proto)));
      stepOutcomes.forEach((name, stepOutcome) -> {
        Outcome outcome = stepOutcome.getOutcome();
        if (outcome != null) {
          Resolver resolver = resolverRegistry.obtain(Outcome.REF_TYPE);
          String instanceId = resolver.consume(ambiance, name, outcome, stepOutcome.getGroup());
          outcomeRefs.add(StepOutcomeRef.newBuilder().setName(name).setInstanceId(instanceId).build());
        }
      });
    }
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
      concludePlanExecution(nodeExecution);
    }
  }

  private void concludePlanExecution(NodeExecution nodeExecution) {
    Ambiance ambiance = nodeExecution.getAmbiance();
    Status status = calculateEndStatus(ambiance.getPlanExecutionId());
    PlanExecution planExecution = planExecutionService.updateStatus(
        ambiance.getPlanExecutionId(), status, ops -> ops.set(PlanExecutionKeys.endTs, System.currentTimeMillis()));
    eventEmitter.emitEvent(OrchestrationEvent.builder()
                               .ambiance(Ambiance.newBuilder()
                                             .setPlanExecutionId(planExecution.getUuid())
                                             .putAllSetupAbstractions(planExecution.getSetupAbstractions())
                                             .build())
                               .nodeExecutionProto(NodeExecutionMapper.toNodeExecutionProto(nodeExecution))
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

  private void handleAdvise(@NotNull NodeExecution nodeExecution, @NotNull AdviserResponse adviserResponse) {
    AdviserResponseHandler adviserResponseHandler = adviseHandlerFactory.obtainHandler(adviserResponse.getType());
    adviserResponseHandler.handleAdvise(nodeExecution, adviserResponse);
  }

  public void resume(String nodeExecutionId, Map<String, ByteString> response, boolean asyncError) {
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

      Map<String, byte[]> byteResponseMap = new HashMap<>();
      if (isNotEmpty(response)) {
        response.forEach((k, v) -> byteResponseMap.put(k, v.toByteArray()));
      }
      ResumeNodeExecutionEventData data = ResumeNodeExecutionEventData.builder()
                                              .asyncError(asyncError)
                                              .nodes(planExecution.getPlan().getNodes())
                                              .response(byteResponseMap)
                                              .build();
      NodeExecutionEvent resumeEvent = NodeExecutionEvent.builder()
                                           .eventType(NodeExecutionEventType.RESUME)
                                           .nodeExecution(NodeExecutionMapper.toNodeExecutionProto(nodeExecution))
                                           .eventData(data)
                                           .build();
      nodeExecutionEventQueuePublisher.send(resumeEvent);
      // Do something with the waitId
    } catch (Exception exception) {
      handleError(ambiance, exception);
    }
  }

  public void handleError(Ambiance ambiance, Exception exception) {
    try {
      StepResponseProto response =
          StepResponseProto.newBuilder()
              .setStatus(Status.FAILED)
              .setFailureInfo(FailureInfo.newBuilder()
                                  .setErrorMessage(ExceptionUtils.getMessage(exception))
                                  .addAllFailureTypes(EngineExceptionUtils.getOrchestrationFailureTypes(exception))
                                  .build())
              .build();
      handleStepResponse(AmbianceUtils.obtainCurrentRuntimeId(ambiance), response);
    } catch (RuntimeException ex) {
      log.error("Error when trying to obtain the advice ", ex);
    }
  }

  public void handleAdvise(String nodeExecutionId, Status status, AdviserResponse adviserResponse) {
    NodeExecution updatedNodeExecution = nodeExecutionService.updateStatusWithOps(nodeExecutionId, status, ops -> {
      if (AdviseTypeUtils.isWaitingAdviseType(adviserResponse.getType())) {
        ops.set(NodeExecutionKeys.endTs, System.currentTimeMillis());
      }
    });
    handleAdvise(updatedNodeExecution, adviserResponse);
  }
}
