package io.harness.advisers.success;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.StatusUtils;
import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.OrchestrationAdviserTypes;
import io.harness.adviser.advise.NextStepAdvise;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.advisers.AdviserType;
import io.harness.serializer.KryoSerializer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

@OwnedBy(CDC)
@Redesign
public class OnSuccessAdviser implements Adviser {
  @Inject KryoSerializer kryoSerializer;

  public static final AdviserType ADVISER_TYPE =
      AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build();

  @Override
  public Advise onAdviseEvent(AdvisingEvent advisingEvent) {
    OnSuccessAdviserParameters parameters = (OnSuccessAdviserParameters) Preconditions.checkNotNull(
        kryoSerializer.asObject(advisingEvent.getAdviserParameters()));
    return NextStepAdvise.builder().nextNodeId(parameters.getNextNodeId()).build();
  }

  @Override
  public boolean canAdvise(AdvisingEvent advisingEvent) {
    return StatusUtils.positiveStatuses().contains(advisingEvent.getToStatus());
  }
}
