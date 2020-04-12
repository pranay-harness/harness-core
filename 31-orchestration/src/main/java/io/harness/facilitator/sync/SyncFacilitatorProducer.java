package io.harness.facilitator.sync;

import io.harness.annotations.ProducesFacilitator;
import io.harness.facilitate.Facilitator;
import io.harness.facilitate.FacilitatorType;
import io.harness.facilitate.io.FacilitatorParameters;
import io.harness.registries.facilitator.FacilitatorProducer;

@ProducesFacilitator
public class SyncFacilitatorProducer implements FacilitatorProducer {
  @Override
  public Facilitator produce(FacilitatorParameters parameters) {
    return SyncFacilitator.builder().build();
  }

  @Override
  public FacilitatorType getType() {
    return FacilitatorType.builder().type(FacilitatorType.SYNC).build();
  }
}
