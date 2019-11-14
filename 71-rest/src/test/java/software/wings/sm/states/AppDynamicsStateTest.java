package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.ERROR;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.exception.WingsException;
import io.harness.rule.OwnerRule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.beans.AccountType;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.beans.WorkflowExecution;
import software.wings.metrics.MetricType;
import software.wings.metrics.appdynamics.AppdynamicsConstants;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
import software.wings.service.intfc.verification.CVActivityLogService.Logger;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// import software.wings.common.Constants;

/**
 * author Srinivas
 */
public class AppDynamicsStateTest extends APMStateVerificationTestBase {
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

    AppService appService = mock(AppService.class);
    when(appService.getAccountIdByAppId(anyString())).thenReturn(generateUuid());
    when(appService.get(anyString()))
        .thenReturn(Application.Builder.anApplication().name(generateUuid()).accountId(accountId).build());

    AccountService accountService = mock(AccountService.class);
    when(accountService.getAccountType(anyString())).thenReturn(Optional.of(AccountType.PAID));

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
    FieldUtils.writeField(appDynamicsState, "appService", appService, true);
    FieldUtils.writeField(appDynamicsState, "accountService", accountService, true);
    FieldUtils.writeField(appDynamicsState, "cvActivityLogService", cvActivityLogService, true);
    when(cvActivityLogService.getLoggerByStateExecutionId(anyString())).thenReturn(mock(Logger.class));

    when(executionContext.getContextElement(ContextElementType.PARAM, AbstractAnalysisStateTest.PHASE_PARAM))
        .thenReturn(phaseElement);
    when(executionContext.fetchInfraMappingId()).thenReturn(infraMappingId);
    when(executionContext.getAppId()).thenReturn(appId);
    when(infraMappingService.get(anyString(), anyString()))
        .thenReturn(anAwsInfrastructureMapping().withDeploymentType(DeploymentType.AWS_CODEDEPLOY.name()).build());
    when(serviceResourceService.getDeploymentType(anyObject(), anyObject(), anyObject()))
        .thenReturn(DeploymentType.AWS_CODEDEPLOY);
    setupCommonMocks();
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldTestNonTemplatized() {
    AppDynamicsState spyAppDynamicsState = setupNonTemplatized(false);
    ExecutionResponse executionResponse = spyAppDynamicsState.execute(executionContext);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.RUNNING);
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
             StateType.APP_DYNAMICS, appId, workflowId, serviceId, infraMappingId, environment.getUuid()))
        .thenReturn(workflowExecutionId);
    return spyAppDynamicsState;
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldTestNonTemplatizedBadTier() {
    ExecutionResponse executionResponse = setupNonTemplatized(true).execute(executionContext);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ERROR);
    assertThat(executionResponse.getErrorMessage())
        .isEqualTo("Error while fetching from AppDynamics. ApplicationId : 30444 and "
            + "TierId : 123aa in AppDynamics setup must be valid numbers");
  }

  @Test
  @Owner(developers = UNKNOWN)
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
             StateType.APP_DYNAMICS, appId, workflowId, serviceId, infraMappingId, environment.getUuid()))
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
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.RUNNING);
    Map<Long,
        LinkedHashMap<String,
            LinkedHashMap<String,
                LinkedHashMap<String, LinkedHashMap<String, List<ContinuousVerificationExecutionMetaData>>>>>>
        cvExecutionMetaData =
            continuousVerificationService.getCVExecutionMetaData(accountId, 1519200000000L, 1519200000001L, user);
    assertThat(cvExecutionMetaData).isNotNull();
    ContinuousVerificationExecutionMetaData continuousVerificationExecutionMetaData1 =
        cvExecutionMetaData.get(1519171200000L)
            .get("dummy artifact")
            .get("dummy env/dummy workflow")
            .values()
            .iterator()
            .next()
            .get("BASIC")
            .get(0);
    assertThat(accountId).isEqualTo(continuousVerificationExecutionMetaData1.getAccountId());
    assertThat("dummy artifact").isEqualTo(continuousVerificationExecutionMetaData1.getArtifactName());
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testGetMetricType() {
    String errType = AppDynamicsState.getMetricTypeForMetric(AppdynamicsConstants.ERRORS_PER_MINUTE);
    assertThat(errType).isNotNull();
    assertThat(errType).isEqualTo(MetricType.ERROR.name());
    String throughput = AppDynamicsState.getMetricTypeForMetric(AppdynamicsConstants.CALLS_PER_MINUTE);
    assertThat(throughput).isNotNull();
    assertThat(throughput).isEqualTo(MetricType.THROUGHPUT.name());
    String respTime = AppDynamicsState.getMetricTypeForMetric(AppdynamicsConstants.AVG_RESPONSE_TIME);
    assertThat(respTime).isNotNull();
    assertThat(respTime).isEqualTo(MetricType.RESP_TIME.name());

    String dummy = AppDynamicsState.getMetricTypeForMetric("incorrectName");
    assertThat(dummy).isNull();
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testValidateFieldsMissingFieldsCase() {
    AppDynamicsState appDynamicsState = new AppDynamicsState("dummy");
    // not adding any metrics for verification
    Map<String, String> invalidFields = appDynamicsState.validateFields();
    assertThat(invalidFields.size() == 1).isTrue();
    assertThat(invalidFields.keySet().iterator().next()).isEqualTo("Required Fields missing");
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testValidateFieldsPartialMissingFieldsCase() {
    AppDynamicsState appDynamicsState = new AppDynamicsState("dummy");
    appDynamicsState.setApplicationId("test");
    appDynamicsState.setTierId("test12");
    // not adding any metrics for verification
    Map<String, String> invalidFields = appDynamicsState.validateFields();
    assertThat(invalidFields.size() == 1).isTrue();
    assertThat(invalidFields.keySet().iterator().next()).isEqualTo("Required Fields missing");
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testValidateFieldsInValidCase() {
    AppDynamicsState appDynamicsState = new AppDynamicsState("dummy");
    appDynamicsState.setApplicationId("test");
    appDynamicsState.setTierId("test12");
    appDynamicsState.setAnalysisServerConfigId("test1234");
    // not adding any metrics for verification
    Map<String, String> invalidFields = appDynamicsState.validateFields();
    assertThat(invalidFields.size() == 1).isTrue();
    assertThat(invalidFields.keySet().iterator().next()).isEqualTo("Invalid Required Fields");
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testEmptyParam() {
    AppDynamicsState appDynamicsState = new AppDynamicsState("dummy");
    final Map<String, String> validationResult = appDynamicsState.validateFields();
    assertThat(validationResult.size()).isEqualTo(1);
    assertThat(validationResult.get("Required Fields missing"))
        .isEqualTo("Connector, Application and tier should be provided");
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testInvalidApp() {
    AppDynamicsState appDynamicsState = new AppDynamicsState("dummy");
    appDynamicsState.setAnalysisServerConfigId(generateUuid());
    appDynamicsState.setApplicationId(generateUuid());
    appDynamicsState.setTierId(generateUuid());

    final Map<String, String> validationResult = appDynamicsState.validateFields();
    assertThat(validationResult.size()).isEqualTo(1);
    assertThat(validationResult.get("Invalid Required Fields")).isEqualTo("Valid AppId and tierId should be provided");
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testInvalidTier() {
    AppDynamicsState appDynamicsState = new AppDynamicsState("dummy");
    appDynamicsState.setAnalysisServerConfigId(generateUuid());
    appDynamicsState.setApplicationId("123");
    appDynamicsState.setTierId(generateUuid());

    final Map<String, String> validationResult = appDynamicsState.validateFields();
    assertThat(validationResult.size()).isEqualTo(1);
    assertThat(validationResult.get("Invalid Required Fields")).isEqualTo("Valid AppId and tierId should be provided");
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testValidNonTemplatized() {
    AppDynamicsState appDynamicsState = new AppDynamicsState("dummy");
    appDynamicsState.setAnalysisServerConfigId(generateUuid());
    appDynamicsState.setApplicationId("123");
    appDynamicsState.setTierId("456");

    final Map<String, String> validationResult = appDynamicsState.validateFields();
    assertThat(validationResult.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testOnlyConnectorTemplatized() {
    AppDynamicsState appDynamicsState = new AppDynamicsState("dummy");
    appDynamicsState.setAnalysisServerConfigId("${AppDynamics_Server}");
    appDynamicsState.setApplicationId("123");
    appDynamicsState.setTierId("456");
    appDynamicsState.setTemplateExpressions(
        Lists.newArrayList(TemplateExpression.builder()
                               .fieldName("analysisServerConfigId")
                               .expression("${AppDynamics_Server}")
                               .metadata(ImmutableMap.of("entityType", "APPDYNAMICS_CONFIGID"))
                               .build()));

    final Map<String, String> validationResult = appDynamicsState.validateFields();
    assertThat(validationResult.size()).isEqualTo(1);
    assertThat(validationResult.get("Invalid templatization for application"))
        .isEqualTo("If connector is templatized then application should be templatized as well");
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testConnectorAndAppTemplatized() {
    AppDynamicsState appDynamicsState = new AppDynamicsState("dummy");
    appDynamicsState.setAnalysisServerConfigId("${AppDynamics_Server}");
    appDynamicsState.setApplicationId("123");
    appDynamicsState.setTierId("456");
    appDynamicsState.setTemplateExpressions(
        Lists.newArrayList(TemplateExpression.builder()
                               .fieldName("analysisServerConfigId")
                               .expression("${AppDynamics_Server}")
                               .metadata(ImmutableMap.of("entityType", "APPDYNAMICS_CONFIGID"))
                               .build(),
            TemplateExpression.builder()
                .fieldName("applicationId")
                .expression("${AppDynamics_App}")
                .metadata(ImmutableMap.of("entityType", "APPDYNAMICS_APPID"))
                .build()));

    final Map<String, String> validationResult = appDynamicsState.validateFields();
    assertThat(validationResult.size()).isEqualTo(1);
    assertThat(validationResult.get("Invalid templatization for tier"))
        .isEqualTo("If application is templatized then tier should be templatized as well");
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testAllTemplatized() {
    AppDynamicsState appDynamicsState = new AppDynamicsState("dummy");
    appDynamicsState.setTemplateExpressions(
        Lists.newArrayList(TemplateExpression.builder()
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

    final Map<String, String> validationResult = appDynamicsState.validateFields();
    assertThat(validationResult.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldTestTriggered() throws IOException {
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
    wingsPersistence.save(WorkflowExecution.builder()
                              .appId(appId)
                              .uuid(workflowExecutionId)
                              .triggeredBy(EmbeddedUser.builder().name("Deployment Trigger workflow").build())
                              .build());

    AppDynamicsState spyAppDynamicsState = spy(appDynamicsState);
    when(appdynamicsService.getAppDynamicsApplication(anyString(), anyString())).thenReturn(null);
    doThrow(new WingsException("Can not find application by name"))
        .when(appdynamicsService)
        .getAppDynamicsApplicationByName(anyString(), anyString());
    when(appdynamicsService.getTier(anyString(), anyLong(), anyString())).thenReturn(null);
    doThrow(new WingsException("Can not find tier by name"))
        .when(appdynamicsService)
        .getTier(anyString(), anyLong(), anyString());

    doReturn(asList(TemplateExpression.builder()
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
                     .build()))
        .when(spyAppDynamicsState)
        .getTemplateExpressions();

    doReturn(Collections.singletonMap("test", DEFAULT_GROUP_NAME))
        .when(spyAppDynamicsState)
        .getCanaryNewHostNames(executionContext);
    doReturn(Collections.singletonMap("control", DEFAULT_GROUP_NAME))
        .when(spyAppDynamicsState)
        .getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyAppDynamicsState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyAppDynamicsState).getPhaseServiceId(executionContext);

    when(metricAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
             StateType.APP_DYNAMICS, appId, workflowId, serviceId, infraMappingId, environment.getUuid()))
        .thenReturn(workflowExecutionId);
    when(executionContext.renderExpression("${workflow.variables.AppDynamics_Server}"))
        .thenReturn(settingAttribute.getUuid());
    when(executionContext.renderExpression("${workflow.variables.AppDynamics_App}")).thenReturn("30444");
    when(executionContext.renderExpression("${workflow.variables.AppDynamics_Tier}")).thenReturn("30889");
    doReturn(Environment.Builder.anEnvironment().uuid(UUID.randomUUID().toString()).build())
        .when(workflowStandardParams)
        .getEnv();
    when(executionContext.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);

    ExecutionResponse executionResponse = spyAppDynamicsState.execute(executionContext);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ERROR);
    assertThat(executionResponse.getErrorMessage()).isEqualTo("Can not find application by name");

    doReturn(NewRelicApplication.builder().build())
        .when(appdynamicsService)
        .getAppDynamicsApplication(anyString(), anyString());
    executionResponse = spyAppDynamicsState.execute(executionContext);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ERROR);
    assertThat(executionResponse.getErrorMessage()).isEqualTo("Can not find tier by name");

    long tierId = 30889;
    doReturn(AppdynamicsTier.builder().name(generateUuid()).id(tierId).build())
        .when(appdynamicsService)
        .getTier(anyString(), anyLong(), anyString());
    doReturn(Sets.newHashSet(AppdynamicsTier.builder().name(generateUuid()).id(tierId).build()))
        .when(appdynamicsService)
        .getTiers(anyString(), anyLong());
    executionResponse = spyAppDynamicsState.execute(executionContext);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(RUNNING);

    when(appdynamicsService.getTier(anyString(), anyLong(), anyString())).thenReturn(null);
    doThrow(new WingsException("No tier found"))
        .when(appdynamicsService)
        .getTierByName(anyString(), anyString(), anyString());

    executionResponse = spyAppDynamicsState.execute(executionContext);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ERROR);
    assertThat(executionResponse.getErrorMessage()).isEqualTo("No tier found");

    when(appdynamicsService.getAppDynamicsApplication(anyString(), anyString())).thenReturn(null);
    doThrow(new WingsException("No app found"))
        .when(appdynamicsService)
        .getAppDynamicsApplicationByName(anyString(), anyString());

    executionResponse = spyAppDynamicsState.execute(executionContext);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ERROR);
    assertThat(executionResponse.getErrorMessage()).isEqualTo("No app found");
  }
}
