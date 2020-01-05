package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseExecutionData;
import software.wings.beans.ServiceInstance;
import software.wings.sm.PhaseExecutionSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;

import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

public interface StateExecutionService {
  Map<String, StateExecutionInstance> executionStatesMap(String appId, String executionUuid);

  List<String> phaseNames(String appId, String executionUuid);

  int getRollingPhaseCount(String appId, String executionUuid);

  enum CurrentPhase { INCLUDE, EXCLUDE }

  List<StateExecutionData> fetchPhaseExecutionData(
      String appId, String executionUuid, String phaseName, CurrentPhase currentPhase);

  void updateStateExecutionData(String appId, String stateExecutionId, StateExecutionData stateExecutionData);

  StateExecutionInstance getStateExecutionData(String appId, String stateExecutionId);

  PageResponse<StateExecutionInstance> list(PageRequest<StateExecutionInstance> pageRequest);

  List<ServiceInstance> getHostExclusionList(
      StateExecutionInstance stateExecutionInstance, PhaseElement phaseElement, String infraMappingId);

  StateExecutionData phaseStateExecutionData(String appId, String executionUuid, String phaseName);

  PhaseExecutionData fetchPhaseExecutionDataSweepingOutput(@NotNull StateExecutionInstance stateExecutionInstance);

  PhaseExecutionSummary fetchPhaseExecutionSummarySweepingOutput(
      @NotNull StateExecutionInstance stateExecutionInstance);

  StateMachine obtainStateMachine(StateExecutionInstance stateExecutionInstance);

  StateExecutionInstance fetchPreviousPhaseStateExecutionInstance(
      String appId, String executionUuid, String currentStateExecutionId);

  StateExecutionInstance fetchCurrentPhaseStateExecutionInstance(
      String appId, String executionUuid, String currentStateExecutionId);

  StateExecutionInstance getStateExecutionInstance(String appId, String executionUuid, String currentStateExecutionId);
}
