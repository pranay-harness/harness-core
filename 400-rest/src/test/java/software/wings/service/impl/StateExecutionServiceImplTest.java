/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.AGORODETKI;
import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.PRASHANT;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static software.wings.sm.StateExecutionData.StateExecutionDataBuilder;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.INFRA_DEFINITION_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskDetails;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseExecutionData.PhaseExecutionDataBuilder;
import software.wings.api.SelectNodeStepExecutionSummary;
import software.wings.beans.ServiceInstance;
import software.wings.service.intfc.sweepingoutput.SweepingOutputInquiry;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionInterruptEffect;
import software.wings.sm.PhaseExecutionSummary;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.Builder;
import software.wings.sm.StateType;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.CDC)
public class StateExecutionServiceImplTest extends WingsBaseTest {
  private static final String RANDOM = "RANDOM";

  @Mock private SweepingOutputService sweepingOutputService;
  @Inject private HPersistence persistence;

  @InjectMocks private StateExecutionServiceImpl stateExecutionService = spy(new StateExecutionServiceImpl());

  @Before
  public void setUp() throws Exception {
    Reflect.on(stateExecutionService).set("sweepingOutputService", sweepingOutputService);
    Reflect.on(stateExecutionService).set("wingsPersistence", persistence);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldReturnHostsForMultiplePhases() {
    PhaseElement phaseElement = PhaseElement.builder().infraDefinitionId(INFRA_DEFINITION_ID).build();
    List<ServiceInstance> serviceInstanceList =
        Collections.singletonList(ServiceInstance.Builder.aServiceInstance().build());
    SelectNodeStepExecutionSummary selectNodeStepExecutionSummary = new SelectNodeStepExecutionSummary();
    selectNodeStepExecutionSummary.setExcludeSelectedHostsFromFuturePhases(true);
    selectNodeStepExecutionSummary.setServiceInstanceList(serviceInstanceList);
    PhaseStepExecutionSummary phaseStepExecutionSummary = new PhaseStepExecutionSummary();

    phaseStepExecutionSummary.setStepExecutionSummaryList(Collections.singletonList(selectNodeStepExecutionSummary));
    PhaseExecutionSummary phaseExecutionSummary = new PhaseExecutionSummary();
    phaseExecutionSummary.setPhaseStepExecutionSummaryMap(Collections.singletonMap(RANDOM, phaseStepExecutionSummary));
    doReturn(phaseExecutionSummary).when(stateExecutionService).getPhaseExecutionSummarySweepingOutput(any());
    PhaseExecutionData phaseExecutionData =
        PhaseExecutionDataBuilder.aPhaseExecutionData().withInfraDefinitionId(INFRA_DEFINITION_ID).build();
    doReturn(phaseExecutionData).when(stateExecutionService).getPhaseExecutionDataSweepingOutput(any());
    StateExecutionInstance stateExecutionInstance =
        aStateExecutionInstance().displayName("Phase 2").appId(APP_ID).build();
    StateExecutionInstance previousStateExecutionInstance = aStateExecutionInstance()
                                                                .appId(APP_ID)
                                                                .displayName("Phase 1")
                                                                .addStateExecutionData("Phase 1", phaseExecutionData)
                                                                .build();
    doReturn(Collections.singletonList(previousStateExecutionInstance))
        .when(stateExecutionService)
        .fetchPreviousPhasesStateExecutionInstances(any(), any(), any(), any());

    List<ServiceInstance> hostExclusionList =
        stateExecutionService.getHostExclusionList(stateExecutionInstance, phaseElement, null);

    assertThat(hostExclusionList).isEqualTo(serviceInstanceList);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldReturnEmptyWhenNoPreviousPhases() {
    PhaseElement phaseElement = PhaseElement.builder().infraDefinitionId(INFRA_DEFINITION_ID).build();
    StateExecutionInstance stateExecutionInstance = aStateExecutionInstance().appId(APP_ID).build();
    doReturn(Collections.emptyList())
        .when(stateExecutionService)
        .fetchPreviousPhasesStateExecutionInstances(any(), any(), any(), any());

    List<ServiceInstance> hostExclusionList =
        stateExecutionService.getHostExclusionList(stateExecutionInstance, phaseElement, null);

    assertThat(hostExclusionList).isEqualTo(Collections.emptyList());
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldReturnHostsForMultiplePhasesForFeatureFlagOff() {
    PhaseElement phaseElement = PhaseElement.builder().infraMappingId(INFRA_MAPPING_ID).build();
    List<ServiceInstance> serviceInstanceList =
        Collections.singletonList(ServiceInstance.Builder.aServiceInstance().build());
    SelectNodeStepExecutionSummary selectNodeStepExecutionSummary = new SelectNodeStepExecutionSummary();
    selectNodeStepExecutionSummary.setExcludeSelectedHostsFromFuturePhases(true);
    selectNodeStepExecutionSummary.setServiceInstanceList(serviceInstanceList);
    PhaseStepExecutionSummary phaseStepExecutionSummary = new PhaseStepExecutionSummary();
    phaseStepExecutionSummary.setStepExecutionSummaryList(Collections.singletonList(selectNodeStepExecutionSummary));
    PhaseExecutionSummary phaseExecutionSummary = new PhaseExecutionSummary();
    phaseExecutionSummary.setPhaseStepExecutionSummaryMap(Collections.singletonMap(RANDOM, phaseStepExecutionSummary));
    doReturn(phaseExecutionSummary).when(stateExecutionService).getPhaseExecutionSummarySweepingOutput(any());
    PhaseExecutionData phaseExecutionData =
        PhaseExecutionDataBuilder.aPhaseExecutionData().withInfraMappingId(INFRA_MAPPING_ID).build();
    StateExecutionInstance stateExecutionInstance =
        aStateExecutionInstance().displayName("Phase 2").appId(APP_ID).build();
    StateExecutionInstance previousStateExecutionInstance = aStateExecutionInstance()
                                                                .appId(APP_ID)
                                                                .displayName("Phase 1")
                                                                .addStateExecutionData("Phase 1", phaseExecutionData)
                                                                .build();
    doReturn(Collections.singletonList(previousStateExecutionInstance))
        .when(stateExecutionService)
        .fetchPreviousPhasesStateExecutionInstances(any(), any(), any(), any());

    List<ServiceInstance> hostExclusionList =
        stateExecutionService.getHostExclusionList(stateExecutionInstance, phaseElement, null);

    assertThat(hostExclusionList).isEqualTo(serviceInstanceList);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void fetchPreviousPhaseStateExecutionInstance() {
    String uuid1 = generateUuid();
    String uuid2 = generateUuid();
    String uuid3 = generateUuid();
    String uuid4 = generateUuid();
    setupStateExecutionInstanceData(uuid1, uuid2, uuid3, uuid4);
    StateExecutionInstance previousInstance =
        stateExecutionService.fetchPreviousPhaseStateExecutionInstance(APP_ID, WORKFLOW_EXECUTION_ID, uuid1);
    assertThat(previousInstance).isNotNull();
    assertThat(previousInstance.getUuid()).isEqualTo(uuid4);
    assertThat(previousInstance.getStateName()).isEqualTo("Phase 1");
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void fetchCurrentPhaseStateExecutionInstance() {
    String uuid1 = generateUuid();
    String uuid2 = generateUuid();
    String uuid3 = generateUuid();
    String uuid4 = generateUuid();
    setupStateExecutionInstanceData(uuid1, uuid2, uuid3, uuid4);
    StateExecutionInstance previousInstance =
        stateExecutionService.fetchCurrentPhaseStateExecutionInstance(APP_ID, WORKFLOW_EXECUTION_ID, uuid1);
    assertThat(previousInstance).isNotNull();
    assertThat(previousInstance.getUuid()).isEqualTo(uuid3);
    assertThat(previousInstance.getStateName()).isEqualTo("Phase 2");
  }

  private void setupStateExecutionInstanceData(String uuid1, String uuid2, String uuid3, String uuid4) {
    Builder instance = aStateExecutionInstance().appId(APP_ID).executionUuid(WORKFLOW_EXECUTION_ID);

    StateExecutionInstance executionInstance1 = instance.uuid(uuid1)
                                                    .displayName("App Resize")
                                                    .stateName("App Resize")
                                                    .stateType(StateType.PCF_RESIZE.name())
                                                    .parentInstanceId(uuid2)
                                                    .build();
    StateExecutionInstance executionInstance2 = instance.uuid(uuid2)
                                                    .displayName("Deploy")
                                                    .stateName("Deploy")
                                                    .stateType(StateType.PHASE_STEP.name())
                                                    .parentInstanceId(uuid3)
                                                    .build();

    StateExecutionInstance executionInstance3 = instance.uuid(uuid3)
                                                    .displayName("Phase 2")
                                                    .stateName("Phase 2")
                                                    .stateType(StateType.PHASE.name())
                                                    .prevInstanceId(uuid4)
                                                    .build();

    StateExecutionInstance executionInstance4 =
        instance.uuid(uuid4).displayName("Phase 1").stateName("Phase 1").stateType(StateType.PHASE.name()).build();

    doReturn(executionInstance1)
        .when(stateExecutionService)
        .getStateExecutionInstance(APP_ID, WORKFLOW_EXECUTION_ID, uuid1);
    doReturn(executionInstance2)
        .when(stateExecutionService)
        .getStateExecutionInstance(APP_ID, WORKFLOW_EXECUTION_ID, uuid2);
    doReturn(executionInstance3)
        .when(stateExecutionService)
        .getStateExecutionInstance(APP_ID, WORKFLOW_EXECUTION_ID, uuid3);
    doReturn(executionInstance4)
        .when(stateExecutionService)
        .getStateExecutionInstance(APP_ID, WORKFLOW_EXECUTION_ID, uuid4);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchPhaseExecutionSummarySweepingOutput() {
    String workflowExecutionId = generateUuid();
    String stateExecutionId = generateUuid();
    String phaseExecutionId = generateUuid();
    String phaseName = "name";
    PhaseExecutionSummary phaseExecutionSummary = new PhaseExecutionSummary();
    PhaseElement phaseElement =
        PhaseElement.builder().uuid(phaseExecutionId).phaseName(phaseName).infraMappingId(INFRA_MAPPING_ID).build();
    StateExecutionInstance stateExecutionInstance = aStateExecutionInstance()
                                                        .uuid(stateExecutionId)
                                                        .displayName(phaseName)
                                                        .appId(APP_ID)
                                                        .executionUuid(workflowExecutionId)
                                                        .addContextElement(phaseElement)
                                                        .build();

    String phaseExecutionIdSweepingOutput = workflowExecutionId + phaseExecutionId + phaseName;

    doReturn(phaseExecutionSummary)
        .when(sweepingOutputService)
        .findSweepingOutput(SweepingOutputInquiry.builder()
                                .appId(APP_ID)
                                .name(PhaseExecutionSummary.SWEEPING_OUTPUT_NAME + phaseName)
                                .workflowExecutionId(workflowExecutionId)
                                .phaseExecutionId(phaseExecutionIdSweepingOutput)
                                .stateExecutionId(stateExecutionId)
                                .build());

    PhaseExecutionSummary savedPhaseExecutionSummary =
        stateExecutionService.fetchPhaseExecutionSummarySweepingOutput(stateExecutionInstance);
    assertThat(savedPhaseExecutionSummary).isEqualTo(phaseExecutionSummary);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldNotFetchExecutionStatesMap() {
    Map<String, StateExecutionInstance> executionStateMap =
        stateExecutionService.executionStatesMap(generateUuid(), generateUuid());
    assertThat(executionStateMap).isEmpty();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldFetchExecutionStatesMap() {
    StateExecutionInstance stateExecutionInstance = createStateExecutionInstance();
    persistence.save(stateExecutionInstance);
    Map<String, StateExecutionInstance> executionStateMap = stateExecutionService.executionStatesMap(
        stateExecutionInstance.getAppId(), stateExecutionInstance.getExecutionUuid());
    assertThat(executionStateMap).isNotEmpty();

    StateExecutionInstance dbStateExecutionInstance = executionStateMap.get(stateExecutionInstance.getUuid());
    assertThat(dbStateExecutionInstance).isNotNull();
    assertThat(dbStateExecutionInstance.getAccountId()).isEqualTo(stateExecutionInstance.getAccountId());
    assertThat(dbStateExecutionInstance.getContextElement()).isEqualTo(stateExecutionInstance.getContextElement());
    assertThat(dbStateExecutionInstance.isContextTransition()).isEqualTo(stateExecutionInstance.isContextTransition());
    assertThat(dbStateExecutionInstance.getDedicatedInterruptCount())
        .isEqualTo(stateExecutionInstance.getDedicatedInterruptCount());
    assertThat(dbStateExecutionInstance.getDisplayName()).isEqualTo(stateExecutionInstance.getDisplayName());
    assertThat(dbStateExecutionInstance.getExecutionType()).isEqualTo(stateExecutionInstance.getExecutionType());
    assertThat(dbStateExecutionInstance.getExecutionType()).isEqualTo(stateExecutionInstance.getExecutionType());
    assertThat(dbStateExecutionInstance.getUuid()).isEqualTo(stateExecutionInstance.getUuid());
    assertThat(dbStateExecutionInstance.getInterruptHistory()).isEqualTo(stateExecutionInstance.getInterruptHistory());
    assertThat(dbStateExecutionInstance.getLastUpdatedAt()).isEqualTo(stateExecutionInstance.getLastUpdatedAt());
    assertThat(dbStateExecutionInstance.getParentInstanceId()).isEqualTo(stateExecutionInstance.getParentInstanceId());
    assertThat(dbStateExecutionInstance.getPrevInstanceId()).isEqualTo(stateExecutionInstance.getPrevInstanceId());
    assertThat(dbStateExecutionInstance.getStateExecutionDataHistory().get(0).getStateName())
        .isEqualTo(stateExecutionInstance.getStateExecutionDataHistory().get(0).getStateName());
    assertThat(dbStateExecutionInstance.getStateExecutionMap().size()).isEqualTo(0);
    assertThat(dbStateExecutionInstance.getStateName()).isEqualTo(stateExecutionInstance.getStateName());
    assertThat(dbStateExecutionInstance.getStateType()).isEqualTo(stateExecutionInstance.getStateType());
    assertThat(dbStateExecutionInstance.getStatus()).isEqualTo(stateExecutionInstance.getStatus());
    assertThat(dbStateExecutionInstance.getStatus()).isEqualTo(stateExecutionInstance.getStatus());
    assertThat(dbStateExecutionInstance.isHasInspection()).isEqualTo(stateExecutionInstance.isHasInspection());
    assertThat(dbStateExecutionInstance.getAppId()).isEqualTo(stateExecutionInstance.getAppId());
    assertThat(dbStateExecutionInstance.isSelectionLogsTrackingForTasksEnabled())
        .isEqualTo(stateExecutionInstance.isSelectionLogsTrackingForTasksEnabled());
    assertThat(dbStateExecutionInstance.getDelegateTasksDetails())
        .isEqualTo(stateExecutionInstance.getDelegateTasksDetails());
    assertThat(dbStateExecutionInstance.getDelegateTaskId()).isEqualTo(stateExecutionInstance.getDelegateTaskId());
  }

  private StateExecutionInstance createStateExecutionInstance() {
    StateExecutionData stateExecutionData =
        StateExecutionDataBuilder.aStateExecutionData().withStateName("stateName").build();
    Map<String, StateExecutionData> stateExecutionDataMap = new HashMap<>();
    stateExecutionDataMap.put(stateExecutionData.getStateName(), stateExecutionData);

    StateExecutionInstance stateExecutionInstance =
        StateExecutionInstance.Builder.aStateExecutionInstance()
            .accountId(ACCOUNT_ID)
            .executionUuid(generateUuid())
            .contextElement(InstanceElement.Builder.anInstanceElement().build())
            .contextTransition(false)
            .displayName("displayName")
            .executionType(WorkflowType.ORCHESTRATION)
            .uuid(generateUuid())
            .lastUpdatedAt(System.currentTimeMillis())
            .parentInstanceId(generateUuid())
            .prevInstanceId(generateUuid())
            .stateExecutionDataHistory(Arrays.asList(stateExecutionData))
            .stateExecutionMap(stateExecutionDataMap)
            .stateName("stateName")
            .stateType("stateType")
            .status(ExecutionStatus.SUCCESS)
            .appId("appId")
            .build();

    stateExecutionInstance.setDedicatedInterruptCount(1);
    stateExecutionInstance.setInterruptHistory(Arrays.asList(ExecutionInterruptEffect.builder().build()));
    stateExecutionInstance.setHasInspection(false);
    stateExecutionInstance.setDelegateTaskId(generateUuid());
    stateExecutionInstance.setSelectionLogsTrackingForTasksEnabled(true);

    Map<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put("key1", "value1");
    setupAbstractions.put("key2", "value2");

    List<DelegateTaskDetails> delegateTaskDetailsList = new ArrayList<>();
    delegateTaskDetailsList.add(DelegateTaskDetails.builder()
                                    .delegateTaskId(generateUuid())
                                    .taskDescription("description")
                                    .setupAbstractions(setupAbstractions)
                                    .taskType("type")
                                    .build());
    stateExecutionInstance.setDelegateTasksDetails(delegateTaskDetailsList);

    return stateExecutionInstance;
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testListByIdsUsingSecondary() {
    String id = persistence.save(createStateExecutionInstance());

    List<StateExecutionInstance> stateExecutionInstances =
        stateExecutionService.listByIdsUsingSecondary(Collections.emptyList());
    assertThat(stateExecutionInstances).isNotNull();
    assertThat(stateExecutionInstances).isEmpty();

    stateExecutionInstances = stateExecutionService.listByIdsUsingSecondary(Collections.singletonList(id));
    assertThat(stateExecutionInstances).isNotNull();
    assertThat(stateExecutionInstances.size()).isEqualTo(1);
    assertThat(stateExecutionInstances.get(0).getUuid()).isEqualTo(id);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testAppendDelegateTaskDetails() {
    StateExecutionInstance instance = createStateExecutionInstance();
    String id = persistence.save(instance);

    Map<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put("key3", "value3");
    setupAbstractions.put("key4", "value4");

    stateExecutionService.appendDelegateTaskDetails(id,
        DelegateTaskDetails.builder()
            .delegateTaskId(generateUuid())
            .taskDescription("description")
            .setupAbstractions(setupAbstractions)
            .taskType("type")
            .build());

    instance = stateExecutionService.getStateExecutionInstance(instance.getAppId(), instance.getExecutionUuid(), id);

    assertThat(instance.getDelegateTasksDetails()).hasSize(2);
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void fetchCurrentPhaseStepStateExecutionInstance() {
    String uuid1 = generateUuid();
    String uuid2 = generateUuid();
    String uuid3 = generateUuid();
    String uuid4 = generateUuid();
    setupStateExecutionInstanceData(uuid1, uuid2, uuid3, uuid4);
    StateExecutionInstance phaseStepState =
        stateExecutionService.fetchCurrentPhaseStepStateExecutionInstance(APP_ID, WORKFLOW_EXECUTION_ID, uuid1);
    assertThat(phaseStepState).isNotNull();
    assertThat(phaseStepState.getUuid()).isEqualTo(uuid2);
    assertThat(phaseStepState.getStateName()).isEqualTo("Deploy");
  }
}
