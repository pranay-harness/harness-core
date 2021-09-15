/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

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
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.registries.timeout.TimeoutRegistry;
import io.harness.timeout.TimeoutCallback;
import io.harness.timeout.TimeoutEngine;
import io.harness.timeout.TimeoutInstance;
import io.harness.timeout.TimeoutParameters;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutParameters;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutTrackerFactory;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@OwnedBy(HarnessTeam.PIPELINE)
public class InterventionWaitAdviserResponseHandler implements AdviserResponseHandler {
  @Inject private OrchestrationEventEmitter eventEmitter;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private TimeoutRegistry timeoutRegistry;
  @Inject private TimeoutEngine timeoutEngine;

  @Override
  public void handleAdvise(NodeExecution nodeExecution, AdviserResponse adviserResponse) {
    InterventionWaitAdvise interventionWaitAdvise = adviserResponse.getInterventionWaitAdvise();

    timeoutEngine.deleteTimeouts(nodeExecution.getTimeoutInstanceIds());
    TimeoutCallback timeoutCallback =
        new InterventionWaitTimeoutCallback(nodeExecution.getAmbiance().getPlanExecutionId(), nodeExecution.getUuid());
    TimeoutParameters parameters = AbsoluteTimeoutParameters.builder()
                                       .timeoutMillis(getTimeoutInMillis(interventionWaitAdvise.getTimeout()))
                                       .build();
    TimeoutInstance instance =
        timeoutEngine.registerTimeout(AbsoluteTimeoutTrackerFactory.DIMENSION, parameters, timeoutCallback);

    nodeExecutionService.updateStatusWithOps(nodeExecution.getUuid(), INTERVENTION_WAITING,
        ops
        -> ops.set(NodeExecutionKeys.adviserTimeoutInstanceIds, Arrays.asList(instance.getUuid())),
        EnumSet.noneOf(Status.class));
    Map<String, Object> resolvedStepParameters = nodeExecution.getResolvedStepParameters();
    String stepParameters = null;
    if (resolvedStepParameters != null) {
      stepParameters = RecastOrchestrationUtils.toJson(resolvedStepParameters);
    }
    eventEmitter.emitEvent(OrchestrationEvent.newBuilder()
                               .setEventType(OrchestrationEventType.INTERVENTION_WAIT_START)
                               .setAmbiance(nodeExecution.getAmbiance())
                               .setStatus(nodeExecution.getStatus())
                               .setStepParameters(ByteString.copyFromUtf8(emptyIfNull(stepParameters)))
                               .setServiceName(nodeExecution.getNode().getServiceName())
                               .build());
  }

  private long getTimeoutInMillis(Duration duration) {
    if (duration == null) {
      return TimeoutParameters.DEFAULT_TIMEOUT_IN_MILLIS;
    }
    return TimeUnit.SECONDS.toMillis(duration.getSeconds());
  }
}
