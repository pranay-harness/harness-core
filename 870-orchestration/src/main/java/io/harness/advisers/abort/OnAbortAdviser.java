package io.harness.advisers.abort;

import static io.harness.pms.execution.Status.ABORTED;

import io.harness.StatusUtils;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.OrchestrationAdviserTypes;
import io.harness.pms.advisers.AdviseType;
import io.harness.pms.advisers.AdviserResponse;
import io.harness.pms.advisers.AdviserType;
import io.harness.pms.advisers.EndPlanAdvise;

public class OnAbortAdviser implements Adviser {
  public static final AdviserType ADVISER_TYPE =
      AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ABORT.name()).build();

  @Override
  public AdviserResponse onAdviseEvent(AdvisingEvent advisingEvent) {
    return AdviserResponse.newBuilder()
        .setEndPlanAdvise(EndPlanAdvise.newBuilder().build())
        .setType(AdviseType.END_PLAN)
        .build();
  }

  @Override
  public boolean canAdvise(AdvisingEvent advisingEvent) {
    return StatusUtils.brokeStatuses().contains(advisingEvent.getToStatus()) || ABORTED == advisingEvent.getToStatus();
  }
}
