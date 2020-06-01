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
  private static final String PRODUCER_ID = "__PRODUCER_ID__";

  @NonNull String name;
  @NonNull @Builder.Default String producerId = PRODUCER_ID;

  @Override
  public RefType getRefType() {
    return RefType.builder().type(RefType.SWEEPING_OUTPUT).build();
  }
}