package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.common.Constants;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.metrics.MetricType;
import software.wings.metrics.appdynamics.AppdynamicsConstants;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
import software.wings.sm.ExecutionResponse;
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
  @Mock private InfrastructureMappingService infraMappingService;
  @Mock private ServiceResourceService serviceResourceService;

  @Mock private AppdynamicsService appdynamicsService;
  @Mock private PhaseElement phaseElement;

  private AppDynamicsState appDynamicsState;
  private String infraMappingId;

  @Before
  public void setup() throws IOException, IllegalAccessException {
    setupCommon();

    MockitoAnnotations.initMocks(this);
    infraMappingId = generateUuid();

    appDynamicsState = new AppDynamicsState("AppDynamicsState");
    appDynamicsState.setApplicationId("30444");
    appDynamicsState.setTierId("456");
    appDynamicsState.setTimeDuration("6000");

    when(appdynamicsService.getTiers(anyString(), anyLong()))
        .thenReturn(Sets.newHashSet(AppdynamicsTier.builder().id(456).name("tier").build()));
    FieldUtils.writeField(appDynamicsState, "appService", appService, true);
    FieldUtils.writeField(appDynamicsState, "configuration", configuration, true);
    FieldUtils.writeField(appDynamicsState, "settingsService", settingsService, true);
    FieldUtils.writeField(appDynamicsState, "waitNotifyEngine", waitNotifyEngine, true);
    FieldUtils.writeField(appDynamicsState, "delegateService", delegateService, true);
    FieldUtils.writeField(appDynamicsState, "wingsPersistence", wingsPersistence, true);
    FieldUtils.writeField(appDynamicsState, "secretManager", secretManager, true);
    FieldUtils.writeField(appDynamicsState, "metricAnalysisService", metricAnalysisService, true);
    FieldUtils.writeField(appDynamicsState, "templateExpressionProcessor", templateExpressionProcessor, true);
    FieldUtils.writeField(appDynamicsState, "workflowExecutionService", workflowExecutionService, true);
    FieldUtils.writeField(appDynamicsState, "continuousVerificationService", continuousVerificationService, true);
    FieldUtils.writeField(appDynamicsState, "workflowExecutionBaselineService", workflowExecutionBaselineService, true);
    FieldUtils.writeField(appDynamicsState, "appdynamicsService", appdynamicsService, true);
    FieldUtils.writeField(appDynamicsState, "featureFlagService", featureFlagService, true);
    FieldUtils.writeField(appDynamicsState, "infraMappingService", infraMappingService, true);
    FieldUtils.writeField(appDynamicsState, "versionInfoManager", versionInfoManager, true);
    FieldUtils.writeField(appDynamicsState, "serviceResourceService", serviceResourceService, true);

    when(executionContext.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM)).thenReturn(phaseElement);
    when(phaseElement.getInfraMappingId()).thenReturn(infraMappingId);
    when(executionContext.getAppId()).thenReturn(appId);
    when(infraMappingService.get(anyString(), anyString()))
        .thenReturn(anAwsInfrastructureMapping().withDeploymentType(DeploymentType.AWS_CODEDEPLOY.name()).build());
    when(serviceResourceService.getDeploymentType(anyObject(), anyObject(), anyObject()))
        .thenReturn(DeploymentType.AWS_CODEDEPLOY);

    setupCommonMocks();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldTestNonTemplatized() {
    AppDynamicsState spyAppDynamicsState = setupNonTemplatized(false);
    ExecutionResponse executionResponse = spyAppDynamicsState.execute(executionContext);
    assertEquals(ExecutionStatus.RUNNING, executionResponse.getExecutionStatus());
  }

  private AppDynamicsState setupNonTemplatized(boolean isBadTier) {
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

    if (isBadTier) {
      appDynamicsState.setTierId("123aa");
    }
    AppDynamicsState spyAppDynamicsState = spy(appDynamicsState);
    doReturn(Collections.singletonMap("test", DEFAULT_GROUP_NAME))
        .when(spyAppDynamicsState)
        .getCanaryNewHostNames(executionContext);
    doReturn(Collections.singletonMap("control", DEFAULT_GROUP_NAME))
        .when(spyAppDynamicsState)
        .getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyAppDynamicsState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyAppDynamicsState).getPhaseServiceId(executionContext);
    when(workflowStandardParams.getEnv())
        .thenReturn(Environment.Builder.anEnvironment().uuid(UUID.randomUUID().toString()).build());
    when(executionContext.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);

    when(metricAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
             StateType.APP_DYNAMICS, appId, workflowId, serviceId))
        .thenReturn(workflowExecutionId);
    return spyAppDynamicsState;
  }

  @Test
  @Category(UnitTests.class)
  public void shouldTestNonTemplatizedBadTier() {
    ExecutionResponse executionResponse = setupNonTemplatized(true).execute(executionContext);
    assertEquals(ExecutionStatus.ERROR, executionResponse.getExecutionStatus());
    assertEquals(
        "ApplicationID and TierID in AppDynamics setup must be valid numbers", executionResponse.getErrorMessage());
  }

  @Test
  @Category(UnitTests.class)
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
    doReturn(Collections.singletonMap("test", DEFAULT_GROUP_NAME))
        .when(spyAppDynamicsState)
        .getCanaryNewHostNames(executionContext);
    doReturn(Collections.singletonMap("control", DEFAULT_GROUP_NAME))
        .when(spyAppDynamicsState)
        .getLastExecutionNodes(executionContext);
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
        .thenReturn(Environment.Builder.anEnvironment().uuid(UUID.randomUUID().toString()).build());
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

  @Test
  @Category(UnitTests.class)
  public void testGetMetricType() {
    String errType = AppDynamicsState.getMetricTypeForMetric(AppdynamicsConstants.ERRORS_PER_MINUTE);
    assertNotNull(errType);
    assertEquals("Error Type should be", MetricType.ERROR.name(), errType);
    String throughput = AppDynamicsState.getMetricTypeForMetric(AppdynamicsConstants.CALLS_PER_MINUTE);
    assertNotNull(throughput);
    assertEquals("Error Type should be", MetricType.THROUGHPUT.name(), throughput);
    String respTime = AppDynamicsState.getMetricTypeForMetric(AppdynamicsConstants.AVG_RESPONSE_TIME);
    assertNotNull(respTime);
    assertEquals("Error Type should be", MetricType.RESP_TIME.name(), respTime);

    String dummy = AppDynamicsState.getMetricTypeForMetric("incorrectName");
    assertNull(dummy);
  }
}
