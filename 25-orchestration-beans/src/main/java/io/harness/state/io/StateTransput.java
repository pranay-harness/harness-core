package io.harness.state.io;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.references.RefType;

@OwnedBy(CDC)
@Redesign
public interface StateTransput {
  RefType getRefType();
}
