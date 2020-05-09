package io.harness.state.io;

import io.harness.annotations.Redesign;
import io.harness.references.RefType;

@Redesign
public interface StateTransput {
  RefType getRefType();
}
