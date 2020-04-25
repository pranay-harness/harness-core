package io.harness.adviser.impl.success;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.adviser.AdvisingEvent;
import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Value;

@Value
@Redesign
public class OnSuccessAdviser implements Adviser {
  AdviserType type = AdviserType.builder().type(AdviserType.ON_SUCCESS).build();

  OnSuccessAdviserParameters parameters;

  @Builder
  OnSuccessAdviser(OnSuccessAdviserParameters parameters) {
    this.parameters = parameters;
  }

  @Override
  public Advise onAdviseEvent(AdvisingEvent adviseEvent) {
    return OnSuccessAdvise.builder().nextNodeId(parameters.getNextNodeId()).build();
  }
}
