package io.harness.beans;

import io.harness.references.RefType;
import io.harness.state.io.StateTransput;

public interface SweepingOutput extends StateTransput {
  @Override
  default RefType getRefType() {
    return RefType.builder().type("SWEEPING_OUTPUT").build();
  }
}
