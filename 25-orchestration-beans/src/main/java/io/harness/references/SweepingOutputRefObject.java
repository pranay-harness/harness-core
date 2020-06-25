package io.harness.references;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder(buildMethodName = "internalBuild")
@Redesign
public class SweepingOutputRefObject implements RefObject {
  private static final String PRODUCER_ID = "__PRODUCER_ID__";

  @NonNull String name;
  @NonNull @Builder.Default String producerId = PRODUCER_ID;
  String key;

  @Override
  public RefType getRefType() {
    return RefType.builder().type(RefType.SWEEPING_OUTPUT).build();
  }

  public static class SweepingOutputRefObjectBuilder {
    public SweepingOutputRefObject build() {
      if (isEmpty(key)) {
        key = name;
      }
      return internalBuild();
    }
  }
}