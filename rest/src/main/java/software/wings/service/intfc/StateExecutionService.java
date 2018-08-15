package software.wings.service.intfc;

import software.wings.api.PhaseElement;
import software.wings.beans.ServiceInstance;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;

import java.util.List;
import java.util.Map;

public interface StateExecutionService {
  Map<String, StateExecutionInstance> executionStatesMap(String appId, String executionUuid);

  List<String> phaseNames(String appId, String executionUuid);

  List<ServiceInstance> getHostExclusionList(StateExecutionInstance stateExecutionInstance, PhaseElement phaseElement);

  StateExecutionData phaseStateExecutionData(String appId, String executionUuid, String phaseName);
}
