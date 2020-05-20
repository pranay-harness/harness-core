package io.harness.state.inspection;

import java.util.Collection;
import java.util.List;

public interface StateInspectionService {
  StateInspection get(String stateExecutionInstanceId);
  List<StateInspection> listUsingSecondary(Collection<String> stateExecutionInstanceIds);
  void append(String stateExecutionInstanceId, StateInspectionData data);
  void append(String stateExecutionInstanceId, List<StateInspectionData> data);
}
