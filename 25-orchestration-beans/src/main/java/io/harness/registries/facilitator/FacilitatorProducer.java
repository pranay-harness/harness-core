package io.harness.registries.facilitator;

import io.harness.annotations.Redesign;
import io.harness.facilitate.Facilitator;
import io.harness.facilitate.FacilitatorType;
import io.harness.facilitate.io.FacilitatorParameters;

@Redesign
public interface FacilitatorProducer {
  Facilitator produce(FacilitatorParameters parameters);

  FacilitatorType getType();
}
