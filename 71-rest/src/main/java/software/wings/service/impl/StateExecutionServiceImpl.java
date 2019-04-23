package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.sm.StateType.PHASE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HIterator;
import org.mongodb.morphia.query.Sort;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseExecutionData;
import software.wings.api.SelectNodeStepExecutionSummary;
import software.wings.beans.ServiceInstance;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.PhaseExecutionSummary;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;
import software.wings.sm.StateMachine;
import software.wings.sm.StateType;
import software.wings.sm.StepExecutionSummary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class StateExecutionServiceImpl implements StateExecutionService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowExecutionService workflowExecutionService;

  public Map<String, StateExecutionInstance> executionStatesMap(String appId, String executionUuid) {
    Map<String, StateExecutionInstance> allInstancesIdMap = new HashMap<>();

    try (HIterator<StateExecutionInstance> stateExecutionInstances =
             new HIterator<>(wingsPersistence.createQuery(StateExecutionInstance.class)
                                 .filter(StateExecutionInstanceKeys.appId, appId)
                                 .filter(StateExecutionInstanceKeys.executionUuid, executionUuid)
                                 .project(StateExecutionInstanceKeys.contextElement, true)
                                 .project(StateExecutionInstanceKeys.contextTransition, true)
                                 .project(StateExecutionInstanceKeys.dedicatedInterruptCount, true)
                                 .project(StateExecutionInstanceKeys.displayName, true)
                                 .project(StateExecutionInstanceKeys.executionType, true)
                                 .project(StateExecutionInstanceKeys.uuid, true)
                                 .project(StateExecutionInstanceKeys.interruptHistory, true)
                                 .project(StateExecutionInstanceKeys.lastUpdatedAt, true)
                                 .project(StateExecutionInstanceKeys.parentInstanceId, true)
                                 .project(StateExecutionInstanceKeys.prevInstanceId, true)
                                 .project(StateExecutionInstanceKeys.stateExecutionDataHistory, true)
                                 .project(StateExecutionInstanceKeys.stateExecutionMap, true)
                                 .project(StateExecutionInstanceKeys.stateName, true)
                                 .project(StateExecutionInstanceKeys.stateType, true)
                                 .project(StateExecutionInstanceKeys.status, true)
                                 .project(StateExecutionInstanceKeys.hasInspection, true)
                                 .fetch())) {
      while (stateExecutionInstances.hasNext()) {
        StateExecutionInstance stateExecutionInstance = stateExecutionInstances.next();
        stateExecutionInstance.getStateExecutionMap().entrySet().removeIf(
            entry -> !entry.getKey().equals(stateExecutionInstance.getDisplayName()));
        allInstancesIdMap.put(stateExecutionInstance.getUuid(), stateExecutionInstance);
      }
    }
    return allInstancesIdMap;
  }

  public List<String> phaseNames(String appId, String executionUuid) {
    List<String> names = new ArrayList<>();
    try (HIterator<StateExecutionInstance> stateExecutionInstances =
             new HIterator<>(wingsPersistence.createQuery(StateExecutionInstance.class)
                                 .filter(StateExecutionInstanceKeys.appId, appId)
                                 .filter(StateExecutionInstanceKeys.executionUuid, executionUuid)
                                 .filter(StateExecutionInstanceKeys.stateType, PHASE.name())
                                 .project(StateExecutionInstanceKeys.displayName, true)
                                 .fetch())) {
      while (stateExecutionInstances.hasNext()) {
        StateExecutionInstance stateExecutionInstance = stateExecutionInstances.next();
        names.add(stateExecutionInstance.getDisplayName());
      }
    }
    return names;
  }

  @Override
  public List<StateExecutionData> fetchPhaseExecutionData(
      String appId, String executionUuid, String phaseName, CurrentPhase curentPhase) {
    List<StateExecutionData> executionDataList = new ArrayList<>();
    try (HIterator<StateExecutionInstance> stateExecutionInstances =
             new HIterator<>(wingsPersistence.createQuery(StateExecutionInstance.class)
                                 .filter(StateExecutionInstanceKeys.appId, appId)
                                 .filter(StateExecutionInstanceKeys.executionUuid, executionUuid)
                                 .filter(StateExecutionInstanceKeys.stateType, StateType.PHASE.name())
                                 .order(Sort.ascending(StateExecutionInstanceKeys.createdAt))
                                 .project(StateExecutionInstanceKeys.displayName, true)
                                 .project(StateExecutionInstanceKeys.stateExecutionMap, true)
                                 .project(StateExecutionInstanceKeys.uuid, true)
                                 .fetch())) {
      while (stateExecutionInstances.hasNext()) {
        StateExecutionInstance stateExecutionInstance = stateExecutionInstances.next();
        StateExecutionData stateExecutionData = stateExecutionInstance.fetchStateExecutionData();
        if (CurrentPhase.EXCLUDE.equals(curentPhase) && stateExecutionInstance.getDisplayName().equals(phaseName)) {
          return executionDataList;
        }
        executionDataList.add(stateExecutionData);
        if (CurrentPhase.INCLUDE.equals(curentPhase) && stateExecutionInstance.getDisplayName().equals(phaseName)) {
          return executionDataList;
        }
      }
    }
    if (phaseName != null) {
      throw new InvalidRequestException("Phase Name [" + phaseName + " is missing from workflow execution]");
    }
    return executionDataList;
  }

  @Override
  public void updateStateExecutionData(String appId, String stateExecutionId, StateExecutionData stateExecutionData) {
    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.getWithAppId(StateExecutionInstance.class, appId, stateExecutionId);
    Map<String, StateExecutionData> stateExecutionMap = stateExecutionInstance.getStateExecutionMap();
    stateExecutionMap.put(stateExecutionInstance.getDisplayName(), stateExecutionData);
    wingsPersistence.save(stateExecutionInstance);
  }

  @Override
  public PageResponse<StateExecutionInstance> list(PageRequest<StateExecutionInstance> pageRequest) {
    return wingsPersistence.query(StateExecutionInstance.class, pageRequest);
  }

  @Override
  public List<ServiceInstance> getHostExclusionList(
      StateExecutionInstance stateExecutionInstance, PhaseElement phaseElement) {
    List<ServiceInstance> hostExclusionList = new ArrayList<>();

    List<StateExecutionData> previousPhaseExecutionsData =
        fetchPhaseExecutionData(stateExecutionInstance.getAppId(), stateExecutionInstance.getExecutionUuid(),
            phaseElement == null ? null : phaseElement.getPhaseName(), CurrentPhase.EXCLUDE);

    if (isEmpty(previousPhaseExecutionsData)) {
      return hostExclusionList;
    }

    for (StateExecutionData stateExecutionData : previousPhaseExecutionsData) {
      PhaseExecutionData phaseExecutionData = (PhaseExecutionData) stateExecutionData;
      if (phaseElement != null && phaseElement.getInfraMappingId() != null
          && !phaseExecutionData.getInfraMappingId().equals(phaseElement.getInfraMappingId())) {
        continue;
      }
      PhaseExecutionSummary phaseExecutionSummary = phaseExecutionData.getPhaseExecutionSummary();
      if (phaseExecutionSummary == null || phaseExecutionSummary.getPhaseStepExecutionSummaryMap() == null) {
        continue;
      }
      for (PhaseStepExecutionSummary phaseStepExecutionSummary :
          phaseExecutionSummary.getPhaseStepExecutionSummaryMap().values()) {
        if (phaseStepExecutionSummary == null || isEmpty(phaseStepExecutionSummary.getStepExecutionSummaryList())) {
          continue;
        }
        for (StepExecutionSummary stepExecutionSummary : phaseStepExecutionSummary.getStepExecutionSummaryList()) {
          if (stepExecutionSummary instanceof SelectNodeStepExecutionSummary) {
            SelectNodeStepExecutionSummary selectNodeStepExecutionSummary =
                (SelectNodeStepExecutionSummary) stepExecutionSummary;
            if (selectNodeStepExecutionSummary.isExcludeSelectedHostsFromFuturePhases()) {
              List<ServiceInstance> serviceInstanceList =
                  ((SelectNodeStepExecutionSummary) stepExecutionSummary).getServiceInstanceList();
              if (serviceInstanceList != null) {
                hostExclusionList.addAll(serviceInstanceList);
              }
            }
          }
        }
      }
    }
    return hostExclusionList;
  }

  public StateExecutionData phaseStateExecutionData(String appId, String executionUuid, String phaseName) {
    StateExecutionInstance stateExecutionInstance = wingsPersistence.createQuery(StateExecutionInstance.class)
                                                        .filter(StateExecutionInstanceKeys.appId, appId)
                                                        .filter(StateExecutionInstanceKeys.executionUuid, executionUuid)
                                                        .filter(StateExecutionInstanceKeys.displayName, phaseName)
                                                        .filter(StateExecutionInstanceKeys.stateType, PHASE.name())
                                                        .project(StateExecutionInstanceKeys.displayName, true)
                                                        .project(StateExecutionInstanceKeys.stateExecutionMap, true)
                                                        .get();
    return stateExecutionInstance.fetchStateExecutionData();
  }

  @Override
  public StateMachine obtainStateMachine(StateExecutionInstance stateExecutionInstance) {
    final WorkflowExecution workflowExecution = wingsPersistence.getWithAppId(
        WorkflowExecution.class, stateExecutionInstance.getAppId(), stateExecutionInstance.getExecutionUuid());

    return workflowExecutionService.obtainStateMachine(workflowExecution);
  }
}
