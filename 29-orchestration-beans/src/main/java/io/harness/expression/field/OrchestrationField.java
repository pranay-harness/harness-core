package io.harness.expression.field;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface OrchestrationField<T> {
  OrchestrationFieldType getType();
  T getValue();
}
