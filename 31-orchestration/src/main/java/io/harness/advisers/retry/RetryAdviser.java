package io.harness.advisers.retry;

import static io.harness.StatusUtils.retryableStatuses;
import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.advise.EndPlanAdvise;
import io.harness.adviser.advise.InterventionWaitAdvise;
import io.harness.adviser.advise.NextStepAdvise;
import io.harness.adviser.advise.RetryAdvise;
import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.state.io.FailureInfo;

import java.util.Collections;
import java.util.List;

@OwnedBy(CDC)
@Redesign
public class RetryAdviser implements Adviser<RetryAdviserParameters> {
  @Inject private NodeExecutionService nodeExecutionService;

  public static final AdviserType ADVISER_TYPE = AdviserType.builder().type(AdviserType.RETRY).build();

  @Override
  public Advise onAdviseEvent(AdvisingEvent<RetryAdviserParameters> advisingEvent) {
    RetryAdviserParameters parameters = advisingEvent.getAdviserParameters();
    Ambiance ambiance = advisingEvent.getAmbiance();
    NodeExecution nodeExecution =
        Preconditions.checkNotNull(nodeExecutionService.get(ambiance.obtainCurrentRuntimeId()));
    if (nodeExecution.retryCount() < parameters.getRetryCount()) {
      int waitInterval = calculateWaitInterval(parameters.getWaitIntervalList(), nodeExecution.retryCount());
      return RetryAdvise.builder().retryNodeExecutionId(nodeExecution.getUuid()).waitInterval(waitInterval).build();
    }
    return handlePostRetry(parameters);
  }

  @Override
  public boolean canAdvise(AdvisingEvent<RetryAdviserParameters> advisingEvent) {
    boolean canAdvise = retryableStatuses().contains(advisingEvent.getToStatus());
    FailureInfo failureInfo = advisingEvent.getFailureInfo();
    RetryAdviserParameters parameters = advisingEvent.getAdviserParameters();
    if (failureInfo != null && !isEmpty(failureInfo.getFailureTypes())) {
      return canAdvise && !Collections.disjoint(parameters.getApplicableFailureTypes(), failureInfo.getFailureTypes());
    }
    return canAdvise;
  }

  private Advise handlePostRetry(RetryAdviserParameters parameters) {
    switch (parameters.getRepairActionCodeAfterRetry()) {
      case MANUAL_INTERVENTION:
        return InterventionWaitAdvise.builder().build();
      case END_EXECUTION:
        return EndPlanAdvise.builder().build();
      case IGNORE:
        return NextStepAdvise.builder().nextNodeId(parameters.getNextNodeId()).build();
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
}
