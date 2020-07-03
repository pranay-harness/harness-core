package io.harness.references;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder(buildMethodName = "internalBuild")
public class OutcomeRefObject implements RefObject {
  String name;
  String producerId;
  String key;

  @Override
  public RefType getRefType() {
    return RefType.builder().type(RefType.OUTCOME).build();
  }

  public static class OutcomeRefObjectBuilder {
    public OutcomeRefObject build() {
      if (isEmpty(key)) {
        key = name;
      }
      return internalBuild();
    }
  }
}
