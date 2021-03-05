package io.harness.engine.advise.handlers;

import io.harness.engine.advise.AdviserResponseHandler;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.executions.InterventionWaitTimeoutCallback;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.NodeExecutionMapper;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.InterventionWaitAdvise;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
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

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class InterventionWaitAdviserResponseHandler implements AdviserResponseHandler {
  @Inject private OrchestrationEventEmitter eventEmitter;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private TimeoutRegistry timeoutRegistry;
  @Inject private TimeoutEngine timeoutEngine;

  @Override
  public void handleAdvise(NodeExecution nodeExecution, AdviserResponse adviserResponse) {
    InterventionWaitAdvise interventionWaitAdvise = adviserResponse.getInterventionWaitAdvise();

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

    nodeExecutionService.update(nodeExecution.getUuid(),
        ops -> ops.set(NodeExecutionKeys.adviserTimeoutInstanceIds, Arrays.asList(instance.getUuid())));

    eventEmitter.emitEvent(OrchestrationEvent.builder()
                               .eventType(OrchestrationEventType.INTERVENTION_WAIT_START)
                               .ambiance(nodeExecution.getAmbiance())
                               .nodeExecutionProto(NodeExecutionMapper.toNodeExecutionProto(nodeExecution))
                               .build());
    nodeExecutionService.updateStatus(nodeExecution.getUuid(), Status.INTERVENTION_WAITING);
    planExecutionService.updateStatus(nodeExecution.getAmbiance().getPlanExecutionId(), Status.INTERVENTION_WAITING);
  }

  private long getTimeoutInMillis(Duration duration) {
    if (duration == null) {
      return TimeoutParameters.DEFAULT_TIMEOUT_IN_MILLIS;
    }
    return TimeUnit.SECONDS.toMillis(duration.getSeconds());
  }
}
