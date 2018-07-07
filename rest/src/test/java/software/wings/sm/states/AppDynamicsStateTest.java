package software.wings.sm.states;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * author Srinivas
 */
public class AppDynamicsStateTest extends APMStateVerificationTestBase {
  @Inject private TemplateExpressionProcessor templateExpressionProcessor;
  @Mock private MetricDataAnalysisService metricAnalysisService;

  @Mock private AppdynamicsService appdynamicsService;

  private AppDynamicsState appDynamicsState;

  @Before
  public void setup() throws IOException {
    setupCommon();

    MockitoAnnotations.initMocks(this);

    appDynamicsState = new AppDynamicsState("AppDynamicsState");
    appDynamicsState.setApplicationId("30444");
    appDynamicsState.setTierId("456");
    appDynamicsState.setTimeDuration("6000");

    when(appdynamicsService.getTiers(anyString(), anyLong()))
        .thenReturn(Sets.newHashSet(AppdynamicsTier.builder().id(456).name("tier").build()));
    setInternalState(appDynamicsState, "appService", appService);
    setInternalState(appDynamicsState, "configuration", configuration);
    setInternalState(appDynamicsState, "settingsService", settingsService);
    setInternalState(appDynamicsState, "waitNotifyEngine", waitNotifyEngine);
    setInternalState(appDynamicsState, "delegateService", delegateService);
    setInternalState(appDynamicsState, "jobScheduler", jobScheduler);
    setInternalState(appDynamicsState, "secretManager", secretManager);
    setInternalState(appDynamicsState, "metricAnalysisService", metricAnalysisService);
    setInternalState(appDynamicsState, "templateExpressionProcessor", templateExpressionProcessor);
    setInternalState(appDynamicsState, "workflowExecutionService", workflowExecutionService);
    setInternalState(appDynamicsState, "continuousVerificationService", continuousVerificationService);
    setInternalState(appDynamicsState, "workflowExecutionBaselineService", workflowExecutionBaselineService);
    setInternalState(appDynamicsState, "appdynamicsService", appdynamicsService);
    setInternalState(appDynamicsState, "featureFlagService", featureFlagService);

    setupCommonMocks();
  }

  @Test
  public void shouldTestNonTemplatized() {
    AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                              .accountId(accountId)
                                              .controllerUrl("appd-url")
                                              .username("appd-user")
                                              .password("appd-pwd".toCharArray())
                                              .build();
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withName("appd-config")
                                            .withValue(appDynamicsConfig)
                                            .build();
    wingsPersistence.save(settingAttribute);
    appDynamicsState.setAnalysisServerConfigId(settingAttribute.getUuid());

    AppDynamicsState spyAppDynamicsState = spy(appDynamicsState);
    doReturn(Collections.singleton("test")).when(spyAppDynamicsState).getCanaryNewHostNames(executionContext);
    doReturn(Collections.singleton("control")).when(spyAppDynamicsState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyAppDynamicsState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyAppDynamicsState).getPhaseServiceId(executionContext);
    when(workflowStandardParams.getEnv())
        .thenReturn(Environment.Builder.anEnvironment().withUuid(UUID.randomUUID().toString()).build());
    when(executionContext.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);

    when(metricAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
             StateType.APP_DYNAMICS, appId, workflowId, serviceId))
        .thenReturn(workflowExecutionId);
    ExecutionResponse executionResponse = spyAppDynamicsState.execute(executionContext);
    assertEquals(ExecutionStatus.RUNNING, executionResponse.getExecutionStatus());
  }

  @Test
  public void shouldTestAllTemplatized() throws ParseException, IOException {
    AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                              .accountId(accountId)
                                              .controllerUrl("appd-url")
                                              .username("appd-user")
                                              .password("appd-pwd".toCharArray())
                                              .build();
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withName("appd-config")
                                            .withValue(appDynamicsConfig)
                                            .build();
    wingsPersistence.save(settingAttribute);
    appDynamicsState.setAnalysisServerConfigId(settingAttribute.getUuid());

    appDynamicsState.setTemplateExpressions(asList(TemplateExpression.builder()
                                                       .fieldName("analysisServerConfigId")
                                                       .expression("${AppDynamics_Server}")
                                                       .metadata(ImmutableMap.of("entityType", "APPDYNAMICS_CONFIGID"))
                                                       .build(),
        TemplateExpression.builder()
            .fieldName("applicationId")
            .expression("${AppDynamics_App}")
            .metadata(ImmutableMap.of("entityType", "APPDYNAMICS_APPID"))
            .build(),
        TemplateExpression.builder()
            .fieldName("tierId")
            .expression("${AppDynamics_Tier}")
            .metadata(ImmutableMap.of("entityType", "APPDYNAMICS_TIERID"))
            .build()));

    AppDynamicsState spyAppDynamicsState = spy(appDynamicsState);
    doReturn(Collections.singleton("test")).when(spyAppDynamicsState).getCanaryNewHostNames(executionContext);
    doReturn(Collections.singleton("control")).when(spyAppDynamicsState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyAppDynamicsState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyAppDynamicsState).getPhaseServiceId(executionContext);

    when(metricAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
             StateType.APP_DYNAMICS, appId, workflowId, serviceId))
        .thenReturn(workflowExecutionId);
    when(executionContext.renderExpression("${workflow.variables.AppDynamics_Server}"))
        .thenReturn(settingAttribute.getUuid());
    when(executionContext.renderExpression("${workflow.variables.AppDynamics_App}")).thenReturn("30444");
    when(executionContext.renderExpression("${workflow.variables.AppDynamics_Tier}")).thenReturn("30889");
    when(appdynamicsService.getTiers(anyString(), anyLong()))
        .thenReturn(Sets.newHashSet(AppdynamicsTier.builder().id(30889).name("tier").build()));
    when(workflowStandardParams.getEnv())
        .thenReturn(Environment.Builder.anEnvironment().withUuid(UUID.randomUUID().toString()).build());
    when(executionContext.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);
    ExecutionResponse executionResponse = spyAppDynamicsState.execute(executionContext);
    assertEquals(ExecutionStatus.RUNNING, executionResponse.getExecutionStatus());
    Map<Long,
        LinkedHashMap<String,
            LinkedHashMap<String,
                LinkedHashMap<String, LinkedHashMap<String, List<ContinuousVerificationExecutionMetaData>>>>>>
        cvExecutionMetaData =
            continuousVerificationService.getCVExecutionMetaData(accountId, 1519200000000L, 1519200000001L, user);
    assertNotNull(cvExecutionMetaData);
    ContinuousVerificationExecutionMetaData continuousVerificationExecutionMetaData1 =
        cvExecutionMetaData.get(1519171200000L)
            .get("dummy artifact")
            .get("dummy env/dummy workflow")
            .values()
            .iterator()
            .next()
            .get("BASIC")
            .get(0);
    assertEquals(continuousVerificationExecutionMetaData1.getAccountId(), accountId);
    assertEquals(continuousVerificationExecutionMetaData1.getArtifactName(), "dummy artifact");
  }
}
