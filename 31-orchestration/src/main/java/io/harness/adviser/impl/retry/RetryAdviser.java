package io.harness.adviser.impl.retry;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.adviser.AdvisingEvent;
import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Value;

@Value
@Redesign
public class RetryAdviser implements Adviser {
  AdviserType type = AdviserType.builder().type(AdviserType.RETRY).build();

  RetryAdviserParameters parameters;

  @Builder
  RetryAdviser(RetryAdviserParameters parameters) {
    this.parameters = parameters;
  }

  @Override
  public Advise onAdviseEvent(AdvisingEvent advisingEvent) {
    return null;
  }
}
