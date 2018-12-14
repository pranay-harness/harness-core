package io.harness.state.inspection;

import java.util.List;

public interface StateInspectionService {
  StateInspection get(String stateExecutionInstanceId);
  void append(String stateExecutionInstanceId, StateInspectionData data);
  void append(String stateExecutionInstanceId, List<StateInspectionData> data);
}
