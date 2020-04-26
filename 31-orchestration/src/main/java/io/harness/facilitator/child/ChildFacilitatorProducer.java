package io.harness.facilitator.child;

import io.harness.annotations.ProducesFacilitator;
import io.harness.annotations.Redesign;
import io.harness.facilitate.Facilitator;
import io.harness.facilitate.FacilitatorType;
import io.harness.facilitate.io.FacilitatorParameters;
import io.harness.registries.facilitator.FacilitatorProducer;

@Redesign
@ProducesFacilitator
public class ChildFacilitatorProducer implements FacilitatorProducer {
  @Override
  public Facilitator produce(FacilitatorParameters parameters) {
    return ChildFacilitator.builder().build();
  }

  @Override
  public FacilitatorType getType() {
    return FacilitatorType.builder().type(FacilitatorType.CHILD).build();
  }
}
