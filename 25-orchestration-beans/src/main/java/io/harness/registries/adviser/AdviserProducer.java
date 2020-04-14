package io.harness.registries.adviser;

import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserParameters;
import io.harness.adviser.AdviserType;
import io.harness.annotations.Redesign;

@Redesign
public interface AdviserProducer {
  Adviser produce(AdviserParameters adviserParameters);

  AdviserType getType();
}
