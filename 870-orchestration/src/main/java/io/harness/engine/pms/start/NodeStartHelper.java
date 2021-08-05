package io.harness.engine.pms.start;

import static io.harness.springdata.SpringDataMongoUtils.setUnset;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.HarnessStringUtils;
import io.harness.engine.ExecutionCheck;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.node.NodeExecutionTimeoutCallback;
import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.pms.commons.events.PmsEventSender;
import io.harness.execution.ExecutionModeUtils;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.start.NodeStartEvent;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.events.base.PmsEventCategory;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.expression.PmsEngineExpressionService;
import io.harness.pms.sdk.core.execution.NodeExecutionUtils;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.timeout.SdkTimeoutTrackerParameters;
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

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class NodeStartHelper {
  @Inject private PmsEventSender eventSender;
  @Inject private InterruptService interruptService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private TimeoutEngine timeoutEngine;
  @Inject private TimeoutRegistry timeoutRegistry;
  @Inject private PmsEngineExpressionService pmsEngineExpressionService;

  public void startNode(Ambiance ambiance, FacilitatorResponseProto facilitatorResponse) {
    ExecutionCheck check = interruptService.checkInterruptsPreInvocation(
        ambiance.getPlanExecutionId(), AmbianceUtils.obtainCurrentRuntimeId(ambiance));
    if (!check.isProceed()) {
      log.info("Not Proceeding with Execution : {}", check.getReason());
      return;
    }
    NodeExecution nodeExecution = prepareNodeExecutionForInvocation(ambiance, facilitatorResponse.getExecutionMode());
    log.info("Sending NodeExecution START event");
    sendEvent(nodeExecution, facilitatorResponse.getPassThroughDataBytes());
  }

  private void sendEvent(NodeExecution nodeExecution, ByteString passThroughData) {
    String serviceName = nodeExecution.getNode().getServiceName();
    NodeStartEvent nodeStartEvent =
        NodeStartEvent.newBuilder()
            .setAmbiance(nodeExecution.getAmbiance())
            .addAllRefObjects(nodeExecution.getNode().getRebObjectsList())
            .setFacilitatorPassThoroughData(passThroughData)
            .setStepParameters(ByteString.copyFromUtf8(HarnessStringUtils.emptyIfNull(
                RecastOrchestrationUtils.toJson(nodeExecution.getResolvedStepParameters()))))
            .setMode(nodeExecution.getMode())
            .build();
    eventSender.sendEvent(
        nodeExecution.getAmbiance(), nodeStartEvent.toByteString(), PmsEventCategory.NODE_START, serviceName, true);
  }

  private NodeExecution prepareNodeExecutionForInvocation(Ambiance ambiance, ExecutionMode executionMode) {
    NodeExecution nodeExecution = nodeExecutionService.get(AmbianceUtils.obtainCurrentRuntimeId(ambiance));
    return Preconditions.checkNotNull(nodeExecutionService.updateStatusWithOps(
        AmbianceUtils.obtainCurrentRuntimeId(ambiance), calculateStatusFromMode(executionMode),
        ops
        -> setUnset(ops, NodeExecutionKeys.timeoutInstanceIds, registerTimeouts(nodeExecution)),
        EnumSet.noneOf(Status.class)));
  }

  private Status calculateStatusFromMode(ExecutionMode executionMode) {
    switch (executionMode) {
      case CONSTRAINT:
        return Status.RESOURCE_WAITING;
      case APPROVAL:
        return Status.APPROVAL_WAITING;
      case ASYNC:
        return Status.ASYNC_WAITING;
      default:
        return Status.RUNNING;
    }
  }

  private List<String> registerTimeouts(NodeExecution nodeExecution) {
    List<TimeoutObtainment> timeoutObtainmentList = nodeExecution.getNode().getTimeoutObtainmentsList();
    List<String> timeoutInstanceIds = new ArrayList<>();
    TimeoutCallback timeoutCallback =
        new NodeExecutionTimeoutCallback(nodeExecution.getAmbiance().getPlanExecutionId(), nodeExecution.getUuid());
    if (EmptyPredicate.isEmpty(timeoutObtainmentList)) {
      if (ExecutionModeUtils.isParentMode(nodeExecution.getMode())) {
        return timeoutInstanceIds;
      }
      TimeoutTrackerFactory timeoutTrackerFactory = timeoutRegistry.obtain(AbsoluteTimeoutTrackerFactory.DIMENSION);
      TimeoutTracker timeoutTracker = timeoutTrackerFactory.create(
          AbsoluteTimeoutParameters.builder().timeoutMillis(TimeoutParameters.DEFAULT_TIMEOUT_IN_MILLIS).build());
      TimeoutInstance instance = timeoutEngine.registerTimeout(timeoutTracker, timeoutCallback);
      timeoutInstanceIds.add(instance.getUuid());
    } else {
      EngineExpressionEvaluator evaluator =
          pmsEngineExpressionService.prepareExpressionEvaluator(nodeExecution.getAmbiance());
      for (TimeoutObtainment timeoutObtainment : timeoutObtainmentList) {
        TimeoutTrackerFactory timeoutTrackerFactory = timeoutRegistry.obtain(timeoutObtainment.getDimension());

        // Evaluate timeout expressions and convert sdk timeout parameters to timeout engine specific parameters.
        SdkTimeoutTrackerParameters sdkTimeoutTrackerParameters =
            (SdkTimeoutTrackerParameters) kryoSerializer.asObject(timeoutObtainment.getParameters().toByteArray());
        sdkTimeoutTrackerParameters = resolve(evaluator, sdkTimeoutTrackerParameters);
        TimeoutParameters timeoutParameters = sdkTimeoutTrackerParameters.prepareTimeoutParameters();

        TimeoutTracker timeoutTracker = timeoutTrackerFactory.create(timeoutParameters);
        TimeoutInstance instance = timeoutEngine.registerTimeout(timeoutTracker, timeoutCallback);
        timeoutInstanceIds.add(instance.getUuid());
      }
    }
    log.info(format("Registered node execution timeouts: %s", timeoutInstanceIds.toString()));
    return timeoutInstanceIds;
  }

  private <T> T resolve(EngineExpressionEvaluator evaluator, T o) {
    if (o == null) {
      return null;
    }

    Class<?> cls = o.getClass();
    Map<String, Object> m = NodeExecutionUtils.extractObject(RecastOrchestrationUtils.toJson(o));
    String json = RecastOrchestrationUtils.toJson(evaluator.resolve(m, false));
    return (T) RecastOrchestrationUtils.fromJson(json, cls);
  }
}
