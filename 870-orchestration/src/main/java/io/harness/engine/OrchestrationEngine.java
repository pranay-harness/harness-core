package io.harness.engine;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.contracts.execution.Status.RUNNING;
import static io.harness.springdata.SpringDataMongoUtils.setUnset;

import static java.lang.String.format;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delay.DelayEventHelper;
import io.harness.engine.advise.AdviseHandlerFactory;
import io.harness.engine.advise.AdviserResponseHandler;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.executables.InvocationHelper;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.node.NodeExecutionTimeoutCallback;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.facilitation.InterruptPreFacilitationChecker;
import io.harness.engine.facilitation.RunPreFacilitationChecker;
import io.harness.engine.facilitation.SkipPreFacilitationChecker;
import io.harness.engine.interrupts.PreFacilitationCheck;
import io.harness.engine.pms.EngineAdviseCallback;
import io.harness.engine.pms.EngineFacilitationCallback;
import io.harness.engine.resume.EngineWaitResumeCallback;
import io.harness.exception.ExceptionUtils;
import io.harness.execution.ExecutionModeUtils;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.NodeExecutionMapper;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.contracts.plan.NodeExecutionEventType;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.execution.AdviseNodeExecutionEventData;
import io.harness.pms.execution.NodeExecutionEvent;
import io.harness.pms.execution.ResumeNodeExecutionEventData;
import io.harness.pms.execution.StartNodeExecutionEventData;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.EngineExceptionUtils;
import io.harness.pms.execution.utils.LevelUtils;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.expression.PmsEngineExpressionService;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.execution.NodeExecutionUtils;
import io.harness.pms.sdk.core.steps.io.StepResponseNotifyData;
import io.harness.registries.timeout.TimeoutRegistry;
import io.harness.serializer.KryoSerializer;
import io.harness.timeout.TimeoutCallback;
import io.harness.timeout.TimeoutEngine;
import io.harness.timeout.TimeoutInstance;
import io.harness.timeout.TimeoutParameters;
import io.harness.timeout.TimeoutTracker;
import io.harness.timeout.TimeoutTrackerFactory;
import io.harness.timeout.contracts.TimeoutObtainment;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutParameters;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutTrackerFactory;
import io.harness.waiter.WaitNotifyEngine;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Please do not use this class outside of orchestration module. All the interactions with engine must be done via
 * {@link OrchestrationService}. This is for the internal workings of the engine
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Slf4j
@OwnedBy(CDC)
public class OrchestrationEngine {
  @Inject private Injector injector;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject private TimeoutRegistry timeoutRegistry;
  @Inject private AdviseHandlerFactory adviseHandlerFactory;
  @Inject private DelayEventHelper delayEventHelper;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private PmsEngineExpressionService pmsEngineExpressionService;
  @Inject private TimeoutEngine timeoutEngine;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;
  @Inject private OrchestrationEventEmitter eventEmitter;
  @Inject private NodeExecutionEventQueuePublisher nodeExecutionEventQueuePublisher;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private EndNodeExecutionHelper endNodeExecutionHelper;
  @Inject private InvocationHelper invocationHelper;

  public void startNodeExecution(String nodeExecutionId) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    facilitateAndStartStep(nodeExecution.getAmbiance(), nodeExecution);
  }

  public void triggerExecution(Ambiance ambiance, PlanNodeProto node) {
    String uuid = generateUuid();
    NodeExecution previousNodeExecution = null;
    if (AmbianceUtils.obtainCurrentRuntimeId(ambiance) != null) {
      previousNodeExecution = nodeExecutionService.update(AmbianceUtils.obtainCurrentRuntimeId(ambiance),
          ops -> ops.set(NodeExecutionKeys.nextId, uuid).set(NodeExecutionKeys.endTs, System.currentTimeMillis()));
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
            .unitProgresses(new ArrayList<>())
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
      PreFacilitationCheck check = performPreFacilitationChecks(nodeExecution);
      if (!check.isProceed()) {
        log.info("Not Proceeding with  Execution. Reason : {}", check.getReason());
        return;
      }
      log.info("Proceeding with  Execution. Reason : {}", check.getReason());
      PlanNodeProto node = nodeExecution.getNode();
      String stepParameters = node.getStepParameters();
      Object resolvedStepParameters = stepParameters == null
          ? null
          : pmsEngineExpressionService.resolve(ambiance, NodeExecutionUtils.extractObject(stepParameters));
      Object resolvedStepInputs = node.getStepInputs() == null
          ? null
          : pmsEngineExpressionService.resolve(ambiance, NodeExecutionUtils.extractObject(node.getStepInputs()));

      NodeExecution updatedNodeExecution =
          Preconditions.checkNotNull(nodeExecutionService.update(nodeExecution.getUuid(), ops -> {
            setUnset(ops, NodeExecutionKeys.resolvedStepParameters, resolvedStepParameters);
            setUnset(ops, NodeExecutionKeys.resolvedStepInputs, resolvedStepInputs);
          }));

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

  private PreFacilitationCheck performPreFacilitationChecks(NodeExecution nodeExecution) {
    InterruptPreFacilitationChecker iChecker = injector.getInstance(InterruptPreFacilitationChecker.class);
    RunPreFacilitationChecker rChecker = injector.getInstance(RunPreFacilitationChecker.class);
    SkipPreFacilitationChecker sChecker = injector.getInstance(SkipPreFacilitationChecker.class);
    iChecker.setNextChecker(rChecker);
    rChecker.setNextChecker(sChecker);
    return iChecker.check(nodeExecution);
  }

  public void facilitateExecution(String nodeExecutionId, FacilitatorResponseProto facilitatorResponse) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    Ambiance ambiance = nodeExecution.getAmbiance();
    if (facilitatorResponse.getInitialWait() != null && facilitatorResponse.getInitialWait().getSeconds() != 0) {
      // Update Status
      Preconditions.checkNotNull(
          nodeExecutionService.updateStatusWithOps(AmbianceUtils.obtainCurrentRuntimeId(ambiance), Status.TIMED_WAITING,
              ops
              -> ops.set(NodeExecutionKeys.initialWaitDuration, facilitatorResponse.getInitialWait()),
              EnumSet.noneOf(Status.class)));
      String resumeId =
          delayEventHelper.delay(facilitatorResponse.getInitialWait().getSeconds(), Collections.emptyMap());
      waitNotifyEngine.waitForAllOn(publisherName,
          EngineWaitResumeCallback.builder().ambiance(ambiance).facilitatorResponse(facilitatorResponse).build(),
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
    List<TimeoutObtainment> timeoutObtainmentList;
    if (nodeExecution.getNode().getTimeoutObtainmentsList().isEmpty()) {
      timeoutObtainmentList = Collections.singletonList(
          TimeoutObtainment.newBuilder()
              .setDimension(AbsoluteTimeoutTrackerFactory.DIMENSION)
              .setParameters(ByteString.copyFrom(
                  kryoSerializer.asBytes(AbsoluteTimeoutParameters.builder()
                                             .timeoutMillis(TimeoutParameters.DEFAULT_TIMEOUT_IN_MILLIS)
                                             .build())))
              .build());
    } else {
      timeoutObtainmentList = nodeExecution.getNode().getTimeoutObtainmentsList();
    }

    List<String> timeoutInstanceIds = new ArrayList<>();
    TimeoutCallback timeoutCallback =
        new NodeExecutionTimeoutCallback(nodeExecution.getAmbiance().getPlanExecutionId(), nodeExecution.getUuid());
    for (TimeoutObtainment timeoutObtainment : timeoutObtainmentList) {
      TimeoutTrackerFactory timeoutTrackerFactory = timeoutRegistry.obtain(timeoutObtainment.getDimension());
      TimeoutTracker timeoutTracker = timeoutTrackerFactory.create(
          (TimeoutParameters) kryoSerializer.asObject(timeoutObtainment.getParameters().toByteArray()));
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
          if (!ExecutionModeUtils.isParentMode(nodeExecution.getMode())) {
            setUnset(ops, NodeExecutionKeys.timeoutInstanceIds, registerTimeouts(nodeExecution));
          }
        }, EnumSet.noneOf(Status.class)));
  }

  public void handleStepResponse(@NonNull String nodeExecutionId, @NonNull StepResponseProto stepResponse) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    PlanNodeProto node = nodeExecution.getNode();
    if (isEmpty(node.getAdviserObtainmentsList())) {
      endNodeExecutionHelper.endNodeExecutionWithNoAdvisers(nodeExecution, stepResponse);
      return;
    }
    NodeExecution updatedNodeExecution =
        endNodeExecutionHelper.handleStepResponsePreAdviser(nodeExecution, stepResponse);
    queueAdvisingEvent(updatedNodeExecution, nodeExecution.getStatus());
  }

  public void concludeNodeExecution(NodeExecution nodeExecution, Status status, EnumSet<Status> overrideStatusSet) {
    NodeExecution updatedNodeExecution = nodeExecutionService.updateStatusWithOps(nodeExecution.getUuid(), status,
        ops -> ops.set(NodeExecutionKeys.endTs, System.currentTimeMillis()), overrideStatusSet);
    if (updatedNodeExecution == null) {
      log.warn(
          "Cannot conclude node execution. Status update failed From :{}, To:{}", nodeExecution.getStatus(), status);
      return;
    }
    PlanNodeProto node = nodeExecution.getNode();
    if (isEmpty(node.getAdviserObtainmentsList())) {
      endTransition(nodeExecution);
      return;
    }
    queueAdvisingEvent(updatedNodeExecution, nodeExecution.getStatus());
  }

  public void concludeNodeExecution(NodeExecution nodeExecution, Status status) {
    concludeNodeExecution(nodeExecution, status, EnumSet.noneOf(Status.class));
  }

  public void queueAdvisingEvent(NodeExecution nodeExecution, Status fromStatus) {
    NodeExecutionEvent adviseEvent = NodeExecutionEvent.builder()
                                         .eventType(NodeExecutionEventType.ADVISE)
                                         .nodeExecution(NodeExecutionMapper.toNodeExecutionProto(nodeExecution))
                                         .eventData(AdviseNodeExecutionEventData.builder()
                                                        .toStatus(nodeExecution.getStatus())
                                                        .fromStatus(fromStatus)
                                                        .build())
                                         .build();

    nodeExecutionEventQueuePublisher.send(adviseEvent);
    waitNotifyEngine.waitForAllOn(publisherName,
        EngineAdviseCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build(), adviseEvent.getNotifyId());
  }

  public void endTransition(NodeExecution nodeExecution) {
    nodeExecutionService.update(
        nodeExecution.getUuid(), ops -> ops.set(NodeExecutionKeys.endTs, System.currentTimeMillis()));
    if (isNotEmpty(nodeExecution.getNotifyId())) {
      PlanNodeProto planNode = nodeExecution.getNode();
      StepResponseNotifyData responseData = StepResponseNotifyData.builder()
                                                .nodeUuid(planNode.getUuid())
                                                .stepOutcomeRefs(nodeExecution.getOutcomeRefs())
                                                .failureInfo(nodeExecution.getFailureInfo())
                                                .identifier(planNode.getIdentifier())
                                                .group(planNode.getGroup())
                                                .status(nodeExecution.getStatus())
                                                .adviserResponse(nodeExecution.getAdviserResponse())
                                                .build();
      waitNotifyEngine.doneWith(nodeExecution.getNotifyId(), responseData);
    } else {
      log.info("Ending Execution");
      concludePlanExecution(nodeExecution);
    }
  }

  private void concludePlanExecution(NodeExecution nodeExecution) {
    Ambiance ambiance = nodeExecution.getAmbiance();
    Status status = planExecutionService.calculateEndStatus(ambiance.getPlanExecutionId());
    PlanExecution planExecution = planExecutionService.updateStatus(
        ambiance.getPlanExecutionId(), status, ops -> ops.set(PlanExecutionKeys.endTs, System.currentTimeMillis()));
    eventEmitter.emitEvent(OrchestrationEvent.builder()
                               .ambiance(Ambiance.newBuilder()
                                             .setPlanExecutionId(planExecution.getUuid())
                                             .putAllSetupAbstractions(planExecution.getSetupAbstractions() == null
                                                     ? Collections.emptyMap()
                                                     : planExecution.getSetupAbstractions())
                                             .build())
                               .nodeExecutionProto(NodeExecutionMapper.toNodeExecutionProto(nodeExecution))
                               .eventType(OrchestrationEventType.ORCHESTRATION_END)
                               .build());
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

      if (nodeExecution.getStatus() != RUNNING) {
        nodeExecution = Preconditions.checkNotNull(
            nodeExecutionService.updateStatusWithOps(nodeExecutionId, RUNNING, null, EnumSet.noneOf(Status.class)));
      }

      ResumeNodeExecutionEventData data = ResumeNodeExecutionEventData.builder()
                                              .asyncError(asyncError)
                                              .response(invocationHelper.buildResponseMap(nodeExecution, response))
                                              .build();
      NodeExecutionEvent resumeEvent = NodeExecutionEvent.builder()
                                           .eventType(NodeExecutionEventType.RESUME)
                                           .nodeExecution(NodeExecutionMapper.toNodeExecutionProto(nodeExecution))
                                           .eventData(data)
                                           .build();
      nodeExecutionEventQueuePublisher.send(resumeEvent);
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

  public void handleAdvise(String nodeExecutionId, AdviserResponse adviserResponse) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    if (adviserResponse.getType() == AdviseType.UNKNOWN) {
      endNodeExecutionHelper.endNodeForNullAdvise(nodeExecution);
      return;
    }
    NodeExecution updatedNodeExecution = nodeExecutionService.update(
        nodeExecutionId, ops -> ops.set(NodeExecutionKeys.adviserResponse, adviserResponse));
    AdviserResponseHandler adviserResponseHandler = adviseHandlerFactory.obtainHandler(adviserResponse.getType());
    adviserResponseHandler.handleAdvise(nodeExecution, adviserResponse);
  }
}
