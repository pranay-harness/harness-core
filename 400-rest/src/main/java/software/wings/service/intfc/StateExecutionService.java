/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.beans.DelegateTaskDetails;

import software.wings.api.PhaseElement;
import software.wings.api.PhaseExecutionData;
import software.wings.beans.ServiceInstance;
import software.wings.sm.PhaseExecutionSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public interface StateExecutionService {
  Map<String, StateExecutionInstance> executionStatesMap(String appId, String executionUuid);

  List<String> phaseNames(String appId, String executionUuid);

  int getRollingPhaseCount(String appId, String executionUuid);

  enum CurrentPhase { INCLUDE, EXCLUDE }

  List<StateExecutionData> fetchPhaseExecutionData(
      String appId, String executionUuid, String phaseName, CurrentPhase currentPhase);

  void updateStateExecutionData(String appId, String stateExecutionId, StateExecutionData stateExecutionData);

  void updateStateExecutionInstance(@NotNull StateExecutionInstance stateExecutionInstance);

  StateExecutionInstance getStateExecutionData(String appId, String stateExecutionId);

  PageResponse<StateExecutionInstance> list(PageRequest<StateExecutionInstance> pageRequest);

  List<StateExecutionInstance> listByIdsUsingSecondary(Collection<String> stateExecutionInstanceIds);

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

  StateExecutionInstance fetchCurrentPhaseStepStateExecutionInstance(
      String appId, String executionUuid, String currentStateExecutionId);

  StateExecutionInstance getStateExecutionInstance(String appId, String executionUuid, String currentStateExecutionId);

  void appendDelegateTaskDetails(String instanceId, DelegateTaskDetails delegateTaskDetails);
}
