package io.harness.advisers.abort;

import static io.harness.pms.execution.Status.ABORTED;

import io.harness.StatusUtils;
import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.OrchestrationAdviserTypes;
import io.harness.adviser.advise.EndPlanAdvise;
import io.harness.pms.advisers.AdviserType;

public class OnAbortAdviser implements Adviser {
  public static final AdviserType ADVISER_TYPE =
      AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ABORT.name()).build();

  @Override
  public Advise onAdviseEvent(AdvisingEvent advisingEvent) {
    return EndPlanAdvise.builder().build();
  }

  @Override
  public boolean canAdvise(AdvisingEvent advisingEvent) {
    return StatusUtils.brokeStatuses().contains(advisingEvent.getToStatus()) || ABORTED == advisingEvent.getToStatus();
  }
}
