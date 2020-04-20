package io.harness.references;

import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
@Redesign
public class SweepingOutputRefObject implements RefObject {
  @NotNull String name;
  @NotNull String producerId;

  @Override
  public RefType getRefType() {
    return RefType.builder().type(RefType.SWEEPING_OUTPUT).build();
  }
}