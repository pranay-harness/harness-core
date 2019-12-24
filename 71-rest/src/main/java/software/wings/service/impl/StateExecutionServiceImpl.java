package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.sm.StateType.PHASE;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.context.ContextElementType;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HIterator;
import org.jetbrains.annotations.Nullable;
import org.mongodb.morphia.query.Sort;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseExecutionData;
import software.wings.api.SelectNodeStepExecutionSummary;
import software.wings.beans.FeatureName;
import software.wings.beans.ServiceInstance;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.SweepingOutputService;
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
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class StateExecutionServiceImpl implements StateExecutionService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private AppService appService;
  @Inject private SweepingOutputService sweepingOutputService;

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
                                 .project(StateExecutionInstanceKeys.appId, true)
                                 .fetch())) {
      for (StateExecutionInstance stateExecutionInstance : stateExecutionInstances) {
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
      for (StateExecutionInstance stateExecutionInstance : stateExecutionInstances) {
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
      for (StateExecutionInstance stateExecutionInstance : stateExecutionInstances) {
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

  @VisibleForTesting
  List<StateExecutionInstance> fetchPreviousPhasesStateExecutionInstances(
      String appId, String executionUuid, String phaseName, CurrentPhase currentPhase) {
    List<StateExecutionInstance> instanceList = new ArrayList<>();
    try (HIterator<StateExecutionInstance> stateExecutionInstances =
             new HIterator<>(wingsPersistence.createQuery(StateExecutionInstance.class)
                                 .filter(StateExecutionInstanceKeys.appId, appId)
                                 .filter(StateExecutionInstanceKeys.executionUuid, executionUuid)
                                 .filter(StateExecutionInstanceKeys.stateType, StateType.PHASE.name())
                                 .order(Sort.ascending(StateExecutionInstanceKeys.createdAt))
                                 .fetch())) {
      for (StateExecutionInstance stateExecutionInstance : stateExecutionInstances) {
        if (CurrentPhase.EXCLUDE.equals(currentPhase) && stateExecutionInstance.getDisplayName().equals(phaseName)) {
          return instanceList;
        }
        instanceList.add(stateExecutionInstance);
        if (CurrentPhase.INCLUDE.equals(currentPhase) && stateExecutionInstance.getDisplayName().equals(phaseName)) {
          return instanceList;
        }
      }
    }

    return instanceList;
  }

  @Override
  public void updateStateExecutionData(String appId, String stateExecutionId, StateExecutionData stateExecutionData) {
    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.getWithAppId(StateExecutionInstance.class, appId, stateExecutionId);
    if (stateExecutionInstance == null) {
      return;
    }
    Map<String, StateExecutionData> stateExecutionMap = stateExecutionInstance.getStateExecutionMap();
    stateExecutionMap.put(stateExecutionInstance.getDisplayName(), stateExecutionData);
    wingsPersistence.save(stateExecutionInstance);
  }

  @Override
  public StateExecutionInstance getStateExecutionData(String appId, String stateExecutionId) {
    return wingsPersistence.getWithAppId(StateExecutionInstance.class, appId, stateExecutionId);
  }

  @Override
  public PageResponse<StateExecutionInstance> list(PageRequest<StateExecutionInstance> pageRequest) {
    return wingsPersistence.query(StateExecutionInstance.class, pageRequest);
  }

  @Override
  public List<ServiceInstance> getHostExclusionList(
      StateExecutionInstance stateExecutionInstance, PhaseElement phaseElement, String infraMappingId) {
    List<ServiceInstance> hostExclusionList = new ArrayList<>();

    List<StateExecutionInstance> instanceList = fetchPreviousPhasesStateExecutionInstances(
        stateExecutionInstance.getAppId(), stateExecutionInstance.getExecutionUuid(),
        phaseElement == null ? null : phaseElement.getPhaseName(), CurrentPhase.EXCLUDE);

    if (isEmpty(instanceList)) {
      return hostExclusionList;
    }

    processPreviousPhaseExecutionsData(
        stateExecutionInstance, phaseElement, infraMappingId, hostExclusionList, instanceList);
    return hostExclusionList;
  }

  private void processPreviousPhaseExecutionsData(StateExecutionInstance stateExecutionInstance,
      PhaseElement phaseElement, String infraMappingId, List<ServiceInstance> hostExclusionList,
      List<StateExecutionInstance> previousStateExecutionInstances) {
    for (StateExecutionInstance previousStateExecutionInstance : previousStateExecutionInstances) {
      PhaseExecutionData phaseExecutionData = getPhaseExecutionDataSweepingOutput(previousStateExecutionInstance);

      if (doesNotNeedProcessing(stateExecutionInstance, phaseElement, infraMappingId, phaseExecutionData)) {
        continue;
      }

      PhaseExecutionSummary phaseExecutionSummary =
          getPhaseExecutionSummarySweepingOutput(previousStateExecutionInstance);
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
  }

  @Override
  public PhaseExecutionSummary fetchPhaseExecutionSummarySweepingOutput(
      @NotNull StateExecutionInstance stateExecutionInstance) {
    return getPhaseExecutionSummarySweepingOutput(stateExecutionInstance);
  }

  @Override
  public PhaseExecutionData fetchPhaseExecutionDataSweepingOutput(StateExecutionInstance stateExecutionInstance) {
    return getPhaseExecutionDataSweepingOutput(stateExecutionInstance);
  }

  @VisibleForTesting
  PhaseExecutionSummary getPhaseExecutionSummarySweepingOutput(@NotNull StateExecutionInstance stateExecutionInstance) {
    return (PhaseExecutionSummary) sweepingOutputService.findSweepingOutput(
        SweepingOutputService.SweepingOutputInquiry.builder()
            .appId(stateExecutionInstance.getAppId())
            .name(PhaseExecutionSummary.SWEEPING_OUTPUT_NAME + stateExecutionInstance.getDisplayName())
            .workflowExecutionId(stateExecutionInstance.getExecutionUuid())
            .stateExecutionId(stateExecutionInstance.getUuid())
            .phaseExecutionId(getPhaseExecutionId(stateExecutionInstance))
            .build());
  }

  @VisibleForTesting
  PhaseExecutionData getPhaseExecutionDataSweepingOutput(@NotNull StateExecutionInstance stateExecutionInstance) {
    return (PhaseExecutionData) sweepingOutputService.findSweepingOutput(
        SweepingOutputService.SweepingOutputInquiry.builder()
            .appId(stateExecutionInstance.getAppId())
            .name(PhaseExecutionData.SWEEPING_OUTPUT_NAME + stateExecutionInstance.getDisplayName())
            .workflowExecutionId(stateExecutionInstance.getExecutionUuid())
            .stateExecutionId(stateExecutionInstance.getUuid())
            .phaseExecutionId(getPhaseExecutionId(stateExecutionInstance))
            .build());
  }

  @Nullable
  private String getPhaseExecutionId(@NotNull StateExecutionInstance stateExecutionInstance) {
    PhaseElement phaseElement = fetchPhaseElement(stateExecutionInstance);
    return phaseElement == null
        ? null
        : stateExecutionInstance.getExecutionUuid() + phaseElement.getUuid() + phaseElement.getPhaseName();
  }

  @Nullable
  private PhaseElement fetchPhaseElement(@NotNull StateExecutionInstance stateExecutionInstance) {
    PhaseElement phaseElement = (PhaseElement) stateExecutionInstance.getContextElements()
                                    .stream()
                                    .filter(ce
                                        -> ContextElementType.PARAM.equals(ce.getElementType())
                                            && PhaseElement.PHASE_PARAM.equals(ce.getName()))
                                    .findFirst()
                                    .orElse(null);
    if (phaseElement == null) {
      return null;
    }
    return phaseElement;
  }

  private boolean doesNotNeedProcessing(StateExecutionInstance stateExecutionInstance, PhaseElement phaseElement,
      String infraMappingId, PhaseExecutionData phaseExecutionData) {
    if (stateExecutionInstance.getDisplayName().equals(phaseElement == null ? null : phaseElement.getPhaseName())) {
      return true;
    }
    if (featureFlagService.isEnabled(
            FeatureName.INFRA_MAPPING_REFACTOR, appService.getAccountIdByAppId(stateExecutionInstance.getAppId()))) {
      return phaseElement != null && phaseElement.getInfraDefinitionId() != null
          && !phaseElement.getInfraDefinitionId().equals(phaseExecutionData.getInfraDefinitionId());
    } else {
      return infraMappingId != null && !phaseExecutionData.getInfraMappingId().equals(infraMappingId);
    }
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

  @Override
  public StateExecutionInstance fetchPreviousPhaseStateExecutionInstance(
      String appId, String executionUuid, String currentStateExecutionId) {
    StateExecutionInstance stateExecutionInstance =
        getStateExecutionInstance(appId, executionUuid, currentStateExecutionId);

    if (stateExecutionInstance == null) {
      return null;
    }

    if (stateExecutionInstance.getStateType().equals(PHASE.name())) {
      StateExecutionInstance previousPhaseStateExecutionInstance =
          getStateExecutionInstance(appId, executionUuid, stateExecutionInstance.getPrevInstanceId());
      if (previousPhaseStateExecutionInstance != null
          && previousPhaseStateExecutionInstance.getStateType().equals(PHASE.name())) {
        return previousPhaseStateExecutionInstance;
      }
    } else {
      return fetchPreviousPhaseStateExecutionInstance(
          appId, executionUuid, stateExecutionInstance.getParentInstanceId());
    }
    return null;
  }

  @Override
  public StateExecutionInstance fetchCurrentPhaseStateExecutionInstance(
      String appId, String executionUuid, String currentStateExecutionId) {
    StateExecutionInstance stateExecutionInstance =
        getStateExecutionInstance(appId, executionUuid, currentStateExecutionId);

    if (stateExecutionInstance == null) {
      return null;
    }
    if (stateExecutionInstance.getStateType().equals(PHASE.name())) {
      return stateExecutionInstance;
    } else {
      return fetchCurrentPhaseStateExecutionInstance(
          appId, executionUuid, stateExecutionInstance.getParentInstanceId());
    }
  }

  @Override
  public StateExecutionInstance getStateExecutionInstance(String appId, String executionUuid, String instanceId) {
    return wingsPersistence.createQuery(StateExecutionInstance.class)
        .filter(StateExecutionInstanceKeys.appId, appId)
        .filter(StateExecutionInstanceKeys.executionUuid, executionUuid)
        .filter(StateExecutionInstanceKeys.uuid, instanceId)
        .get();
  }
}
