package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import io.harness.category.element.UnitTests;
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
import software.wings.utils.WingsTestConstants;

import java.util.Collections;
import java.util.List;

public class StateExecutionServiceImplTest extends WingsBaseTest {
  private static final String INFRA_DEFINITION_ID = "INFRA_DEFINITION_ID";
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
    StateExecutionInstance stateExecutionInstance =
        Builder.aStateExecutionInstance().appId(WingsTestConstants.APP_ID).build();
    doReturn(Collections.singletonList(phaseExecutionData))
        .when(stateExecutionService)
        .fetchPhaseExecutionData(any(), any(), any(), any());
    doReturn(WingsTestConstants.ACCOUNT_ID).when(appService).getAccountIdByAppId(any());
    doReturn(true).when(featureFlagService).isEnabled(any(), any());

    List<ServiceInstance> hostExclusionList =
        stateExecutionService.getHostExclusionList(stateExecutionInstance, phaseElement, null);

    assertThat(serviceInstanceList).isEqualTo(hostExclusionList);
  }
}