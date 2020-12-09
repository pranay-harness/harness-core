package io.harness.pms.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface OrchestrationField {
  OrchestrationFieldType getType();
  Object getFinalValue();
}
