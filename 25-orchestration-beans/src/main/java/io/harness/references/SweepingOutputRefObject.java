package io.harness.references;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class SweepingOutputRefObject implements RefObject {
  @NonNull String name;
  @NonNull String producerId;

  @Override
  public RefType getRefType() {
    return RefType.builder().type(RefType.SWEEPING_OUTPUT).build();
  }
}