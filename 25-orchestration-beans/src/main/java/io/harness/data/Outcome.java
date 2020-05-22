package io.harness.data;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.references.RefType;
import io.harness.state.io.StepTransput;

@OwnedBy(CDC)
public interface Outcome extends StepTransput {
  @Override
  default RefType getRefType() {
    return RefType.builder().type(RefType.OUTCOME).build();
  }
}
