package io.harness.advisers.retry;

import static io.harness.StatusUtils.retryableStatuses;
import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.AmbianceUtils;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.OrchestrationAdviserTypes;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.pms.advisers.AdviseType;
import io.harness.pms.advisers.AdviserResponse;
import io.harness.pms.advisers.AdviserType;
import io.harness.pms.advisers.EndPlanAdvise;
import io.harness.pms.advisers.InterventionWaitAdvise;
import io.harness.pms.advisers.NextStepAdvise;
import io.harness.pms.advisers.RetryAdvise;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.failure.FailureInfo;
import io.harness.serializer.KryoSerializer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.Duration;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Redesign
public class RetryAdviser implements Adviser {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private KryoSerializer kryoSerializer;

  public static final AdviserType ADVISER_TYPE =
      AdviserType.newBuilder().setType(OrchestrationAdviserTypes.RETRY.name()).build();

  @Override
  public AdviserResponse onAdviseEvent(AdvisingEvent advisingEvent) {
    RetryAdviserParameters parameters = extractParameters(advisingEvent);
    Ambiance ambiance = advisingEvent.getAmbiance();
    NodeExecution nodeExecution =
        Preconditions.checkNotNull(nodeExecutionService.get(AmbianceUtils.obtainCurrentRuntimeId(ambiance)));
    if (nodeExecution.retryCount() < parameters.getRetryCount()) {
      int waitInterval = calculateWaitInterval(parameters.getWaitIntervalList(), nodeExecution.retryCount());
      return AdviserResponse.newBuilder()
          .setType(AdviseType.RETRY)
          .setRetryAdvise(RetryAdvise.newBuilder()
                              .setRetryNodeExecutionId(nodeExecution.getUuid())
                              .setWaitInterval(waitInterval)
                              .build())
          .build();
    }
    return handlePostRetry(parameters);
  }

  @Override
  public boolean canAdvise(AdvisingEvent advisingEvent) {
    boolean canAdvise = retryableStatuses().contains(advisingEvent.getToStatus());
    FailureInfo failureInfo = advisingEvent.getFailureInfo();
    RetryAdviserParameters parameters = extractParameters(advisingEvent);
    if (failureInfo != null && !isEmpty(failureInfo.getFailureTypesList())) {
      return canAdvise
          && !Collections.disjoint(parameters.getApplicableFailureTypes(), failureInfo.getFailureTypesList());
    }
    return canAdvise;
  }

  private AdviserResponse handlePostRetry(RetryAdviserParameters parameters) {
    switch (parameters.getRepairActionCodeAfterRetry()) {
      case MANUAL_INTERVENTION:
        return AdviserResponse.newBuilder()
            .setInterventionWaitAdvise(
                InterventionWaitAdvise.newBuilder()
                    .setTimeout(Duration.newBuilder().setSeconds(java.time.Duration.ofDays(1).toMinutes() * 60).build())
                    .build())
            .setType(AdviseType.INTERVENTION_WAIT)
            .build();
      case END_EXECUTION:
        return AdviserResponse.newBuilder()
            .setEndPlanAdvise(EndPlanAdvise.newBuilder().build())
            .setType(AdviseType.END_PLAN)
            .build();
      case IGNORE:
        return AdviserResponse.newBuilder()
            .setNextStepAdvise(NextStepAdvise.newBuilder().setNextNodeId(parameters.getNextNodeId()).build())
            .setType(AdviseType.NEXT_STEP)
            .build();
      default:
        throw new IllegalStateException("Unexpected value: " + parameters.getRepairActionCodeAfterRetry());
    }
  }

  private int calculateWaitInterval(List<Integer> waitIntervalList, int retryCount) {
    if (isEmpty(waitIntervalList)) {
      return 0;
    }
    return waitIntervalList.size() <= retryCount ? waitIntervalList.get(waitIntervalList.size() - 1)
                                                 : waitIntervalList.get(retryCount);
  }

  @NotNull
  private RetryAdviserParameters extractParameters(AdvisingEvent advisingEvent) {
    return (RetryAdviserParameters) Preconditions.checkNotNull(
        kryoSerializer.asObject(advisingEvent.getAdviserParameters()));
  }
}
