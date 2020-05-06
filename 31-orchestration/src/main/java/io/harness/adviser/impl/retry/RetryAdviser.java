package io.harness.adviser.impl.retry;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.adviser.AdvisingEvent;
import io.harness.annotations.Produces;
import io.harness.annotations.Redesign;
import io.harness.engine.AmbianceHelper;
import io.harness.execution.NodeExecution;

@Redesign
@Produces(Adviser.class)
public class RetryAdviser implements Adviser {
  @Inject AmbianceHelper ambianceHelper;

  @Override
  public Advise onAdviseEvent(AdvisingEvent advisingEvent) {
    RetryAdviserParameters parameters = (RetryAdviserParameters) advisingEvent.getAdviserParameters();
    NodeExecution nodeExecution =
        Preconditions.checkNotNull(ambianceHelper.obtainNodeExecution(advisingEvent.getAmbiance()));
    if (nodeExecution.retryCount() <= parameters.getRetryCount()) {
      return RetryAdvise.builder().retryNodeExecutionId(nodeExecution.getUuid()).waitInterval(0).build();
    }

    // TODO => handle repair action code after retry
    return null;
  }

  @Override
  public AdviserType getType() {
    return AdviserType.builder().type(AdviserType.RETRY).build();
  }
}
