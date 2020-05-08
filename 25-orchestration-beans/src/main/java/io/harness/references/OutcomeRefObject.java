package io.harness.references;

import lombok.Builder;
import lombok.Value;

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
