package io.harness.advisers.manualintervention;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.execution.status.Status.INTERVENTION_WAITING;
import static io.harness.execution.status.Status.brokeStatuses;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.advise.InterventionWaitAdvise;
import io.harness.state.io.FailureInfo;

import java.util.Collections;

public class ManualInterventionAdviser implements Adviser<ManualInterventionAdviserParameters> {
  public static final AdviserType ADVISER_TYPE = AdviserType.builder().type(AdviserType.MANUAL_INTERVENTION).build();

  @Override
  public Advise onAdviseEvent(AdvisingEvent<ManualInterventionAdviserParameters> advisingEvent) {
    return InterventionWaitAdvise.builder().build();
  }

  @Override
  public boolean canAdvise(AdvisingEvent<ManualInterventionAdviserParameters> advisingEvent) {
    boolean canAdvise =
        brokeStatuses().contains(advisingEvent.getToStatus()) && advisingEvent.getFromStatus() != INTERVENTION_WAITING;
    ManualInterventionAdviserParameters parameters = advisingEvent.getAdviserParameters();
    FailureInfo failureInfo = advisingEvent.getFailureInfo();
    if (failureInfo != null && !isEmpty(failureInfo.getFailureTypes())) {
      return canAdvise && !Collections.disjoint(parameters.getApplicableFailureTypes(), failureInfo.getFailureTypes());
    }
    return canAdvise;
  }
}
