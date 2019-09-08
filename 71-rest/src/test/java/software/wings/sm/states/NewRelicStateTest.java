package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.ERROR;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.api.DeploymentType;
import software.wings.beans.AccountType;
import software.wings.beans.Environment;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.beans.WorkflowExecution;
import software.wings.metrics.MetricType;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup.TimeSeriesMlAnalysisGroupInfo;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.impl.newrelic.NewRelicMetricValueDefinition;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.service.intfc.verification.CVActivityLogService.Logger;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;
import software.wings.sm.states.NewRelicState.Metric;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * author: Praveen
 */

public class NewRelicStateTest extends APMStateVerificationTestBase {
  private NewRelicState nrState;

  private NewRelicState.Metric requestsPerMinuteMetric, averageResponseTimeMetric, errorMetric, apdexScoreMetric;
  private List<Metric> expectedMetrics;

  @Mock private MetricDataAnalysisService metricAnalysisService;
  @Mock private InfrastructureMappingService infraMappingService;
  @Inject @InjectMocks private NewRelicService newRelicService;
  private String infraMappingId;
  private NewRelicState newRelicState;

  @Before
  public void setup() throws Exception {
    nrState = new NewRelicState("nrStateName");
    setupCommon();
    MockitoAnnotations.initMocks(this);
    FieldUtils.writeField(nrState, "metricAnalysisService", metricAnalysisService, true);
    requestsPerMinuteMetric = NewRelicState.Metric.builder()
                                  .metricName(NewRelicMetricValueDefinition.REQUSET_PER_MINUTE)
                                  .mlMetricType(MetricType.THROUGHPUT)
                                  .displayName("Requests per Minute")
                                  .build();
    averageResponseTimeMetric = NewRelicState.Metric.builder()
                                    .metricName(NewRelicMetricValueDefinition.AVERAGE_RESPONSE_TIME)
                                    .mlMetricType(MetricType.RESP_TIME)
                                    .displayName("Response Time")
                                    .build();
    errorMetric = NewRelicState.Metric.builder()
                      .metricName(NewRelicMetricValueDefinition.ERROR)
                      .mlMetricType(MetricType.ERROR)
                      .displayName("ERROR")
                      .build();
    apdexScoreMetric = NewRelicState.Metric.builder()
                           .metricName(NewRelicMetricValueDefinition.APDEX_SCORE)
                           .mlMetricType(MetricType.APDEX)
                           .displayName("Apdex Score")
                           .build();

    expectedMetrics = Arrays.asList(requestsPerMinuteMetric, averageResponseTimeMetric, errorMetric, apdexScoreMetric);
    infraMappingId = generateUuid();
    when(executionContext.getAccountId()).thenReturn(accountId);
    when(executionContext.getContextElement(ContextElementType.PARAM, AbstractAnalysisStateTest.PHASE_PARAM))
        .thenReturn(phaseElement);
    when(executionContext.fetchInfraMappingId()).thenReturn(infraMappingId);
    when(executionContext.getAppId()).thenReturn(appId);
    when(infraMappingService.get(anyString(), anyString()))
        .thenReturn(anAwsInfrastructureMapping().withDeploymentType(DeploymentType.KUBERNETES.name()).build());

    newRelicState = new NewRelicState("NewRelicState");
    newRelicState.setApplicationId("30444");
    newRelicState.setTimeDuration("6000");

    AccountService accountService = mock(AccountService.class);
    when(accountService.getAccountType(anyString())).thenReturn(Optional.of(AccountType.PAID));

    FieldUtils.writeField(newRelicState, "appService", appService, true);
    FieldUtils.writeField(newRelicState, "configuration", configuration, true);
    FieldUtils.writeField(newRelicState, "settingsService", settingsService, true);
    FieldUtils.writeField(newRelicState, "waitNotifyEngine", waitNotifyEngine, true);
    FieldUtils.writeField(newRelicState, "delegateService", delegateService, true);
    FieldUtils.writeField(newRelicState, "wingsPersistence", wingsPersistence, true);
    FieldUtils.writeField(newRelicState, "secretManager", secretManager, true);
    FieldUtils.writeField(newRelicState, "metricAnalysisService", metricAnalysisService, true);
    FieldUtils.writeField(newRelicState, "templateExpressionProcessor", templateExpressionProcessor, true);
    FieldUtils.writeField(newRelicState, "workflowExecutionService", workflowExecutionService, true);
    FieldUtils.writeField(newRelicState, "continuousVerificationService", continuousVerificationService, true);
    FieldUtils.writeField(newRelicState, "workflowExecutionBaselineService", workflowExecutionBaselineService, true);
    FieldUtils.writeField(newRelicState, "newRelicService", newRelicService, true);
    FieldUtils.writeField(newRelicState, "featureFlagService", featureFlagService, true);
    FieldUtils.writeField(newRelicState, "infraMappingService", infraMappingService, true);
    FieldUtils.writeField(newRelicState, "versionInfoManager", versionInfoManager, true);
    //    FieldUtils.writeField(newRelicState, "serviceResourceService", serviceResourceService, true);
    FieldUtils.writeField(newRelicState, "appService", appService, true);
    FieldUtils.writeField(newRelicState, "accountService", accountService, true);
    FieldUtils.writeField(newRelicState, "cvActivityLogService", cvActivityLogService, true);
    when(cvActivityLogService.getLoggerByStateExecutionId(anyString())).thenReturn(mock(Logger.class));

    setupCommonMocks();
  }

  @Test
  @Category(UnitTests.class)
  public void testAnalysisType() {
    nrState.setComparisonStrategy("COMPARE_WITH_CURRENT");
    assertThat(nrState.getAnalysisType()).isEqualTo(TimeSeriesMlAnalysisType.COMPARATIVE);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetAnalysisTypePredictive() {
    nrState.setComparisonStrategy("PREDICTIVE");
    assertThat(nrState.getAnalysisType()).isEqualTo(TimeSeriesMlAnalysisType.PREDICTIVE);
  }

  @Test
  @Category(UnitTests.class)
  public void testCreateGroup() {
    // setup
    Map<String, String> hosts = new HashMap<>();
    hosts.put("dummy", DEFAULT_GROUP_NAME);
    Map<String, TimeSeriesMlAnalysisGroupInfo> metricGroups = new HashMap<>();
    TimeSeriesMlAnalysisGroupInfo analysisGroupInfo = TimeSeriesMlAnalysisGroupInfo.builder()
                                                          .groupName(DEFAULT_GROUP_NAME)
                                                          .mlAnalysisType(TimeSeriesMlAnalysisType.COMPARATIVE)
                                                          .build();
    metricGroups.put(DEFAULT_GROUP_NAME, analysisGroupInfo);
    doNothing()
        .when(metricAnalysisService)
        .saveMetricGroups(appId, StateType.NEW_RELIC, stateExecutionId, metricGroups);

    // execute

    nrState.setComparisonStrategy("COMPARE_WITH_CURRENT");
    nrState.createAndSaveMetricGroups(executionContext, hosts);

    // verify
    verify(metricAnalysisService).saveMetricGroups(appId, StateType.NEW_RELIC, stateExecutionId, metricGroups);
  }

  @Test
  @Category(UnitTests.class)
  public void testMetricsCorrespondingToMetricNames() {
    /*
    Case 1: metricNames is an empty list
    Expected output: Metric Map should contain all metrics present in the YAML file
     */
    List<String> metricNames = new ArrayList<>();
    Map<String, Metric> metrics = newRelicService.getMetricsCorrespondingToMetricNames(metricNames);
    assertThat(metrics.containsKey("requestsPerMinute")).isTrue();
    assertThat(metrics.containsKey("averageResponseTime")).isTrue();
    assertThat(metrics.containsKey("error")).isTrue();
    assertThat(metrics.containsKey("apdexScore")).isTrue();

    /*
    Case 2: metricNames contains a non-empty subset of metrics
     */
    metricNames = Arrays.asList("apdexScore");
    metrics = newRelicService.getMetricsCorrespondingToMetricNames(metricNames);
    assertThat(metrics.containsKey("apdexScore")).isTrue();
    assertThat(metrics).hasSize(1);
    assertThat(metrics.get("apdexScore").getTags().size() >= 1).isTrue();
    assertThat(metrics.get("apdexScore").getTags()).isEqualTo(Sets.newHashSet("WebTransactions"));

    metricNames = Arrays.asList("apdexScore", "averageResponseTime", "requestsPerMinute");
    metrics = newRelicService.getMetricsCorrespondingToMetricNames(metricNames);
    assertThat(metrics.containsKey("apdexScore")).isTrue();
    assertThat(metrics.containsKey("averageResponseTime")).isTrue();
    assertThat(metrics.containsKey("requestsPerMinute")).isTrue();
    assertThat(metrics).hasSize(3);

    /*
    Case 3: metricNames contains a list in which are metric names are incorrect
    Expected output: Empty map
     */
    metricNames = Arrays.asList("ApdexScore");
    metrics = newRelicService.getMetricsCorrespondingToMetricNames(metricNames);
    assertThat(metrics).isEqualTo(new HashMap<>());

    /*
    Case 4: metricNames is null
    Expected output:
     */
    metricNames = null;
    metrics = newRelicService.getMetricsCorrespondingToMetricNames(metricNames);
    assertThat(metrics.containsKey("requestsPerMinute")).isTrue();
    assertThat(metrics.containsKey("averageResponseTime")).isTrue();
    assertThat(metrics.containsKey("error")).isTrue();
    assertThat(metrics.containsKey("apdexScore")).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void metricNames() {
    List<NewRelicState.Metric> actualMetrics = newRelicService.getListOfMetrics();
    assertThat(actualMetrics).isEqualTo(expectedMetrics);
  }

  private TimeSeriesMetricDefinition buildTimeSeriesMetricDefinition(Metric metric) {
    return TimeSeriesMetricDefinition.builder()
        .metricName(metric.getMetricName())
        .metricType(metric.getMlMetricType())
        .tags(metric.getTags())
        .build();
  }

  @Test
  @Category(UnitTests.class)
  public void metricDefinitions() {
    Map<String, TimeSeriesMetricDefinition> expectedMetricDefinitions = new HashMap<>();
    expectedMetricDefinitions.put(
        requestsPerMinuteMetric.getMetricName(), buildTimeSeriesMetricDefinition(requestsPerMinuteMetric));
    expectedMetricDefinitions.put(
        averageResponseTimeMetric.getMetricName(), buildTimeSeriesMetricDefinition(averageResponseTimeMetric));
    expectedMetricDefinitions.put(errorMetric.getMetricName(), buildTimeSeriesMetricDefinition(errorMetric));
    expectedMetricDefinitions.put(apdexScoreMetric.getMetricName(), buildTimeSeriesMetricDefinition(apdexScoreMetric));

    List<String> metricNames = Arrays.asList("requestsPerMinute", "averageResponseTime", "error", "apdexScore");
    Map<String, Metric> metrics = newRelicService.getMetricsCorrespondingToMetricNames(metricNames);
    Map<String, TimeSeriesMetricDefinition> actualMetricDefinitions =
        newRelicService.metricDefinitions(metrics.values());

    assertThat(actualMetricDefinitions.get("requestsPerMinute"))
        .isEqualTo(expectedMetricDefinitions.get("requestsPerMinute"));
    assertThat(actualMetricDefinitions.get("averageResponseTime"))
        .isEqualTo(expectedMetricDefinitions.get("averageResponseTime"));
    assertThat(actualMetricDefinitions.get("error")).isEqualTo(expectedMetricDefinitions.get("error"));
    assertThat(actualMetricDefinitions.get("apdexScore")).isEqualTo(expectedMetricDefinitions.get("apdexScore"));
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMetricType() {
    String errType = NewRelicState.getMetricTypeForMetric(NewRelicMetricValueDefinition.ERROR);
    assertThat(errType).isNotNull();
    assertThat(errType).isEqualTo(MetricType.ERROR.name());
    String throughput = NewRelicState.getMetricTypeForMetric(NewRelicMetricValueDefinition.REQUSET_PER_MINUTE);
    assertThat(throughput).isNotNull();
    assertThat(throughput).isEqualTo(MetricType.THROUGHPUT.name());
    String respTime = NewRelicState.getMetricTypeForMetric(NewRelicMetricValueDefinition.AVERAGE_RESPONSE_TIME);
    assertThat(respTime).isNotNull();
    assertThat(respTime).isEqualTo(MetricType.RESP_TIME.name());

    String dummy = NewRelicState.getMetricTypeForMetric("incorrectName");
    assertThat(dummy).isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldTestTriggered() throws IOException, IllegalAccessException {
    NewRelicConfig newRelicConfig = NewRelicConfig.builder()
                                        .accountId(accountId)
                                        .newRelicUrl("newrelic-url")
                                        .apiKey(generateUuid().toCharArray())
                                        .build();
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withName("relic-config")
                                            .withValue(newRelicConfig)
                                            .build();
    wingsPersistence.save(settingAttribute);
    newRelicState.setAnalysisServerConfigId(settingAttribute.getUuid());
    wingsPersistence.save(WorkflowExecution.builder()
                              .appId(appId)
                              .uuid(workflowExecutionId)
                              .triggeredBy(EmbeddedUser.builder().name("Deployment Trigger workflow").build())
                              .build());

    final NewRelicService mockNewRelicService = mock(NewRelicService.class);
    FieldUtils.writeField(newRelicState, "newRelicService", mockNewRelicService, true);
    doThrow(new WingsException("Can not find application by id"))
        .when(mockNewRelicService)
        .resolveApplicationId(anyString(), anyString());

    doThrow(new WingsException("Can not find application by name"))
        .when(mockNewRelicService)
        .resolveApplicationName(anyString(), anyString());

    NewRelicState spyNewRelicState = spy(newRelicState);
    doReturn(false).when(spyNewRelicState).isEligibleForPerMinuteTask(accountId);
    doReturn(false).when(spyNewRelicState).isDemoPath(accountId);

    doReturn(asList(TemplateExpression.builder()
                        .fieldName("analysisServerConfigId")
                        .expression("${NewRelic_Server}")
                        .metadata(ImmutableMap.of("entityType", "NEWRELIC_CONFIGID"))
                        .build(),
                 TemplateExpression.builder()
                     .fieldName("applicationId")
                     .expression("${NewRelic_App}")
                     .metadata(ImmutableMap.of("entityType", "NEWRELIC_APPID"))
                     .build()))
        .when(spyNewRelicState)
        .getTemplateExpressions();

    doReturn(Collections.singletonMap("test", DEFAULT_GROUP_NAME))
        .when(spyNewRelicState)
        .getCanaryNewHostNames(executionContext);
    doReturn(Collections.singletonMap("control", DEFAULT_GROUP_NAME))
        .when(spyNewRelicState)
        .getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyNewRelicState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyNewRelicState).getPhaseServiceId(executionContext);

    when(metricAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
             StateType.NEW_RELIC, appId, workflowId, serviceId, infraMappingId, environment.getUuid()))
        .thenReturn(workflowExecutionId);
    when(executionContext.renderExpression("${workflow.variables.NewRelic_Server}"))
        .thenReturn(settingAttribute.getUuid());
    when(executionContext.renderExpression("${workflow.variables.NewRelic_App}")).thenReturn("30444");
    doReturn(Environment.Builder.anEnvironment().uuid(UUID.randomUUID().toString()).build())
        .when(workflowStandardParams)
        .getEnv();
    when(executionContext.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);

    ExecutionResponse executionResponse = spyNewRelicState.execute(executionContext);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ERROR);
    assertThat(executionResponse.getErrorMessage()).isEqualTo("Can not find application by name");

    doReturn(NewRelicApplication.builder().build())
        .when(mockNewRelicService)
        .resolveApplicationId(anyString(), anyString());
    executionResponse = spyNewRelicState.execute(executionContext);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(RUNNING);

    doThrow(new RuntimeException("Can not find application by id"))
        .when(mockNewRelicService)
        .resolveApplicationId(anyString(), anyString());

    executionResponse = spyNewRelicState.execute(executionContext);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ERROR);
    assertThat(executionResponse.getErrorMessage()).isEqualTo("RuntimeException: Can not find application by id");
  }
}
