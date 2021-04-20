package io.harness.state.inspection;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.Collection;
import java.util.List;

@OwnedBy(CDC)
public interface StateInspectionService {
  StateInspection get(String stateExecutionInstanceId);
  List<StateInspection> listUsingSecondary(Collection<String> stateExecutionInstanceIds);
  void append(String stateExecutionInstanceId, StateInspectionData data);
  void append(String stateExecutionInstanceId, List<StateInspectionData> data);
}
