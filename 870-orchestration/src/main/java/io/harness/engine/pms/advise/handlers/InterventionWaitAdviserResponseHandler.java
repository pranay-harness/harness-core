package io.harness.engine.pms.advise.handlers;

import static io.harness.data.structure.HarnessStringUtils.emptyIfNull;
import static io.harness.pms.contracts.execution.Status.INTERVENTION_WAITING;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.executions.InterventionWaitTimeoutCallback;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.pms.advise.AdviserResponseHandler;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.InterventionWaitAdvise;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.OrchestrationEvent;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.registries.timeout.TimeoutRegistry;
import io.harness.serializer.KryoSerializer;
import io.harness.serializer.ProtoUtils;
import io.harness.timeout.TimeoutCallback;
import io.harness.timeout.TimeoutEngine;
import io.harness.timeout.TimeoutInstance;
import io.harness.timeout.TimeoutParameters;
import io.harness.timeout.TimeoutTracker;
import io.harness.timeout.TimeoutTrackerFactory;
import io.harness.timeout.contracts.TimeoutObtainment;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutParameters;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutTrackerFactory;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import org.bson.Document;

@OwnedBy(HarnessTeam.PIPELINE)
public class InterventionWaitAdviserResponseHandler implements AdviserResponseHandler {
  @Inject private OrchestrationEventEmitter eventEmitter;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private TimeoutRegistry timeoutRegistry;
  @Inject private TimeoutEngine timeoutEngine;

  @Override
  public void handleAdvise(NodeExecution nodeExecution, AdviserResponse adviserResponse) {
    InterventionWaitAdvise interventionWaitAdvise = adviserResponse.getInterventionWaitAdvise();

    timeoutEngine.deleteTimeouts(nodeExecution.getTimeoutInstanceIds());
    TimeoutCallback timeoutCallback =
        new InterventionWaitTimeoutCallback(nodeExecution.getAmbiance().getPlanExecutionId(), nodeExecution.getUuid());
    TimeoutObtainment timeoutObtainment =
        TimeoutObtainment.newBuilder()
            .setDimension(AbsoluteTimeoutTrackerFactory.DIMENSION)
            .setParameters(ByteString.copyFrom(
                kryoSerializer.asBytes(AbsoluteTimeoutParameters.builder()
                                           .timeoutMillis(getTimeoutInMillis(interventionWaitAdvise.getTimeout()))
                                           .build())))
            .build();
    TimeoutTrackerFactory timeoutTrackerFactory = timeoutRegistry.obtain(timeoutObtainment.getDimension());
    TimeoutTracker timeoutTracker = timeoutTrackerFactory.create(
        (TimeoutParameters) kryoSerializer.asObject(timeoutObtainment.getParameters().toByteArray()));
    TimeoutInstance instance = timeoutEngine.registerTimeout(timeoutTracker, timeoutCallback);

    nodeExecutionService.updateStatusWithOps(nodeExecution.getUuid(), INTERVENTION_WAITING,
        ops
        -> ops.set(NodeExecutionKeys.adviserTimeoutInstanceIds, Arrays.asList(instance.getUuid())),
        EnumSet.noneOf(Status.class));
    Document resolvedStepParameters = nodeExecution.getResolvedStepParameters();
    String stepParameters = null;
    if (resolvedStepParameters != null) {
      stepParameters = resolvedStepParameters.toJson();
    }
    eventEmitter.emitEvent(OrchestrationEvent.newBuilder()
                               .setEventType(OrchestrationEventType.INTERVENTION_WAIT_START)
                               .setAmbiance(nodeExecution.getAmbiance())
                               .setStatus(nodeExecution.getStatus())
                               .setStepParameters(ByteString.copyFromUtf8(emptyIfNull(stepParameters)))
                               .setServiceName(nodeExecution.getNode().getServiceName())
                               .setCreatedAt(ProtoUtils.unixMillisToTimestamp(System.currentTimeMillis()))
                               .build());
  }

  private long getTimeoutInMillis(Duration duration) {
    if (duration == null) {
      return TimeoutParameters.DEFAULT_TIMEOUT_IN_MILLIS;
    }
    return TimeUnit.SECONDS.toMillis(duration.getSeconds());
  }
}
