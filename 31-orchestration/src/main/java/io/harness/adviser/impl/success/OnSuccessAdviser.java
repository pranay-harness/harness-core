package io.harness.adviser.impl.success;

import com.google.common.base.Preconditions;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.adviser.AdvisingEvent;
import io.harness.annotations.Produces;
import io.harness.annotations.Redesign;

@Redesign
@Produces(Adviser.class)
public class OnSuccessAdviser implements Adviser {
  @Override
  public Advise onAdviseEvent(AdvisingEvent advisingEvent) {
    OnSuccessAdviserParameters parameters =
        (OnSuccessAdviserParameters) Preconditions.checkNotNull(advisingEvent.getAdviserParameters());
    return OnSuccessAdvise.builder().nextNodeId(parameters.getNextNodeId()).build();
  }

  @Override
  public AdviserType getType() {
    return AdviserType.builder().type(AdviserType.ON_SUCCESS).build();
  }
}
