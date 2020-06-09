package io.harness.adviser.impl.success;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.execution.status.Status.positiveStatuses;

import com.google.common.base.Preconditions;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.advise.NextStepAdvise;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
@Redesign
public class OnSuccessAdviser implements Adviser {
  public static final AdviserType ADVISER_TYPE = AdviserType.builder().type(AdviserType.ON_SUCCESS).build();

  @Override
  public Advise onAdviseEvent(AdvisingEvent advisingEvent) {
    if (!positiveStatuses().contains(advisingEvent.getStatus())) {
      return null;
    }
    OnSuccessAdviserParameters parameters =
        (OnSuccessAdviserParameters) Preconditions.checkNotNull(advisingEvent.getAdviserParameters());
    return NextStepAdvise.builder().nextNodeId(parameters.getNextNodeId()).build();
  }
}
