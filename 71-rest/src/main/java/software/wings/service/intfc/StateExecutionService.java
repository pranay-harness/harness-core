package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.api.PhaseElement;
import software.wings.beans.ServiceInstance;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;

import java.util.List;
import java.util.Map;

public interface StateExecutionService {
  Map<String, StateExecutionInstance> executionStatesMap(String appId, String executionUuid);

  List<String> phaseNames(String appId, String executionUuid);

  enum CurrentPhase { INCLUDE, EXCLUDE }

  List<StateExecutionData> fetchPhaseExecutionData(
      String appId, String executionUuid, String phaseName, CurrentPhase currentPhase);

  void updateStateExecutionData(String appId, String stateExecutionId, StateExecutionData stateExecutionData);

  PageResponse<StateExecutionInstance> list(PageRequest<StateExecutionInstance> pageRequest);

  List<ServiceInstance> getHostExclusionList(StateExecutionInstance stateExecutionInstance, PhaseElement phaseElement);

  StateExecutionData phaseStateExecutionData(String appId, String executionUuid, String phaseName);

  StateMachine obtainStateMachine(StateExecutionInstance stateExecutionInstance);
}
