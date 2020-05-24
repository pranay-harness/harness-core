package io.harness.adviser.impl.success;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.execution.status.NodeExecutionStatus.positiveStatuses;

import com.google.common.base.Preconditions;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.NextStepAdvise;
import io.harness.annotations.Produces;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.state.io.StepResponse;

@OwnedBy(CDC)
@Redesign
@Produces(Adviser.class)
public class OnSuccessAdviser implements Adviser {
  public static final AdviserType ADVISER_TYPE = AdviserType.builder().type(AdviserType.ON_SUCCESS).build();

  @Override
  public Advise onAdviseEvent(AdvisingEvent advisingEvent) {
    StepResponse stateResponse = advisingEvent.getStepResponse();
    if (!positiveStatuses().contains(stateResponse.getStatus())) {
      return null;
    }
    OnSuccessAdviserParameters parameters =
        (OnSuccessAdviserParameters) Preconditions.checkNotNull(advisingEvent.getAdviserParameters());
    return NextStepAdvise.builder().nextNodeId(parameters.getNextNodeId()).build();
  }

  @Override
  public AdviserType getType() {
    return ADVISER_TYPE;
  }
}
