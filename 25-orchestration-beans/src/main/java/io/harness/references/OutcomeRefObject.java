package io.harness.references;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
public class OutcomeRefObject implements RefObject {
  String name;
  String producerId;

  @Builder
  private OutcomeRefObject(String name, String producerId) {
    this.name = name;
    this.producerId = producerId;
  }

  @Override
  public RefType getRefType() {
    return RefType.builder().type("OUTCOME").build();
  }
}
