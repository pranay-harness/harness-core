package io.harness.advisers.manualintervention;

import static io.harness.execution.status.Status.brokeStatuses;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.advise.InterventionWaitAdvise;

public class ManualInterventionAdviser implements Adviser {
  @Override
  public Advise onAdviseEvent(AdvisingEvent advisingEvent) {
    if (!brokeStatuses().contains(advisingEvent.getStatus())) {
      return null;
    }
    return new InterventionWaitAdvise();
  }
}
