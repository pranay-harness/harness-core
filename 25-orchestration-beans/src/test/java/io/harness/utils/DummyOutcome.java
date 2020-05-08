package io.harness.utils;

import io.harness.data.Outcome;
import io.harness.data.OutcomeType;

public class DummyOutcome implements Outcome {
  @Override
  public OutcomeType getOutcomeType() {
    return OutcomeType.builder().type("OUTCOME").build();
  }
}
