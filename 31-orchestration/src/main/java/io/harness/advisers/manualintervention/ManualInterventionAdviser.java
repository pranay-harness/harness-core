package io.harness.advisers.manualintervention;

import static io.harness.execution.status.Status.INTERVENTION_WAITING;
import static io.harness.execution.status.Status.brokeStatuses;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.advise.InterventionWaitAdvise;

public class ManualInterventionAdviser implements Adviser {
  public static final AdviserType ADVISER_TYPE = AdviserType.builder().type(AdviserType.MANUAL_INTERVENTION).build();

  @Override
  public Advise onAdviseEvent(AdvisingEvent advisingEvent) {
    if (!brokeStatuses().contains(advisingEvent.getToStatus())
        || advisingEvent.getFromStatus() == INTERVENTION_WAITING) {
      return null;
    }
    return InterventionWaitAdvise.builder().build();
  }
}
