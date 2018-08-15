package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.sm.StateType.PHASE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseExecutionData;
import software.wings.api.SelectNodeStepExecutionSummary;
import software.wings.beans.ServiceInstance;
import software.wings.dl.HIterator;
import software.wings.dl.WingsPersistence;
import software.wings.exception.InvalidRequestException;
import software.wings.service.intfc.StateExecutionService;
import software.wings.sm.PhaseExecutionSummary;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
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
  private static final Logger logger = LoggerFactory.getLogger(StateExecutionServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;

  public Map<String, StateExecutionInstance> executionStatesMap(String appId, String executionUuid) {
    Map<String, StateExecutionInstance> allInstancesIdMap = new HashMap<>();

    try (HIterator<StateExecutionInstance> stateExecutionInstances =
             new HIterator<>(wingsPersistence.createQuery(StateExecutionInstance.class)
                                 .filter(StateExecutionInstance.APP_ID_KEY, appId)
                                 .filter(StateExecutionInstance.EXECUTION_UUID_KEY, executionUuid)
                                 .project(StateExecutionInstance.CONTEXT_ELEMENT_KEY, true)
                                 .project(StateExecutionInstance.CONTEXT_TRANSITION_KEY, true)
                                 .project(StateExecutionInstance.DEDICATED_INTERRUPT_COUNT_KEY, true)
                                 .project(StateExecutionInstance.DISPLAY_NAME_KEY, true)
                                 .project(StateExecutionInstance.EXECUTION_TYPE_KEY, true)
                                 .project(StateExecutionInstance.ID_KEY, true)
                                 .project(StateExecutionInstance.INTERRUPT_HISTORY_KEY, true)
                                 .project(StateExecutionInstance.LAST_UPDATED_AT_KEY, true)
                                 .project(StateExecutionInstance.PARENT_INSTANCE_ID_KEY, true)
                                 .project(StateExecutionInstance.PREV_INSTANCE_ID_KEY, true)
                                 .project(StateExecutionInstance.STATE_EXECUTION_DATA_HISTORY_KEY, true)
                                 .project(StateExecutionInstance.STATE_EXECUTION_MAP_KEY, true)
                                 .project(StateExecutionInstance.STATE_NAME_KEY, true)
                                 .project(StateExecutionInstance.STATE_TYPE_KEY, true)
                                 .project(StateExecutionInstance.STATUS_KEY, true)
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
                                 .filter(StateExecutionInstance.APP_ID_KEY, appId)
                                 .filter(StateExecutionInstance.EXECUTION_UUID_KEY, executionUuid)
                                 .filter(StateExecutionInstance.STATE_TYPE_KEY, PHASE.name())
                                 .project(StateExecutionInstance.DISPLAY_NAME_KEY, true)
                                 .fetch())) {
      while (stateExecutionInstances.hasNext()) {
        StateExecutionInstance stateExecutionInstance = stateExecutionInstances.next();
        names.add(stateExecutionInstance.getDisplayName());
      }
    }
    return names;
  }

  private List<StateExecutionData> fetchPreviousPhaseExecutionData(
      String appId, String executionUuid, String phaseName) {
    List<StateExecutionData> previousExecutionDataList = new ArrayList<>();
    try (HIterator<StateExecutionInstance> stateExecutionInstances =
             new HIterator<>(wingsPersistence.createQuery(StateExecutionInstance.class)
                                 .filter(StateExecutionInstance.APP_ID_KEY, appId)
                                 .filter(StateExecutionInstance.EXECUTION_UUID_KEY, executionUuid)
                                 .filter(StateExecutionInstance.STATE_TYPE_KEY, StateType.PHASE.name())
                                 .order(Sort.ascending(StateExecutionInstance.CREATED_AT_KEY))
                                 .project(StateExecutionInstance.DISPLAY_NAME_KEY, true)
                                 .project(StateExecutionInstance.STATE_EXECUTION_MAP_KEY, true)
                                 .project(StateExecutionInstance.ID_KEY, true)
                                 .fetch())) {
      while (stateExecutionInstances.hasNext()) {
        StateExecutionInstance stateExecutionInstance = stateExecutionInstances.next();
        StateExecutionData stateExecutionData = stateExecutionInstance.getStateExecutionData();
        if (stateExecutionInstance.getDisplayName().equals(phaseName)) {
          return previousExecutionDataList;
        }
        previousExecutionDataList.add(stateExecutionData);
      }
    }
    if (phaseName != null) {
      throw new InvalidRequestException("Phase Name [" + phaseName + " is missing from workflow execution]");
    }
    return previousExecutionDataList;
  }

  @Override
  public List<ServiceInstance> getHostExclusionList(
      StateExecutionInstance stateExecutionInstance, PhaseElement phaseElement) {
    List<ServiceInstance> hostExclusionList = new ArrayList<>();

    List<StateExecutionData> previousPhaseExecutionsData =
        fetchPreviousPhaseExecutionData(stateExecutionInstance.getAppId(), stateExecutionInstance.getExecutionUuid(),
            phaseElement == null ? null : phaseElement.getPhaseName());

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
}
