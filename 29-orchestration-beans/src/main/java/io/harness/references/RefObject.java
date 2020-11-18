package io.harness.references;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.refobjects.RefType;

@OwnedBy(CDC)
@Redesign
public interface RefObject {
  String getName();
  String getProducerId();
  RefType getRefType();
  String getKey();
}
