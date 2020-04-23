package io.harness.references;

import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

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