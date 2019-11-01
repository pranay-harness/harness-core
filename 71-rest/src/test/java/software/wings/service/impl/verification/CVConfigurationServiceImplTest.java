package software.wings.service.impl.verification;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;

import io.fabric8.utils.Lists;
import io.harness.category.element.UnitTests;
import io.harness.exception.VerificationOperationException;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.FeatureName;
import software.wings.metrics.MetricType;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.NewRelicMetricValueDefinition;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.sm.states.StackDriverState;
import software.wings.verification.CVConfiguration;
import software.wings.verification.CVConfiguration.CVConfigurationKeys;
import software.wings.verification.cloudwatch.CloudWatchCVServiceConfiguration;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;
import software.wings.verification.log.LogsCVConfiguration;
import software.wings.verification.prometheus.PrometheusCVServiceConfiguration;
import software.wings.verification.stackdriver.StackDriverMetricCVConfiguration;
import software.wings.verification.stackdriver.StackDriverMetricDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CVConfigurationServiceImplTest extends WingsBaseTest {
  @Mock private FeatureFlagService featureFlagService;
  @Inject private CVConfigurationService cvConfigurationService;

  private String accountId;

  @Before
  public void setupTest() throws Exception {
    accountId = generateUuid();
    MockitoAnnotations.initMocks(this);
    when(featureFlagService.isEnabled(FeatureName.STACKDRIVER_SERVICEGUARD, accountId)).thenReturn(true);
    FieldUtils.writeField(cvConfigurationService, "featureFlagService", featureFlagService, true);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMetricTemplateAppDynamics() {
    Map<String, TimeSeriesMetricDefinition> actualDefinitions =
        cvConfigurationService.getMetricDefinitionMap(StateType.APP_DYNAMICS, null);
    Map<String, TimeSeriesMetricDefinition> expectedDefinitions =
        NewRelicMetricValueDefinition.APP_DYNAMICS_24X7_VALUES_TO_ANALYZE;
    assertThat(expectedDefinitions).isEqualTo(actualDefinitions);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMetricTemplateNewRelic() {
    Map<String, TimeSeriesMetricDefinition> actualDefinitions =
        cvConfigurationService.getMetricDefinitionMap(StateType.NEW_RELIC, null);
    assertThat(actualDefinitions).isEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMetricTemplateDynaTrace() {
    Map<String, TimeSeriesMetricDefinition> actualDefinitions =
        cvConfigurationService.getMetricDefinitionMap(StateType.DYNA_TRACE, null);
    assertThat(actualDefinitions).isEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMetricTemplatePrometheus() {
    List<TimeSeries> timeSeriesToAnalyze = new ArrayList<>();

    timeSeriesToAnalyze.add(
        TimeSeries.builder().metricName("cpu").metricType(MetricType.INFRA.name()).txnName("cpu").url("url1").build());

    timeSeriesToAnalyze.add(TimeSeries.builder()
                                .metricName("error")
                                .metricType(MetricType.ERROR.name())
                                .txnName("error")
                                .url("url2")
                                .build());

    timeSeriesToAnalyze.add(TimeSeries.builder()
                                .metricName("throughput")
                                .metricType(MetricType.THROUGHPUT.name())
                                .txnName("throughput")
                                .url("url3")
                                .build());

    PrometheusCVServiceConfiguration cvServiceConfiguration =
        PrometheusCVServiceConfiguration.builder().timeSeriesToAnalyze(timeSeriesToAnalyze).build();

    Map<String, TimeSeriesMetricDefinition> actualDefinitions =
        cvConfigurationService.getMetricDefinitionMap(StateType.PROMETHEUS, cvServiceConfiguration);
    assertThat(actualDefinitions).hasSize(3);

    timeSeriesToAnalyze.forEach(timeSeries -> {
      TimeSeriesMetricDefinition metricDefinition = actualDefinitions.get(timeSeries.getMetricName());
      assertThat(metricDefinition).isNotNull();
      assertThat(metricDefinition.getMetricName()).isEqualTo(timeSeries.getMetricName());
      assertThat(metricDefinition.getMetricType().name()).isEqualTo(timeSeries.getMetricType());
    });
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMetricTemplateDatadog() {
    Map<String, String> dockerMetrics = new HashMap<>();
    dockerMetrics.put("filter1", "docker.cpu.usage, docker.mem.rss");
    dockerMetrics.put("filter2", "docker.cpu.throttled");

    Map<String, String> ecsMetrics = new HashMap<>();
    ecsMetrics.put("filter3", "ecs.fargate.cpu.user, ecs.fargate.mem.rss, ecs.fargate.mem.usage");

    DatadogCVServiceConfiguration cvServiceConfiguration = DatadogCVServiceConfiguration.builder()
                                                               .dockerMetrics(dockerMetrics)
                                                               .ecsMetrics(ecsMetrics)
                                                               .customMetrics(new HashMap<>())
                                                               .build();

    Map<String, TimeSeriesMetricDefinition> actualDefinitions =
        cvConfigurationService.getMetricDefinitionMap(StateType.DATA_DOG, cvServiceConfiguration);

    assertThat(actualDefinitions).hasSize(6);

    List<String> expectedKeySet = Lists.newArrayList("Docker CPU Usage", "Docker RSS Memory (%)",
        "Docker CPU Throttled", "ECS Container CPU Usage", "ECS Container RSS Memory", "ECS Container Memory Usage");
    Collections.sort(expectedKeySet);

    List<String> actualKeySet = new ArrayList<>(actualDefinitions.keySet());
    Collections.sort(actualKeySet);

    assertThat(expectedKeySet).isEqualTo(actualKeySet);

    expectedKeySet.forEach(key -> {
      TimeSeriesMetricDefinition definition = actualDefinitions.get(key);

      assertThat(definition.getMetricName()).isEqualTo(key);
      assertThat(definition.getMetricType()).isEqualTo(MetricType.INFRA);
    });
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMetricsCloudWatch() {
    List<CloudWatchMetric> cloudWatchMetrics = new ArrayList<>();

    cloudWatchMetrics.add(CloudWatchMetric.builder().metricName("Latency").metricType(MetricType.ERROR.name()).build());

    cloudWatchMetrics.add(
        CloudWatchMetric.builder().metricName("RequestCount").metricType(MetricType.THROUGHPUT.name()).build());

    cloudWatchMetrics.add(
        CloudWatchMetric.builder().metricName("EstimatedProcessedBytes").metricType(MetricType.VALUE.name()).build());

    cloudWatchMetrics.add(
        CloudWatchMetric.builder().metricName("CPUReservation").metricType(MetricType.VALUE.name()).build());

    cloudWatchMetrics.add(
        CloudWatchMetric.builder().metricName("CPUUtilization").metricType(MetricType.VALUE.name()).build());

    cloudWatchMetrics.add(
        CloudWatchMetric.builder().metricName("DiskReadBytes").metricType(MetricType.VALUE.name()).build());

    Map<String, List<CloudWatchMetric>> loadBalancerMetric = new HashMap<>();
    loadBalancerMetric.put("filter1", cloudWatchMetrics.subList(0, 2));
    loadBalancerMetric.put("filter2", cloudWatchMetrics.subList(2, 3));

    Map<String, List<CloudWatchMetric>> ecsMetrics = new HashMap<>();
    ecsMetrics.put("filter3", cloudWatchMetrics.subList(3, 4));

    List<CloudWatchMetric> ec2Metrics = cloudWatchMetrics.subList(4, 6);

    CloudWatchCVServiceConfiguration cvServiceConfiguration = CloudWatchCVServiceConfiguration.builder()
                                                                  .loadBalancerMetrics(loadBalancerMetric)
                                                                  .ecsMetrics(ecsMetrics)
                                                                  .ec2Metrics(ec2Metrics)
                                                                  .build();

    Map<String, TimeSeriesMetricDefinition> actualDefinitions =
        cvConfigurationService.getMetricDefinitionMap(StateType.CLOUD_WATCH, cvServiceConfiguration);

    assertThat(actualDefinitions).hasSize(6);

    cloudWatchMetrics.forEach(metric -> {
      TimeSeriesMetricDefinition definition = actualDefinitions.get(metric.getMetricName());
      assertThat(definition.getMetricName()).isEqualTo(metric.getMetricName());
      assertThat(definition.getMetricType().name()).isEqualTo(metric.getMetricType());
    });
  }

  @Test(expected = VerificationOperationException.class)
  @Category(UnitTests.class)
  public void testInvalidCvConfig() {
    cvConfigurationService.resetBaseline(generateUuid(), generateUuid(), new LogsCVConfiguration());
  }

  @Test(expected = VerificationOperationException.class)
  @Category(UnitTests.class)
  public void testNullCvConfig() {
    final LogsCVConfiguration logsCVConfig = createLogsCVConfig(false);
    String cvConfigId = wingsPersistence.save(logsCVConfig);
    cvConfigurationService.resetBaseline(logsCVConfig.getAppId(), cvConfigId, null);
  }

  @Test(expected = VerificationOperationException.class)
  @Category(UnitTests.class)
  public void testUpdateWithNoBaselineSet() {
    final LogsCVConfiguration logsCVConfig = createLogsCVConfig(true);
    String cvConfigId = wingsPersistence.save(logsCVConfig);

    final LogsCVConfiguration updatedConfig = createLogsCVConfig(true);
    updatedConfig.setBaselineStartMinute(0);
    updatedConfig.setBaselineEndMinute(0);

    cvConfigurationService.resetBaseline(logsCVConfig.getAppId(), cvConfigId, updatedConfig);
  }

  @Test
  @Category(UnitTests.class)
  public void testCreateStackDriverMetrics() throws Exception {
    StackDriverMetricCVConfiguration configuration = createStackDriverConfig(accountId);

    String cvConfigId = cvConfigurationService.saveConfiguration(
        accountId, "LQWs27mPS7OrwCwDmT8aBA", StateType.STACK_DRIVER, configuration);

    CVConfiguration cvConfiguration = cvConfigurationService.getConfiguration(cvConfigId);
    StackDriverMetricCVConfiguration stackDriverMetricCVConfiguration =
        (StackDriverMetricCVConfiguration) cvConfiguration;
    assertThat(stackDriverMetricCVConfiguration).isNotNull();
    assertThat(stackDriverMetricCVConfiguration.getMetricDefinitions()).isNotNull();
    assertThat(stackDriverMetricCVConfiguration.getMetricDefinitions().size()).isEqualTo(3);
    assertThat(stackDriverMetricCVConfiguration.getMetricDefinitions().get(0).getFilter()).isNotBlank();
  }

  @Test(expected = VerificationOperationException.class)
  @Category(UnitTests.class)
  public void testCreateStackDriverMetricsWithInvalidFields() throws Exception {
    StackDriverMetricCVConfiguration configuration = createStackDriverConfig(accountId);
    configuration.getMetricDefinitions().get(0).setMetricType("ERROR");
    String cvConfigId = cvConfigurationService.saveConfiguration(
        accountId, "LQWs27mPS7OrwCwDmT8aBA", StateType.STACK_DRIVER, configuration);

    CVConfiguration cvConfiguration = cvConfigurationService.getConfiguration(cvConfigId);
    StackDriverMetricCVConfiguration stackDriverMetricCVConfiguration =
        (StackDriverMetricCVConfiguration) cvConfiguration;
  }

  @Test
  @Category(UnitTests.class)
  public void testResetWithBaselineSet() {
    final LogsCVConfiguration logsCVConfig = createLogsCVConfig(true);
    String cvConfigId = wingsPersistence.save(logsCVConfig);

    final LogsCVConfiguration updatedConfig = createLogsCVConfig(false);
    updatedConfig.setBaselineStartMinute(301);
    updatedConfig.setBaselineEndMinute(360);

    LogsCVConfiguration fetchedConfig = wingsPersistence.get(LogsCVConfiguration.class, cvConfigId);
    assertThat(fetchedConfig.isEnabled24x7()).isTrue();

    cvConfigId = cvConfigurationService.resetBaseline(logsCVConfig.getAppId(), cvConfigId, updatedConfig);
    fetchedConfig = wingsPersistence.get(LogsCVConfiguration.class, cvConfigId);
    assertThat(fetchedConfig.isEnabled24x7()).isTrue();
    assertThat(fetchedConfig.getBaselineStartMinute()).isEqualTo(updatedConfig.getBaselineStartMinute());
    assertThat(fetchedConfig.getBaselineEndMinute()).isEqualTo(updatedConfig.getBaselineEndMinute());
  }

  @Test
  @Category(UnitTests.class)
  public void testUpdateStackDriverMetrics() throws Exception {
    testCreateStackDriverMetrics();
    StackDriverMetricCVConfiguration configuration =
        wingsPersistence.createQuery(StackDriverMetricCVConfiguration.class)
            .filter(CVConfigurationKeys.stateType, StateType.STACK_DRIVER)
            .get();

    configuration.getMetricDefinitions().get(0).setMetricName("UpdatedMetricName");

    cvConfigurationService.updateConfiguration(
        accountId, "LQWs27mPS7OrwCwDmT8aBA", StateType.STACK_DRIVER, configuration, configuration.getUuid());

    StackDriverMetricCVConfiguration updatedConfig =
        wingsPersistence.get(StackDriverMetricCVConfiguration.class, configuration.getUuid());

    assertThat(updatedConfig).isNotNull();
    assertThat(updatedConfig.getMetricDefinitions()).isNotNull();
    assertThat(updatedConfig.getMetricDefinitions().get(0).getMetricName()).isEqualTo("UpdatedMetricName");
  }

  @Test(expected = VerificationOperationException.class)
  @Category(UnitTests.class)
  public void testUpdateStackDriverMetricsWithInvalidFields() throws Exception {
    testCreateStackDriverMetrics();
    StackDriverMetricCVConfiguration configuration =
        wingsPersistence.createQuery(StackDriverMetricCVConfiguration.class)
            .filter(CVConfigurationKeys.stateType, StateType.STACK_DRIVER)
            .get();

    configuration.getMetricDefinitions().get(0).setMetricType("THROUGHPUT");
    cvConfigurationService.updateConfiguration(
        accountId, "LQWs27mPS7OrwCwDmT8aBA", StateType.STACK_DRIVER, configuration, configuration.getUuid());
  }

  @Test
  @Category(UnitTests.class)
  public void testResetBaselineWithData() {
    final LogsCVConfiguration logsCVConfig = createLogsCVConfig(true);
    String cvConfigId = wingsPersistence.save(logsCVConfig);
    int numOfAnalysisRecords = 17;
    for (int i = 0; i < numOfAnalysisRecords * CRON_POLL_INTERVAL_IN_MINUTES; i += CRON_POLL_INTERVAL_IN_MINUTES) {
      wingsPersistence.save(LogMLAnalysisRecord.builder().logCollectionMinute(i).cvConfigId(cvConfigId).build());
      wingsPersistence.save(LearningEngineAnalysisTask.builder().cvConfigId(cvConfigId).build());
    }

    assertThat(wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority).count())
        .isEqualTo(numOfAnalysisRecords);
    assertThat(wingsPersistence.createQuery(LogMLAnalysisRecord.class, excludeAuthority).count())
        .isEqualTo(numOfAnalysisRecords);
    int numOfAnalysisToKeep = 8;

    final LogsCVConfiguration updatedConfig = createLogsCVConfig(true);
    updatedConfig.setBaselineStartMinute(numOfAnalysisToKeep * CRON_POLL_INTERVAL_IN_MINUTES);
    updatedConfig.setBaselineEndMinute(numOfAnalysisRecords * CRON_POLL_INTERVAL_IN_MINUTES);

    String newCvConfigId = cvConfigurationService.resetBaseline(logsCVConfig.getAppId(), cvConfigId, updatedConfig);
    LogsCVConfiguration fetchedConfig = wingsPersistence.get(LogsCVConfiguration.class, newCvConfigId);
    assertThat(fetchedConfig.isEnabled24x7()).isTrue();
    assertThat(fetchedConfig.getBaselineStartMinute()).isEqualTo(updatedConfig.getBaselineStartMinute());
    assertThat(fetchedConfig.getBaselineEndMinute()).isEqualTo(updatedConfig.getBaselineEndMinute());

    assertThat(wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority).count()).isEqualTo(0);
    assertThat(wingsPersistence.createQuery(LogMLAnalysisRecord.class, excludeAuthority).count())
        .isEqualTo(numOfAnalysisToKeep);

    sleep(ofSeconds(2));
    wingsPersistence.createQuery(LogMLAnalysisRecord.class, excludeAuthority).asList().forEach(logMLAnalysisRecord -> {
      assertThat(logMLAnalysisRecord.getCvConfigId()).isEqualTo(newCvConfigId);
      assertThat(logMLAnalysisRecord.isDeprecated()).isTrue();
    });
  }

  @Test(expected = UnsupportedOperationException.class)
  @Category(UnitTests.class)
  public void testStackDriverFeatureFlagOff() throws Exception {
    when(featureFlagService.isEnabled(any(), anyString())).thenReturn(false);
    testCreateStackDriverMetrics();
  }

  @Test
  @Category(UnitTests.class)
  public void testValidateDefinitionsForMetricStackdriver() throws Exception {
    StackDriverMetricCVConfiguration configuration = createStackDriverConfig(accountId);
    configuration.setMetricFilters();
    Map<String, String> invalidFields =
        StackDriverState.validateMetricDefinitions(configuration.getMetricDefinitions(), true);
    assertThat(invalidFields).isEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void testValidateDefinitionsForMetricStackdriverInvalidSetup() throws Exception {
    StackDriverMetricCVConfiguration configuration = createStackDriverConfig(accountId);
    configuration.getMetricDefinitions().get(0).setMetricType("ERROR");
    configuration.setMetricFilters();
    Map<String, String> invalidFields =
        StackDriverState.validateMetricDefinitions(configuration.getMetricDefinitions(), true);
    assertThat(invalidFields).isNotEmpty();
    assertThat(invalidFields.size()).isEqualTo(1);
  }

  @Test
  @Category(UnitTests.class)
  public void testValidateDefinitionsForMetricStackdriverNoFilterJson() throws Exception {
    StackDriverMetricCVConfiguration configuration = createStackDriverConfig(accountId);
    Map<String, String> invalidFields =
        StackDriverState.validateMetricDefinitions(configuration.getMetricDefinitions(), true);
    assertThat(invalidFields).isNotEmpty();
    assertThat(invalidFields.size()).isEqualTo(1);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMetricTypeForMetricStackdriver() throws Exception {
    StackDriverMetricCVConfiguration configuration = createStackDriverConfig(accountId);
    String metricType = StackDriverState.getMetricTypeForMetric(configuration, "metricName");
    assertThat(metricType).isEqualTo(MetricType.INFRA.name());
  }

  public static StackDriverMetricCVConfiguration createStackDriverConfig(String accountId) throws Exception {
    String paramsForStackDriver = Resources.toString(
        CVConfigurationServiceImplTest.class.getResource("/apm/stackdriverpayload.json"), Charsets.UTF_8);
    StackDriverMetricDefinition definition = StackDriverMetricDefinition.builder()
                                                 .filterJson(paramsForStackDriver)
                                                 .metricName("metricName")
                                                 .metricType("INFRA")
                                                 .txnName("TransactionName1")
                                                 .build();
    StackDriverMetricDefinition definition1 = StackDriverMetricDefinition.builder()
                                                  .filterJson(paramsForStackDriver)
                                                  .metricName("metricName2")
                                                  .metricType("INFRA")
                                                  .txnName("TransactionName2")
                                                  .build();
    StackDriverMetricDefinition definition2 = StackDriverMetricDefinition.builder()
                                                  .filterJson(paramsForStackDriver)
                                                  .metricName("metricName3")
                                                  .metricType("INFRA")
                                                  .txnName("TransactionName2")
                                                  .build();
    StackDriverMetricCVConfiguration configuration =
        StackDriverMetricCVConfiguration.builder()
            .metricDefinitions(Arrays.asList(definition, definition1, definition2))
            .build();
    configuration.setAccountId(accountId);
    configuration.setStateType(StateType.STACK_DRIVER);
    configuration.setEnvId(generateUuid());
    configuration.setName("StackDriver");
    configuration.setConnectorId(generateUuid());
    configuration.setServiceId(generateUuid());

    return configuration;
  }

  private LogsCVConfiguration createLogsCVConfig(boolean enabled24x7) {
    LogsCVConfiguration logsCVConfiguration = new LogsCVConfiguration();
    logsCVConfiguration.setName("Config 1");
    logsCVConfiguration.setAppId(generateUuid());
    logsCVConfiguration.setEnvId(generateUuid());
    logsCVConfiguration.setServiceId(generateUuid());
    logsCVConfiguration.setEnabled24x7(enabled24x7);
    logsCVConfiguration.setConnectorId(generateUuid());
    logsCVConfiguration.setBaselineStartMinute(100);
    logsCVConfiguration.setBaselineEndMinute(200);
    logsCVConfiguration.setAlertEnabled(false);
    logsCVConfiguration.setAlertThreshold(0.1);
    logsCVConfiguration.setQuery(generateUuid());
    logsCVConfiguration.setStateType(StateType.SUMO);
    return logsCVConfiguration;
  }
}