package io.harness.state.inspection;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface StateInspectionListener {
  void appendedDataFor(String stateExecutionInstanceId);
}
