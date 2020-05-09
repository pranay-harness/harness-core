package io.harness.data;

import io.harness.references.RefType;
import io.harness.state.io.StateTransput;

public interface Outcome extends StateTransput {
  @Override
  default RefType getRefType() {
    return RefType.builder().type(RefType.OUTCOME).build();
  }
}
