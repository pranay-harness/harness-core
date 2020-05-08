package io.harness.utils;

import io.harness.data.Outcome;
import io.harness.data.OutcomeType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DummyOutcome implements Outcome {
  String test;

  @Override
  public OutcomeType getOutcomeType() {
    return OutcomeType.builder().type("DUMMY").build();
  }
}
