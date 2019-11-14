package software.wings.service.impl;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.INFRA_DEFINITION_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseExecutionData.PhaseExecutionDataBuilder;
import software.wings.api.SelectNodeStepExecutionSummary;
import software.wings.beans.ServiceInstance;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.sm.PhaseExecutionSummary;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.Builder;

import java.util.Collections;
import java.util.List;

public class StateExecutionServiceImplTest extends WingsBaseTest {
  private static final String RANDOM = "RANDOM";

  @Mock private FeatureFlagService featureFlagService;
  @Mock private AppService appService;

  private StateExecutionServiceImpl stateExecutionService = spy(new StateExecutionServiceImpl());

  @Before
  public void setUp() throws Exception {
    Reflect.on(stateExecutionService).set("featureFlagService", featureFlagService);
    Reflect.on(stateExecutionService).set("appService", appService);
  }

  @Test
  @Owner(emails = UNKNOWN)
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
    PhaseExecutionData phaseExecutionData = PhaseExecutionDataBuilder.aPhaseExecutionData()
                                                .withInfraDefinitionId(INFRA_DEFINITION_ID)
                                                .withPhaseExecutionSummary(phaseExecutionSummary)
                                                .build();
    StateExecutionInstance stateExecutionInstance = Builder.aStateExecutionInstance().appId(APP_ID).build();
    doReturn(Collections.singletonList(phaseExecutionData))
        .when(stateExecutionService)
        .fetchPhaseExecutionData(any(), any(), any(), any());
    doReturn(ACCOUNT_ID).when(appService).getAccountIdByAppId(any());
    doReturn(true).when(featureFlagService).isEnabled(any(), any());

    List<ServiceInstance> hostExclusionList =
        stateExecutionService.getHostExclusionList(stateExecutionInstance, phaseElement, null);

    assertThat(hostExclusionList).isEqualTo(serviceInstanceList);
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldReturnEmptyWhenNoPreviousPhases() {
    PhaseElement phaseElement = PhaseElement.builder().infraDefinitionId(INFRA_DEFINITION_ID).build();
    StateExecutionInstance stateExecutionInstance = Builder.aStateExecutionInstance().appId(APP_ID).build();
    doReturn(Collections.emptyList()).when(stateExecutionService).fetchPhaseExecutionData(any(), any(), any(), any());
    doReturn(ACCOUNT_ID).when(appService).getAccountIdByAppId(any());
    doReturn(true).when(featureFlagService).isEnabled(any(), any());

    List<ServiceInstance> hostExclusionList =
        stateExecutionService.getHostExclusionList(stateExecutionInstance, phaseElement, null);

    assertThat(hostExclusionList).isEqualTo(Collections.emptyList());
  }

  @Test
  @Owner(emails = UNKNOWN)
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
    PhaseExecutionData phaseExecutionData = PhaseExecutionDataBuilder.aPhaseExecutionData()
                                                .withInfraMappingId(INFRA_MAPPING_ID)
                                                .withPhaseExecutionSummary(phaseExecutionSummary)
                                                .build();
    StateExecutionInstance stateExecutionInstance = Builder.aStateExecutionInstance().appId(APP_ID).build();
    doReturn(Collections.singletonList(phaseExecutionData))
        .when(stateExecutionService)
        .fetchPhaseExecutionData(any(), any(), any(), any());
    doReturn(ACCOUNT_ID).when(appService).getAccountIdByAppId(any());
    doReturn(false).when(featureFlagService).isEnabled(any(), any());

    List<ServiceInstance> hostExclusionList =
        stateExecutionService.getHostExclusionList(stateExecutionInstance, phaseElement, null);

    assertThat(hostExclusionList).isEqualTo(serviceInstanceList);
  }
}