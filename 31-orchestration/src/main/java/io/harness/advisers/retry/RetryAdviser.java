package io.harness.advisers.retry;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.execution.status.Status.retryableStatuses;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.advise.EndPlanAdvise;
import io.harness.adviser.advise.NextStepAdvise;
import io.harness.adviser.advise.RetryAdvise;
import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.AmbianceHelper;
import io.harness.execution.NodeExecution;

import java.util.List;

@OwnedBy(CDC)
@Redesign
public class RetryAdviser implements Adviser {
  @Inject AmbianceHelper ambianceHelper;

  public static final AdviserType ADVISER_TYPE = AdviserType.builder().type(AdviserType.RETRY).build();

  @Override
  public Advise onAdviseEvent(AdvisingEvent advisingEvent) {
    if (!retryableStatuses().contains(advisingEvent.getStatus())) {
      return null;
    }
    RetryAdviserParameters parameters = (RetryAdviserParameters) advisingEvent.getAdviserParameters();
    Ambiance ambiance = advisingEvent.getAmbiance();
    NodeExecution nodeExecution = Preconditions.checkNotNull(ambianceHelper.obtainNodeExecution(ambiance));
    if (nodeExecution.retryCount() < parameters.getRetryCount()) {
      int waitInterval = calculateWaitInterval(parameters.getWaitIntervalList(), nodeExecution.retryCount());
      return RetryAdvise.builder().retryNodeExecutionId(nodeExecution.getUuid()).waitInterval(waitInterval).build();
    }
    return handlePostRetry(ambiance, parameters);
  }

  private Advise handlePostRetry(Ambiance ambiance, RetryAdviserParameters parameters) {
    switch (parameters.getRepairActionCodeAfterRetry()) {
      case MANUAL_INTERVENTION:
      case ROLLBACK_WORKFLOW:
      case ROLLBACK_PHASE:
        return null;
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
