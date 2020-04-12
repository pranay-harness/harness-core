package io.harness.facilitator.async;

import io.harness.annotations.ProducesFacilitator;
import io.harness.facilitate.Facilitator;
import io.harness.facilitate.FacilitatorType;
import io.harness.facilitate.io.FacilitatorParameters;
import io.harness.registries.facilitator.FacilitatorProducer;

@ProducesFacilitator
public class AsyncFacilitatorProducer implements FacilitatorProducer {
  @Override
  public Facilitator produce(FacilitatorParameters parameters) {
    return AsyncFacilitator.builder().build();
  }

  @Override
  public FacilitatorType getType() {
    return FacilitatorType.builder().type(FacilitatorType.ASYNC).build();
  }
}
