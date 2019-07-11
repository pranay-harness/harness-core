package software.wings.integration.verification;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.sm.StateType.APP_DYNAMICS;
import static software.wings.sm.StateType.BUG_SNAG;
import static software.wings.sm.StateType.CLOUD_WATCH;
import static software.wings.sm.StateType.DATA_DOG;
import static software.wings.sm.StateType.DYNA_TRACE;
import static software.wings.sm.StateType.ELK;
import static software.wings.sm.StateType.NEW_RELIC;
import static software.wings.sm.StateType.PROMETHEUS;
import static software.wings.sm.StateType.STACK_DRIVER_LOG;
import static software.wings.sm.StateType.SUMO;
import static software.wings.utils.WingsTestConstants.mockChecker;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import io.harness.limits.LimitCheckerFactory;
import io.harness.rest.RestResponse;
import io.harness.rule.RepeatRule.Repeat;
import io.harness.serializer.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.beans.ElkConfig;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Builder;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.metrics.MetricType;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogDataRecord.LogDataRecordKeys;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRecord.LogMLAnalysisRecordKeys;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask.LearningEngineAnalysisTaskKeys;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.CVConfiguration.CVConfigurationKeys;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;
import software.wings.verification.cloudwatch.CloudWatchCVServiceConfiguration;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;
import software.wings.verification.dynatrace.DynaTraceCVServiceConfiguration;
import software.wings.verification.log.BugsnagCVConfiguration;
import software.wings.verification.log.ElkCVConfiguration;
import software.wings.verification.log.LogsCVConfiguration;
import software.wings.verification.log.StackdriverCVConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;
import software.wings.verification.prometheus.PrometheusCVServiceConfiguration;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * @author Vaibhav Tulsyan
 * 05/Oct/2018
 */
@Slf4j
public class CVConfigurationIntegrationTest extends BaseIntegrationTest {
  @Rule public ExpectedException thrown = ExpectedException.none();
  private String appId, envId, serviceId, appDynamicsApplicationId;
  @Inject private WingsPersistence wingsPersistence;
  @Inject @InjectMocks private AppService appService;
  @Inject @InjectMocks private EnvironmentService environmentService;
  @Mock private LimitCheckerFactory limitCheckerFactory;
  private NewRelicCVServiceConfiguration newRelicCVServiceConfiguration;
  private AppDynamicsCVServiceConfiguration appDynamicsCVServiceConfiguration;
  private DynaTraceCVServiceConfiguration dynaTraceCVServiceConfiguration;
  private PrometheusCVServiceConfiguration prometheusCVServiceConfiguration;
  private DatadogCVServiceConfiguration datadogCVServiceConfiguration;
  private CloudWatchCVServiceConfiguration cloudWatchCVServiceConfiguration;
  private ElkCVConfiguration elkCVConfiguration;
  private LogsCVConfiguration logsCVConfiguration;
  private BugsnagCVConfiguration bugsnagCVConfiguration;
  private StackdriverCVConfiguration stackdriverCVConfiguration;

  private SettingAttribute settingAttribute;
  private String settingAttributeId;
  private Service service;

  @Before
  public void setUp() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    loginAdminUser();

    appId = appService.save(anApplication().name(generateUuid()).accountId(accountId).build()).getUuid();
    envId = environmentService.save(Environment.Builder.anEnvironment().appId(appId).name("Developmenet").build())
                .getUuid();
    appDynamicsApplicationId = generateUuid();

    settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                           .withAccountId(accountId)
                           .withName("someSettingAttributeName")
                           .withCategory(SettingCategory.CONNECTOR)
                           .withEnvId(envId)
                           .withAppId(appId)
                           .build();
    settingAttributeId = wingsPersistence.saveAndGet(SettingAttribute.class, settingAttribute).getUuid();

    service = Service.builder().name("someServiceName").appId(appId).build();
    serviceId = wingsPersistence.saveAndGet(Service.class, service).getUuid();

    createNewRelicConfig(true);
    createAppDynamicsConfig();
    createDynaTraceConfig();
    createPrometheusConfig();
    createDatadogConfig();
    createCloudWatchConfig();
    createElkCVConfig(true);
    createLogsCVConfig(true);
    createBugSnagCVConfig(true);
    createStackdriverCVConfig(true);
  }

  private void createCloudWatchConfig() {
    cloudWatchCVServiceConfiguration = new CloudWatchCVServiceConfiguration();
    cloudWatchCVServiceConfiguration.setName("Config 1");
    cloudWatchCVServiceConfiguration.setAppId(appId);
    cloudWatchCVServiceConfiguration.setEnvId(envId);
    cloudWatchCVServiceConfiguration.setServiceId(serviceId);
    cloudWatchCVServiceConfiguration.setEnabled24x7(true);
    cloudWatchCVServiceConfiguration.setConnectorId(settingAttributeId);
    cloudWatchCVServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);

    Map<String, List<CloudWatchMetric>> loadBalancerMetricsByLoadBalancer = new HashMap<>();
    List<CloudWatchMetric> loadBalancerMetrics = new ArrayList<>();
    loadBalancerMetrics.add(
        new CloudWatchMetric("Latency", "Latenc", "LoadBalancerName", "Load balancer name", "ERROR", true));
    loadBalancerMetricsByLoadBalancer.put("init-test", loadBalancerMetrics);
    cloudWatchCVServiceConfiguration.setLoadBalancerMetrics(loadBalancerMetricsByLoadBalancer);
    cloudWatchCVServiceConfiguration.setRegion("us-east-2");

    Map<String, List<CloudWatchMetric>> metricsByLambdaFunction = new HashMap<>();
    List<CloudWatchMetric> lambdaMetrics = new ArrayList<>();
    lambdaMetrics.add(new CloudWatchMetric(
        "Invocations", "Invocations Sum", "FunctionName", "Lambda Function Name", "THROUGHPUT", true));
    metricsByLambdaFunction.put("lambda_fn1", lambdaMetrics);

    cloudWatchCVServiceConfiguration.setLambdaFunctionsMetrics(metricsByLambdaFunction);
  }

  private void createNewRelicConfig(boolean enabled24x7) {
    String newRelicApplicationId = generateUuid();

    newRelicCVServiceConfiguration = new NewRelicCVServiceConfiguration();
    newRelicCVServiceConfiguration.setName("Config 1");
    newRelicCVServiceConfiguration.setAppId(appId);
    newRelicCVServiceConfiguration.setEnvId(envId);
    newRelicCVServiceConfiguration.setServiceId(serviceId);
    newRelicCVServiceConfiguration.setEnabled24x7(enabled24x7);
    newRelicCVServiceConfiguration.setApplicationId(newRelicApplicationId);
    newRelicCVServiceConfiguration.setConnectorId(settingAttributeId);
    newRelicCVServiceConfiguration.setMetrics(Collections.singletonList("apdexScore"));
    newRelicCVServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);
  }

  private void createAppDynamicsConfig() {
    appDynamicsCVServiceConfiguration = new AppDynamicsCVServiceConfiguration();
    appDynamicsCVServiceConfiguration.setAppId(appId);
    appDynamicsCVServiceConfiguration.setEnvId(envId);
    appDynamicsCVServiceConfiguration.setServiceId(serviceId);
    appDynamicsCVServiceConfiguration.setEnabled24x7(true);
    appDynamicsCVServiceConfiguration.setAppDynamicsApplicationId(appDynamicsApplicationId);
    appDynamicsCVServiceConfiguration.setTierId(generateUuid());
    appDynamicsCVServiceConfiguration.setConnectorId(generateUuid());
    appDynamicsCVServiceConfiguration.setStateType(APP_DYNAMICS);
    appDynamicsCVServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.HIGH);
  }

  private void createDynaTraceConfig() {
    dynaTraceCVServiceConfiguration = new DynaTraceCVServiceConfiguration();
    dynaTraceCVServiceConfiguration.setAppId(appId);
    dynaTraceCVServiceConfiguration.setEnvId(envId);
    dynaTraceCVServiceConfiguration.setServiceId(serviceId);
    dynaTraceCVServiceConfiguration.setEnabled24x7(true);
    dynaTraceCVServiceConfiguration.setServiceMethods("SERVICE_METHOD-991CE862F114C79F\n"
        + "SERVICE_METHOD-65C2EED098275731\n"
        + "SERVICE_METHOD-9D3499F155C8070D\n"
        + "SERVICE_METHOD-AECEC4A5C7E348EC\n"
        + "SERVICE_METHOD-9ACB771237BE05C6\n"
        + "SERVICE_METHOD-DA487A489220E53D");
    dynaTraceCVServiceConfiguration.setConnectorId(generateUuid());
    dynaTraceCVServiceConfiguration.setStateType(APP_DYNAMICS);
    dynaTraceCVServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.HIGH);
  }

  private void createPrometheusConfig() {
    List<TimeSeries> timeSeries = Lists.newArrayList(
        TimeSeries.builder()
            .txnName("Hardware")
            .metricName("CPU")
            .metricType(MetricType.ERROR.name())
            .url(
                "/api/v1/query_range?start=$startTime&end=$endTime&step=60s&query=container_cpu_usage_seconds_total{pod_name=\"$hostName\"}")
            .build());

    prometheusCVServiceConfiguration = new PrometheusCVServiceConfiguration();
    prometheusCVServiceConfiguration.setAppId(appId);
    prometheusCVServiceConfiguration.setEnvId(envId);
    prometheusCVServiceConfiguration.setServiceId(serviceId);
    prometheusCVServiceConfiguration.setEnabled24x7(true);
    prometheusCVServiceConfiguration.setTimeSeriesToAnalyze(timeSeries);
    prometheusCVServiceConfiguration.setConnectorId(generateUuid());
    prometheusCVServiceConfiguration.setStateType(PROMETHEUS);
  }

  private void createDatadogConfig() {
    datadogCVServiceConfiguration = new DatadogCVServiceConfiguration();
    datadogCVServiceConfiguration.setName("datadog config");
    datadogCVServiceConfiguration.setAppId(appId);
    datadogCVServiceConfiguration.setEnvId(envId);
    datadogCVServiceConfiguration.setServiceId(serviceId);
    datadogCVServiceConfiguration.setEnabled24x7(true);
    datadogCVServiceConfiguration.setConnectorId(generateUuid());
    datadogCVServiceConfiguration.setStateType(DATA_DOG);
    datadogCVServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.HIGH);
    datadogCVServiceConfiguration.setDatadogServiceName(generateUuid());

    Map<String, String> dockerMetrics = new HashMap<>();
    dockerMetrics.put("service_name:harness", "docker.cpu.usage, docker.mem.rss");
    datadogCVServiceConfiguration.setDockerMetrics(dockerMetrics);
  }

  private void createElkCVConfig(boolean enabled24x7) {
    String elkSettingId =
        wingsPersistence.save(Builder.aSettingAttribute()
                                  .withName(generateUuid())
                                  .withAccountId(accountId)
                                  .withValue(ElkConfig.builder()
                                                 .elkConnector(ElkConnector.ELASTIC_SEARCH_SERVER)
                                                 .elkUrl("http://ec2-34-227-84-170.compute-1.amazonaws.com:9200/")
                                                 .accountId(accountId)
                                                 .build())
                                  .build());
    elkCVConfiguration = new ElkCVConfiguration();
    elkCVConfiguration.setName("Config 1");
    elkCVConfiguration.setAppId(appId);
    elkCVConfiguration.setEnvId(envId);
    elkCVConfiguration.setServiceId(serviceId);
    elkCVConfiguration.setEnabled24x7(enabled24x7);
    elkCVConfiguration.setConnectorId(elkSettingId);
    elkCVConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);
    elkCVConfiguration.setBaselineStartMinute(100);
    elkCVConfiguration.setBaselineEndMinute(200);

    elkCVConfiguration.setQuery("query1");
    elkCVConfiguration.setQueryType(ElkQueryType.TERM);
    elkCVConfiguration.setIndex("filebeat-*");
    elkCVConfiguration.setHostnameField("host1");
    elkCVConfiguration.setMessageField("message1");
    elkCVConfiguration.setTimestampField("timestamp1");
    elkCVConfiguration.setTimestampFormat("timestamp_format1");
  }

  private void createLogsCVConfig(boolean enabled24x7) {
    logsCVConfiguration = new LogsCVConfiguration();
    logsCVConfiguration.setName("Config 1");
    logsCVConfiguration.setAppId(appId);
    logsCVConfiguration.setEnvId(envId);
    logsCVConfiguration.setServiceId(serviceId);
    logsCVConfiguration.setEnabled24x7(enabled24x7);
    logsCVConfiguration.setConnectorId(settingAttributeId);
    logsCVConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);
    logsCVConfiguration.setBaselineStartMinute(100);
    logsCVConfiguration.setBaselineEndMinute(200);
    logsCVConfiguration.setAlertEnabled(false);
    logsCVConfiguration.setAlertThreshold(0.1);

    logsCVConfiguration.setQuery("query1");
  }

  private void createBugSnagCVConfig(boolean enabled24x7) {
    bugsnagCVConfiguration = new BugsnagCVConfiguration();
    bugsnagCVConfiguration.setName("Config 1");
    bugsnagCVConfiguration.setAppId(appId);
    bugsnagCVConfiguration.setEnvId(envId);
    bugsnagCVConfiguration.setServiceId(serviceId);
    bugsnagCVConfiguration.setEnabled24x7(enabled24x7);
    bugsnagCVConfiguration.setConnectorId(settingAttributeId);
    bugsnagCVConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);
    bugsnagCVConfiguration.setBaselineStartMinute(100);
    bugsnagCVConfiguration.setBaselineEndMinute(200);
    bugsnagCVConfiguration.setAlertEnabled(false);
    bugsnagCVConfiguration.setAlertThreshold(0.1);
    bugsnagCVConfiguration.setProjectId("development");
    bugsnagCVConfiguration.setOrgId("Harness");
    bugsnagCVConfiguration.setBrowserApplication(true);

    bugsnagCVConfiguration.setQuery("*exception*");
  }

  private void createStackdriverCVConfig(boolean enabled24x7) {
    stackdriverCVConfiguration = new StackdriverCVConfiguration();
    stackdriverCVConfiguration.setQuery("*exception*");
    stackdriverCVConfiguration.setName("Config 1");
    stackdriverCVConfiguration.setAppId(appId);
    stackdriverCVConfiguration.setEnvId(envId);
    stackdriverCVConfiguration.setServiceId(serviceId);
    stackdriverCVConfiguration.setEnabled24x7(enabled24x7);
    stackdriverCVConfiguration.setConnectorId(settingAttributeId);
    stackdriverCVConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);
    stackdriverCVConfiguration.setBaselineStartMinute(100);
    stackdriverCVConfiguration.setBaselineEndMinute(200);
    stackdriverCVConfiguration.setAlertEnabled(false);
    stackdriverCVConfiguration.setAlertThreshold(0.1);
    stackdriverCVConfiguration.setStateType(STACK_DRIVER_LOG);
  }

  @Test
  @Category(IntegrationTests.class)
  public void testSaveConfiguration() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    String id = wingsPersistence.save(newRelicCVServiceConfiguration);
    NewRelicCVServiceConfiguration obj = wingsPersistence.get(NewRelicCVServiceConfiguration.class, id);
    assertEquals(id, obj.getUuid());
  }

  @Test
  @Category(IntegrationTests.class)
  public <T extends CVConfiguration> void testNewRelicConfiguration() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    String url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + NEW_RELIC;
    logger.info("POST " + url);
    WebTarget target = client.target(url);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(newRelicCVServiceConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    String savedObjectUuid = restResponse.getResource();

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;

    target = client.target(url);
    RestResponse<NewRelicCVServiceConfiguration> getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<NewRelicCVServiceConfiguration>>() {});
    NewRelicCVServiceConfiguration fetchedObject = getRequestResponse.getResource();

    NewRelicCVServiceConfiguration newRelicCVServiceConfiguration = fetchedObject;
    assertEquals(savedObjectUuid, fetchedObject.getUuid());
    assertEquals(accountId, fetchedObject.getAccountId());
    assertEquals(appId, fetchedObject.getAppId());
    assertEquals(envId, fetchedObject.getEnvId());
    assertEquals(serviceId, fetchedObject.getServiceId());
    assertEquals(NEW_RELIC, fetchedObject.getStateType());
    assertEquals(AnalysisTolerance.MEDIUM, fetchedObject.getAnalysisTolerance());
    assertEquals("someSettingAttributeName", newRelicCVServiceConfiguration.getConnectorName());
    assertEquals("someServiceName", newRelicCVServiceConfiguration.getServiceName());

    url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId;
    target = client.target(url);

    RestResponse<List<Object>> allConfigResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<Object>>>() {});
    List<Object> allConifgs = allConfigResponse.getResource();

    assertEquals(1, allConifgs.size());

    NewRelicCVServiceConfiguration obj =
        JsonUtils.asObject(JsonUtils.asJson(allConifgs.get(0)), NewRelicCVServiceConfiguration.class);

    assertEquals(savedObjectUuid, obj.getUuid());
    assertEquals(accountId, obj.getAccountId());
    assertEquals(appId, obj.getAppId());
    assertEquals(envId, obj.getEnvId());
    assertEquals(serviceId, obj.getServiceId());
    assertEquals(NEW_RELIC, obj.getStateType());
    assertEquals(AnalysisTolerance.MEDIUM, obj.getAnalysisTolerance());
    assertEquals("Config 1", obj.getName());

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId + "&appId=" + appId
        + "&stateType=" + NEW_RELIC + "&serviceConfigurationId=" + savedObjectUuid;
    target = client.target(url);
    newRelicCVServiceConfiguration.setName("Config 2");
    newRelicCVServiceConfiguration.setEnabled24x7(false);
    newRelicCVServiceConfiguration.setMetrics(Collections.singletonList("requestsPerMinute"));
    newRelicCVServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.LOW);
    getRequestBuilderWithAuthHeader(target).put(
        entity(newRelicCVServiceConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<NewRelicCVServiceConfiguration>>() {});
    fetchedObject = getRequestResponse.getResource();
    assertFalse(fetchedObject.isEnabled24x7());
    assertEquals(AnalysisTolerance.LOW, fetchedObject.getAnalysisTolerance());
    assertEquals("Config 2", fetchedObject.getName());

    String delete_url =
        API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId + "&appId=" + appId;
    target = client.target(delete_url);
    RestResponse<Boolean> response = getRequestBuilderWithAuthHeader(target).delete(new GenericType<RestResponse>() {});
    assertEquals(true, response.getResource());

    delete_url =
        API_BASE + "/cv-configuration/" + UUID.randomUUID().toString() + "?accountId=" + accountId + "&appId=" + appId;
    target = client.target(delete_url);
    response = getRequestBuilderWithAuthHeader(target).delete(new GenericType<RestResponse>() {});
    assertEquals(false, response.getResource());
  }

  @Test
  @Category(IntegrationTests.class)
  public <T extends CVConfiguration> void testAppDynamicsConfiguration() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    String url =
        API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + APP_DYNAMICS;
    logger.info("POST " + url);
    WebTarget target = client.target(url);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(appDynamicsCVServiceConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    String savedObjectUuid = restResponse.getResource();

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;

    target = client.target(url);

    RestResponse<AppDynamicsCVServiceConfiguration> getRequestResponse = getRequestBuilderWithAuthHeader(target).get(
        new GenericType<RestResponse<AppDynamicsCVServiceConfiguration>>() {});
    AppDynamicsCVServiceConfiguration fetchedObject = getRequestResponse.getResource();
    assertEquals(savedObjectUuid, fetchedObject.getUuid());
    assertEquals(accountId, fetchedObject.getAccountId());
    assertEquals(appId, fetchedObject.getAppId());
    assertEquals(envId, fetchedObject.getEnvId());
    assertEquals(serviceId, fetchedObject.getServiceId());
    assertEquals(APP_DYNAMICS, fetchedObject.getStateType());
    assertEquals(appDynamicsApplicationId, fetchedObject.getAppDynamicsApplicationId());
    assertEquals(AnalysisTolerance.HIGH, fetchedObject.getAnalysisTolerance());
  }

  @Test
  @Category(IntegrationTests.class)
  public <T extends CVConfiguration> void testDatadogConfiguration() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    String url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + DATA_DOG;
    WebTarget target = client.target(url);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(datadogCVServiceConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    String savedObjectUuid = restResponse.getResource();
    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;
    target = client.target(url);
    RestResponse<DatadogCVServiceConfiguration> getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<DatadogCVServiceConfiguration>>() {});
    DatadogCVServiceConfiguration fetchedObject = getRequestResponse.getResource();
    assertEquals(savedObjectUuid, fetchedObject.getUuid());
    assertEquals(accountId, fetchedObject.getAccountId());
    assertEquals(appId, fetchedObject.getAppId());
    assertEquals(envId, fetchedObject.getEnvId());
    assertEquals(serviceId, fetchedObject.getServiceId());
    assertEquals(DATA_DOG, fetchedObject.getStateType());
    assertEquals(AnalysisTolerance.HIGH, fetchedObject.getAnalysisTolerance());
    assertEquals("docker.cpu.usage, docker.mem.rss", fetchedObject.getDockerMetrics().values().iterator().next());

    // Test PUT API for Datadog
    datadogCVServiceConfiguration.setName("Datadog Config");
    datadogCVServiceConfiguration.setEnabled24x7(false);
    datadogCVServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);
    Map<String, String> dockerMetrics = new HashMap<>();
    dockerMetrics.put("service_name:harness", "docker.cpu.throttled");
    datadogCVServiceConfiguration.setDockerMetrics(dockerMetrics);

    // Call PUT
    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId + "&appId=" + appId
        + "&stateType=" + DATA_DOG + "&serviceConfigurationId=" + savedObjectUuid;
    logger.info("PUT " + url);
    target = client.target(url);
    getRequestBuilderWithAuthHeader(target).put(
        entity(datadogCVServiceConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});

    // Call GET
    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;
    target = client.target(url);
    getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<DatadogCVServiceConfiguration>>() {});
    fetchedObject = getRequestResponse.getResource();

    // Assert
    assertEquals("Datadog Config", fetchedObject.getName());
    assertFalse(fetchedObject.isEnabled24x7());
    assertEquals("docker.cpu.throttled", fetchedObject.getDockerMetrics().values().iterator().next());
    assertEquals(AnalysisTolerance.MEDIUM, fetchedObject.getAnalysisTolerance());
  }

  @Test
  @Category(IntegrationTests.class)
  public <T extends CVConfiguration> void testPrometheusConfiguration() {
    String url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + PROMETHEUS;
    logger.info("POST " + url);
    WebTarget target = client.target(url);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(prometheusCVServiceConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    String savedObjectUuid = restResponse.getResource();

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;

    target = client.target(url);
    RestResponse<PrometheusCVServiceConfiguration> getRequestResponse = getRequestBuilderWithAuthHeader(target).get(
        new GenericType<RestResponse<PrometheusCVServiceConfiguration>>() {});
    PrometheusCVServiceConfiguration fetchedObject = getRequestResponse.getResource();
    assertEquals(savedObjectUuid, fetchedObject.getUuid());
    assertEquals(accountId, fetchedObject.getAccountId());
    assertEquals(appId, fetchedObject.getAppId());
    assertEquals(envId, fetchedObject.getEnvId());
    assertEquals(serviceId, fetchedObject.getServiceId());
    assertEquals(PROMETHEUS, fetchedObject.getStateType());
    assertEquals(prometheusCVServiceConfiguration.getTimeSeriesToAnalyze(), fetchedObject.getTimeSeriesToAnalyze());
  }

  @Test
  @Category(IntegrationTests.class)
  public <T extends CVConfiguration> void testDynaTraceConfiguration() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    String url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + DYNA_TRACE;
    logger.info("POST " + url);
    WebTarget target = client.target(url);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(dynaTraceCVServiceConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    String savedObjectUuid = restResponse.getResource();

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;

    target = client.target(url);
    RestResponse<DynaTraceCVServiceConfiguration> getRequestResponse = getRequestBuilderWithAuthHeader(target).get(
        new GenericType<RestResponse<DynaTraceCVServiceConfiguration>>() {});
    DynaTraceCVServiceConfiguration fetchedObject = getRequestResponse.getResource();
    assertEquals(savedObjectUuid, fetchedObject.getUuid());
    assertEquals(accountId, fetchedObject.getAccountId());
    assertEquals(appId, fetchedObject.getAppId());
    assertEquals(envId, fetchedObject.getEnvId());
    assertEquals(serviceId, fetchedObject.getServiceId());
    assertEquals(DYNA_TRACE, fetchedObject.getStateType());
    assertEquals(dynaTraceCVServiceConfiguration.getServiceMethods(), fetchedObject.getServiceMethods());
    assertEquals(AnalysisTolerance.HIGH, fetchedObject.getAnalysisTolerance());
  }

  @Test
  @Category(IntegrationTests.class)
  public <T extends CVConfiguration> void testCloudWatchConfiguration() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    String url =
        API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + CLOUD_WATCH;
    logger.info("POST " + url);
    WebTarget target = client.target(url);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(cloudWatchCVServiceConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    String savedObjectUuid = restResponse.getResource();

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;

    target = client.target(url);
    RestResponse<CloudWatchCVServiceConfiguration> getRequestResponse = getRequestBuilderWithAuthHeader(target).get(
        new GenericType<RestResponse<CloudWatchCVServiceConfiguration>>() {});
    CloudWatchCVServiceConfiguration fetchedObject = getRequestResponse.getResource();
    assertEquals(savedObjectUuid, fetchedObject.getUuid());
    assertEquals(accountId, fetchedObject.getAccountId());
    assertEquals(appId, fetchedObject.getAppId());
    assertEquals(envId, fetchedObject.getEnvId());
    assertEquals(serviceId, fetchedObject.getServiceId());
    assertEquals(CLOUD_WATCH, fetchedObject.getStateType());
    assertEquals(cloudWatchCVServiceConfiguration.getLoadBalancerMetrics(), fetchedObject.getLoadBalancerMetrics());
  }

  @Test
  @Category(IntegrationTests.class)
  public <T extends CVConfiguration> void testCloudWatchConfigurationNoMetricsShouldFail() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    String url =
        API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + CLOUD_WATCH;
    logger.info("POST " + url);
    WebTarget target = client.target(url);

    cloudWatchCVServiceConfiguration.setLambdaFunctionsMetrics(null);
    cloudWatchCVServiceConfiguration.setLoadBalancerMetrics(null);
    cloudWatchCVServiceConfiguration.setEc2InstanceNames(null);
    cloudWatchCVServiceConfiguration.setEcsMetrics(null);
    cloudWatchCVServiceConfiguration.setEc2Metrics(null);
    thrown.expect(Exception.class);
    getRequestBuilderWithAuthHeader(target).post(
        entity(cloudWatchCVServiceConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
  }

  @Test
  @Category(IntegrationTests.class)
  public <T extends CVConfiguration> void testCloudWatchConfigurationWithoutAnyMetric() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    String url =
        API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + CLOUD_WATCH;
    logger.info("POST " + url);
    WebTarget target = client.target(url);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(cloudWatchCVServiceConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    String savedObjectUuid = restResponse.getResource();

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;

    target = client.target(url);
    RestResponse<CloudWatchCVServiceConfiguration> getRequestResponse = getRequestBuilderWithAuthHeader(target).get(
        new GenericType<RestResponse<CloudWatchCVServiceConfiguration>>() {});
    CloudWatchCVServiceConfiguration fetchedObject = getRequestResponse.getResource();

    fetchedObject.setEcsMetrics(null);
    fetchedObject.setEc2InstanceNames(null);
    fetchedObject.setLoadBalancerMetrics(null);
    fetchedObject.setLambdaFunctionsMetrics(null);

    // Call PUT
    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId + "&appId=" + appId
        + "&stateType=" + CLOUD_WATCH + "&serviceConfigurationId=" + savedObjectUuid;
    logger.info("PUT " + url);
    target = client.target(url);
    thrown.expect(Exception.class);
    getRequestBuilderWithAuthHeader(target).put(
        entity(fetchedObject, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    restResponse.getResource();
  }

  @Test
  @Category(IntegrationTests.class)
  public <T extends CVConfiguration> void testCloudWatchConfigurationWithOnlyLamdaMetrics() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    String url =
        API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + CLOUD_WATCH;
    logger.info("POST " + url);
    WebTarget target = client.target(url);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(cloudWatchCVServiceConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    String savedObjectUuid = restResponse.getResource();

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;

    target = client.target(url);
    RestResponse<CloudWatchCVServiceConfiguration> getRequestResponse = getRequestBuilderWithAuthHeader(target).get(
        new GenericType<RestResponse<CloudWatchCVServiceConfiguration>>() {});
    CloudWatchCVServiceConfiguration fetchedObject = getRequestResponse.getResource();

    fetchedObject.setEcsMetrics(null);
    fetchedObject.setEc2InstanceNames(null);
    fetchedObject.setLoadBalancerMetrics(null);

    // Call PUT
    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId + "&appId=" + appId
        + "&stateType=" + CLOUD_WATCH + "&serviceConfigurationId=" + savedObjectUuid;
    logger.info("PUT " + url);
    target = client.target(url);
    getRequestBuilderWithAuthHeader(target).put(
        entity(fetchedObject, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    assertNotNull(restResponse.getResource());
  }

  @Test
  @Category(IntegrationTests.class)
  public <T extends CVConfiguration> void testElkConfiguration() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    String url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + ELK;
    logger.info("POST " + url);
    WebTarget target = client.target(url);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(elkCVConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    String savedObjectUuid = restResponse.getResource();

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;

    target = client.target(url);
    RestResponse<ElkCVConfiguration> getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<ElkCVConfiguration>>() {});
    ElkCVConfiguration fetchedObject = getRequestResponse.getResource();

    ElkCVConfiguration elkCVServiceConfiguration = fetchedObject;
    assertEquals(savedObjectUuid, fetchedObject.getUuid());
    assertEquals(accountId, fetchedObject.getAccountId());
    assertEquals(appId, fetchedObject.getAppId());
    assertEquals(envId, fetchedObject.getEnvId());
    assertEquals(serviceId, fetchedObject.getServiceId());
    assertEquals(ELK, fetchedObject.getStateType());
    assertEquals(AnalysisTolerance.MEDIUM, fetchedObject.getAnalysisTolerance());
    assertEquals("someServiceName", elkCVServiceConfiguration.getServiceName());
    assertEquals(91, fetchedObject.getBaselineStartMinute());
    assertEquals(195, fetchedObject.getBaselineEndMinute());

    assertEquals("query1", fetchedObject.getQuery());
    assertEquals(ElkQueryType.TERM, fetchedObject.getQueryType());
    assertEquals("filebeat-*", fetchedObject.getIndex());
    assertEquals("host1", fetchedObject.getHostnameField());
    assertEquals("message1", fetchedObject.getMessageField());
    assertEquals("timestamp1", fetchedObject.getTimestampField());
    assertEquals("timestamp_format1", fetchedObject.getTimestampFormat());

    url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId;
    target = client.target(url);

    RestResponse<List<Object>> allConfigResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<Object>>>() {});
    List<Object> allConifgs = allConfigResponse.getResource();

    assertEquals(1, allConifgs.size());

    ElkCVConfiguration obj = JsonUtils.asObject(JsonUtils.asJson(allConifgs.get(0)), ElkCVConfiguration.class);

    assertEquals(savedObjectUuid, obj.getUuid());
    assertEquals(accountId, obj.getAccountId());
    assertEquals(appId, obj.getAppId());
    assertEquals(envId, obj.getEnvId());
    assertEquals(serviceId, obj.getServiceId());
    assertEquals(ELK, obj.getStateType());
    assertEquals(AnalysisTolerance.MEDIUM, obj.getAnalysisTolerance());
    assertEquals("Config 1", obj.getName());

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId + "&appId=" + appId
        + "&stateType=" + ELK + "&serviceConfigurationId=" + savedObjectUuid;
    target = client.target(url);
    elkCVServiceConfiguration.setName("Config 2");
    elkCVServiceConfiguration.setEnabled24x7(false);
    elkCVServiceConfiguration.setBaselineStartMinute(135);
    elkCVServiceConfiguration.setBaselineEndMinute(330);

    elkCVServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.LOW);
    elkCVServiceConfiguration.setQuery("query2");
    elkCVServiceConfiguration.setQueryType(ElkQueryType.MATCH);
    elkCVServiceConfiguration.setIndex("filebeat-*");
    elkCVServiceConfiguration.setHostnameField("host2");
    elkCVServiceConfiguration.setMessageField("message2");
    elkCVServiceConfiguration.setTimestampField("timestamp2");
    elkCVServiceConfiguration.setTimestampFormat("timestamp_format2");

    getRequestBuilderWithAuthHeader(target).put(
        entity(elkCVServiceConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<ElkCVConfiguration>>() {});
    fetchedObject = getRequestResponse.getResource();
    assertFalse(fetchedObject.isEnabled24x7());
    assertEquals(AnalysisTolerance.LOW, fetchedObject.getAnalysisTolerance());
    assertEquals(121, fetchedObject.getBaselineStartMinute());
    assertEquals(330, fetchedObject.getBaselineEndMinute());
    assertEquals("Config 2", fetchedObject.getName());
    assertEquals("query2", fetchedObject.getQuery());
    assertEquals(ElkQueryType.MATCH, fetchedObject.getQueryType());
    assertEquals("filebeat-*", fetchedObject.getIndex());
    assertEquals("host2", fetchedObject.getHostnameField());
    assertEquals("message2", fetchedObject.getMessageField());
    assertEquals("timestamp2", fetchedObject.getTimestampField());
    assertEquals("timestamp_format2", fetchedObject.getTimestampFormat());

    String delete_url =
        API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId + "&appId=" + appId;
    target = client.target(delete_url);
    RestResponse<Boolean> response = getRequestBuilderWithAuthHeader(target).delete(new GenericType<RestResponse>() {});
    assertEquals(true, response.getResource());

    delete_url =
        API_BASE + "/cv-configuration/" + UUID.randomUUID().toString() + "?accountId=" + accountId + "&appId=" + appId;
    target = client.target(delete_url);
    response = getRequestBuilderWithAuthHeader(target).delete(new GenericType<RestResponse>() {});
    assertEquals(false, response.getResource());
  }

  @Test
  @Category(IntegrationTests.class)
  public <T extends CVConfiguration> void testBugSnagConfiguration() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    String url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + BUG_SNAG;
    logger.info("POST " + url);
    WebTarget target = client.target(url);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(bugsnagCVConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    String savedObjectUuid = restResponse.getResource();

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;

    target = client.target(url);
    RestResponse<BugsnagCVConfiguration> getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<BugsnagCVConfiguration>>() {});
    BugsnagCVConfiguration fetchedObject = getRequestResponse.getResource();

    BugsnagCVConfiguration cvConfiguration = fetchedObject;

    validateConfiguration(savedObjectUuid, cvConfiguration);

    url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId;
    target = client.target(url);

    RestResponse<List<Object>> allConfigResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<Object>>>() {});
    List<Object> allConifgs = allConfigResponse.getResource();

    assertEquals(1, allConifgs.size());

    BugsnagCVConfiguration obj = JsonUtils.asObject(JsonUtils.asJson(allConifgs.get(0)), BugsnagCVConfiguration.class);

    assertEquals(savedObjectUuid, obj.getUuid());
    assertEquals(accountId, obj.getAccountId());
    assertEquals(appId, obj.getAppId());
    assertEquals(envId, obj.getEnvId());
    assertEquals(serviceId, obj.getServiceId());
    assertEquals(BUG_SNAG, obj.getStateType());
    assertEquals(AnalysisTolerance.MEDIUM, obj.getAnalysisTolerance());
    assertEquals("Config 1", obj.getName());

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId + "&appId=" + appId
        + "&stateType=" + BUG_SNAG + "&serviceConfigurationId=" + savedObjectUuid;
    target = client.target(url);
    cvConfiguration.setName("Config 2");
    cvConfiguration.setEnabled24x7(false);

    cvConfiguration.setAnalysisTolerance(AnalysisTolerance.LOW);
    cvConfiguration.setQuery("query2");
    cvConfiguration.setBaselineStartMinute(25931716);
    cvConfiguration.setBaselineEndMinute(25931835);

    getRequestBuilderWithAuthHeader(target).put(
        entity(cvConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<BugsnagCVConfiguration>>() {});
    fetchedObject = getRequestResponse.getResource();
    assertFalse(fetchedObject.isEnabled24x7());
    assertEquals(AnalysisTolerance.LOW, fetchedObject.getAnalysisTolerance());
    assertEquals("Config 2", fetchedObject.getName());
    assertEquals("query2", fetchedObject.getQuery());
    assertEquals(25931835, fetchedObject.getBaselineEndMinute());
    assertEquals(25931716, fetchedObject.getBaselineStartMinute());

    String delete_url =
        API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId + "&appId=" + appId;
    target = client.target(delete_url);
    RestResponse<Boolean> response = getRequestBuilderWithAuthHeader(target).delete(new GenericType<RestResponse>() {});
    assertEquals(true, response.getResource());

    delete_url =
        API_BASE + "/cv-configuration/" + UUID.randomUUID().toString() + "?accountId=" + accountId + "&appId=" + appId;
    target = client.target(delete_url);
    response = getRequestBuilderWithAuthHeader(target).delete(new GenericType<RestResponse>() {});
    assertEquals(false, response.getResource());
  }

  @Test
  @Category(IntegrationTests.class)
  public <T extends CVConfiguration> void testStackdriverConfiguration() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    String url =
        API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + STACK_DRIVER_LOG;
    logger.info("POST " + url);
    WebTarget target = client.target(url);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(stackdriverCVConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    String savedObjectUuid = restResponse.getResource();

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;

    target = client.target(url);
    RestResponse<StackdriverCVConfiguration> getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<StackdriverCVConfiguration>>() {});
    StackdriverCVConfiguration fetchedObject = getRequestResponse.getResource();

    StackdriverCVConfiguration cvConfiguration = fetchedObject;
    assertEquals(STACK_DRIVER_LOG, fetchedObject.getStateType());
    validateConfiguration(savedObjectUuid, cvConfiguration);

    url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId;
    target = client.target(url);

    RestResponse<List<Object>> allConfigResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<Object>>>() {});
    List<Object> allConifgs = allConfigResponse.getResource();

    assertEquals(1, allConifgs.size());

    StackdriverCVConfiguration obj =
        JsonUtils.asObject(JsonUtils.asJson(allConifgs.get(0)), StackdriverCVConfiguration.class);

    assertEquals(savedObjectUuid, obj.getUuid());
    assertEquals(accountId, obj.getAccountId());
    assertEquals(appId, obj.getAppId());
    assertEquals(envId, obj.getEnvId());
    assertEquals(serviceId, obj.getServiceId());
    assertEquals(STACK_DRIVER_LOG, obj.getStateType());
    assertEquals(AnalysisTolerance.MEDIUM, obj.getAnalysisTolerance());
    assertEquals("Config 1", obj.getName());

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId + "&appId=" + appId
        + "&stateType=" + STACK_DRIVER_LOG + "&serviceConfigurationId=" + savedObjectUuid;
    target = client.target(url);
    cvConfiguration.setName("Config 2");
    cvConfiguration.setEnabled24x7(false);

    cvConfiguration.setAnalysisTolerance(AnalysisTolerance.LOW);
    cvConfiguration.setQuery("query2");

    getRequestBuilderWithAuthHeader(target).put(
        entity(cvConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<StackdriverCVConfiguration>>() {});
    fetchedObject = getRequestResponse.getResource();
    assertFalse(fetchedObject.isEnabled24x7());
    assertEquals(AnalysisTolerance.LOW, fetchedObject.getAnalysisTolerance());
    assertEquals("Config 2", fetchedObject.getName());
    assertEquals("query2", fetchedObject.getQuery());

    String delete_url =
        API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId + "&appId=" + appId;
    target = client.target(delete_url);
    RestResponse<Boolean> response = getRequestBuilderWithAuthHeader(target).delete(new GenericType<RestResponse>() {});
    assertEquals(true, response.getResource());

    delete_url =
        API_BASE + "/cv-configuration/" + UUID.randomUUID().toString() + "?accountId=" + accountId + "&appId=" + appId;
    target = client.target(delete_url);
    response = getRequestBuilderWithAuthHeader(target).delete(new GenericType<RestResponse>() {});
    assertEquals(false, response.getResource());
  }

  private void validateConfiguration(String savedObjectUuid, LogsCVConfiguration cvConfiguration) {
    assertEquals(savedObjectUuid, cvConfiguration.getUuid());
    assertEquals(accountId, cvConfiguration.getAccountId());
    assertEquals(appId, cvConfiguration.getAppId());
    assertEquals(envId, cvConfiguration.getEnvId());
    assertEquals(serviceId, cvConfiguration.getServiceId());
    assertEquals(AnalysisTolerance.MEDIUM, cvConfiguration.getAnalysisTolerance());
    assertEquals("someServiceName", cvConfiguration.getServiceName());

    assertEquals("*exception*", cvConfiguration.getQuery());
  }

  @Test
  @Category(IntegrationTests.class)
  public <T extends CVConfiguration> void testLogsConfiguration() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    String url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + SUMO;
    logger.info("POST " + url);
    WebTarget target = client.target(url);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(logsCVConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    String savedObjectUuid = restResponse.getResource();

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;

    target = client.target(url);
    RestResponse<LogsCVConfiguration> getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<LogsCVConfiguration>>() {});
    LogsCVConfiguration fetchedObject = getRequestResponse.getResource();

    LogsCVConfiguration logsCVConfiguration = fetchedObject;
    assertEquals(savedObjectUuid, fetchedObject.getUuid());
    assertEquals(accountId, fetchedObject.getAccountId());
    assertEquals(appId, fetchedObject.getAppId());
    assertEquals(envId, fetchedObject.getEnvId());
    assertEquals(serviceId, fetchedObject.getServiceId());
    assertEquals(SUMO, fetchedObject.getStateType());
    assertEquals(AnalysisTolerance.MEDIUM, fetchedObject.getAnalysisTolerance());
    assertEquals("someSettingAttributeName", logsCVConfiguration.getConnectorName());
    assertEquals("someServiceName", logsCVConfiguration.getServiceName());

    assertEquals("query1", fetchedObject.getQuery());
    assertEquals(91, fetchedObject.getBaselineStartMinute());
    assertEquals(195, fetchedObject.getBaselineEndMinute());

    url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId;
    target = client.target(url);

    RestResponse<List<Object>> allConfigResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<Object>>>() {});
    List<Object> allConifgs = allConfigResponse.getResource();

    assertEquals(1, allConifgs.size());

    LogsCVConfiguration obj = JsonUtils.asObject(JsonUtils.asJson(allConifgs.get(0)), LogsCVConfiguration.class);

    assertEquals(savedObjectUuid, obj.getUuid());
    assertEquals(accountId, obj.getAccountId());
    assertEquals(appId, obj.getAppId());
    assertEquals(envId, obj.getEnvId());
    assertEquals(serviceId, obj.getServiceId());
    assertEquals(SUMO, obj.getStateType());
    assertEquals(AnalysisTolerance.MEDIUM, obj.getAnalysisTolerance());
    assertEquals("Config 1", obj.getName());
    assertEquals("query1", obj.getQuery());
    assertEquals(91, obj.getBaselineStartMinute());
    assertEquals(195, obj.getBaselineEndMinute());

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId + "&appId=" + appId
        + "&stateType=" + SUMO + "&serviceConfigurationId=" + savedObjectUuid;
    target = client.target(url);
    logsCVConfiguration.setName("Config 2");
    logsCVConfiguration.setEnabled24x7(false);

    logsCVConfiguration.setAnalysisTolerance(AnalysisTolerance.LOW);
    logsCVConfiguration.setQuery("query2");
    logsCVConfiguration.setBaselineStartMinute(106);
    logsCVConfiguration.setBaselineEndMinute(210);

    getRequestBuilderWithAuthHeader(target).put(
        entity(logsCVConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<LogsCVConfiguration>>() {});
    fetchedObject = getRequestResponse.getResource();
    assertFalse(fetchedObject.isEnabled24x7());
    assertEquals(AnalysisTolerance.LOW, fetchedObject.getAnalysisTolerance());
    assertEquals("Config 2", fetchedObject.getName());
    assertEquals("query2", fetchedObject.getQuery());
    assertEquals(106, fetchedObject.getBaselineStartMinute());
    assertEquals(210, fetchedObject.getBaselineEndMinute());

    String delete_url =
        API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId + "&appId=" + appId;
    target = client.target(delete_url);
    RestResponse<Boolean> response = getRequestBuilderWithAuthHeader(target).delete(new GenericType<RestResponse>() {});
    assertEquals(true, response.getResource());

    delete_url =
        API_BASE + "/cv-configuration/" + UUID.randomUUID().toString() + "?accountId=" + accountId + "&appId=" + appId;
    target = client.target(delete_url);
    response = getRequestBuilderWithAuthHeader(target).delete(new GenericType<RestResponse>() {});
    assertEquals(false, response.getResource());
  }

  @Test
  @Repeat(times = 5, successes = 1)
  @Category(IntegrationTests.class)
  public <T extends CVConfiguration> void testLogsConfigurationValidUntil() throws InterruptedException {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    String url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + SUMO;
    logger.info("POST " + url);
    WebTarget target = client.target(url);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(logsCVConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    String savedObjectUuid = restResponse.getResource();

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;

    target = client.target(url);
    RestResponse<LogsCVConfiguration> getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<LogsCVConfiguration>>() {});
    LogsCVConfiguration fetchedObject = getRequestResponse.getResource();

    LogsCVConfiguration responseConfig = fetchedObject;
    assertEquals(savedObjectUuid, fetchedObject.getUuid());
    assertEquals(accountId, fetchedObject.getAccountId());
    assertEquals(appId, fetchedObject.getAppId());
    assertEquals(envId, fetchedObject.getEnvId());
    assertEquals(serviceId, fetchedObject.getServiceId());
    assertEquals(SUMO, fetchedObject.getStateType());
    assertEquals(AnalysisTolerance.MEDIUM, fetchedObject.getAnalysisTolerance());
    assertEquals("someSettingAttributeName", responseConfig.getConnectorName());
    assertEquals("someServiceName", responseConfig.getServiceName());

    wingsPersistence.updateField(LogsCVConfiguration.class, savedObjectUuid, "validUntil",
        Date.from(OffsetDateTime.now().plusSeconds(1).toInstant()));
    Date validUntil = Date.from(OffsetDateTime.now().plusSeconds(1).toInstant());

    List<CVConfiguration> cvConfigurationList =
        wingsPersistence.createQuery(CVConfiguration.class, excludeAuthority).filter("_id", fetchedObject).asList();

    cvConfigurationList.forEach(configRecords -> {
      if (configRecords.getValidUntil() != null) {
        assertTrue(validUntil.getTime() > configRecords.getValidUntil().getTime());
      }
    });
  }

  @Test
  @Category(IntegrationTests.class)
  public void testListConfig() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    String otherEnvId = generateUuid();
    String appId = generateUuid();

    String newRelicApplicationId = generateUuid();
    newRelicCVServiceConfiguration = new NewRelicCVServiceConfiguration();
    newRelicCVServiceConfiguration.setAppId(appId);
    newRelicCVServiceConfiguration.setEnvId(otherEnvId);
    newRelicCVServiceConfiguration.setServiceId(serviceId);
    newRelicCVServiceConfiguration.setEnabled24x7(true);
    newRelicCVServiceConfiguration.setApplicationId(newRelicApplicationId);
    newRelicCVServiceConfiguration.setConnectorId(settingAttributeId);
    newRelicCVServiceConfiguration.setMetrics(Collections.singletonList("apdexScore"));
    newRelicCVServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);
    newRelicCVServiceConfiguration.setName("name2");

    appDynamicsCVServiceConfiguration = new AppDynamicsCVServiceConfiguration();
    appDynamicsCVServiceConfiguration.setAppId(appId);
    appDynamicsCVServiceConfiguration.setEnvId(otherEnvId);
    appDynamicsCVServiceConfiguration.setServiceId(serviceId);
    appDynamicsCVServiceConfiguration.setEnabled24x7(true);
    appDynamicsCVServiceConfiguration.setAppDynamicsApplicationId(appDynamicsApplicationId);
    appDynamicsCVServiceConfiguration.setTierId(generateUuid());
    appDynamicsCVServiceConfiguration.setConnectorId(generateUuid());
    appDynamicsCVServiceConfiguration.setStateType(APP_DYNAMICS);
    appDynamicsCVServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.HIGH);
    appDynamicsCVServiceConfiguration.setName("name1");

    // Adding a workflow cvConfiguration. And verify this configuration is not returned from the REST API
    AppDynamicsCVServiceConfiguration appDynamicsCVServiceConfigurationFromWorkflow =
        new AppDynamicsCVServiceConfiguration();
    appDynamicsCVServiceConfigurationFromWorkflow.setAppId(appId);
    appDynamicsCVServiceConfigurationFromWorkflow.setEnvId(envId);
    appDynamicsCVServiceConfigurationFromWorkflow.setServiceId(serviceId);
    appDynamicsCVServiceConfigurationFromWorkflow.setEnabled24x7(true);
    appDynamicsCVServiceConfigurationFromWorkflow.setWorkflowConfig(true);
    appDynamicsCVServiceConfigurationFromWorkflow.setAppDynamicsApplicationId(appDynamicsApplicationId);
    appDynamicsCVServiceConfigurationFromWorkflow.setTierId(generateUuid());
    appDynamicsCVServiceConfigurationFromWorkflow.setConnectorId(generateUuid());
    appDynamicsCVServiceConfigurationFromWorkflow.setStateType(APP_DYNAMICS);
    appDynamicsCVServiceConfigurationFromWorkflow.setAnalysisTolerance(AnalysisTolerance.HIGH);
    appDynamicsCVServiceConfigurationFromWorkflow.setName("name");

    // Save 2 cvConfigs with the same appId but different envIds
    String url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + NEW_RELIC;
    WebTarget target = client.target(url);
    getRequestBuilderWithAuthHeader(target).post(
        entity(newRelicCVServiceConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + APP_DYNAMICS;
    target = client.target(url);
    getRequestBuilderWithAuthHeader(target).post(
        entity(appDynamicsCVServiceConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});

    url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + APP_DYNAMICS;
    target = client.target(url);
    getRequestBuilderWithAuthHeader(target).post(
        entity(appDynamicsCVServiceConfigurationFromWorkflow, APPLICATION_JSON),
        new GenericType<RestResponse<String>>() {});

    url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId;
    target = client.target(url);

    RestResponse<List<Object>> allConfigResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<Object>>>() {});
    List<Object> allConfigs = allConfigResponse.getResource();

    assertEquals(2, allConfigs.size());

    NewRelicCVServiceConfiguration obj =
        JsonUtils.asObject(JsonUtils.asJson(allConfigs.get(0)), NewRelicCVServiceConfiguration.class);
    assertEquals(appId, obj.getAppId());
    assertEquals(otherEnvId, obj.getEnvId());

    AppDynamicsCVServiceConfiguration appDObject =
        JsonUtils.asObject(JsonUtils.asJson(allConfigs.get(1)), AppDynamicsCVServiceConfiguration.class);
    assertEquals(appId, appDObject.getAppId());
    assertEquals(otherEnvId, appDObject.getEnvId());

    url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&envId=" + otherEnvId;
    target = client.target(url);

    RestResponse<List<Object>> listOfConfigsByEnvResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<Object>>>() {});
    List<Object> listOfConfigsByEnv = listOfConfigsByEnvResponse.getResource();

    assertEquals(2, listOfConfigsByEnv.size());

    obj = JsonUtils.asObject(JsonUtils.asJson(listOfConfigsByEnv.get(0)), NewRelicCVServiceConfiguration.class);
    assertEquals(appId, obj.getAppId());
    assertEquals(otherEnvId, obj.getEnvId());

    url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&envId=" + otherEnvId;
    target = client.target(url);

    listOfConfigsByEnvResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<Object>>>() {});
    listOfConfigsByEnv = listOfConfigsByEnvResponse.getResource();

    assertEquals(2, listOfConfigsByEnv.size());

    appDObject =
        JsonUtils.asObject(JsonUtils.asJson(listOfConfigsByEnv.get(0)), AppDynamicsCVServiceConfiguration.class);
    assertEquals(appId, appDObject.getAppId());
    assertEquals(otherEnvId, appDObject.getEnvId());

    // This call to list configs should fetch only the app dynamics config
    url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + APP_DYNAMICS;
    target = client.target(url);

    listOfConfigsByEnvResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<Object>>>() {});
    listOfConfigsByEnv = listOfConfigsByEnvResponse.getResource();

    assertEquals(1, listOfConfigsByEnv.size());

    appDObject =
        JsonUtils.asObject(JsonUtils.asJson(listOfConfigsByEnv.get(0)), AppDynamicsCVServiceConfiguration.class);
    assertEquals(appId, appDObject.getAppId());
    assertEquals(otherEnvId, appDObject.getEnvId());
    assertEquals(APP_DYNAMICS, appDObject.getStateType());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testLogsConfigurationResetBaseline() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    String url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + SUMO;
    logger.info("POST " + url);
    WebTarget target = client.target(url);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(logsCVConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    String savedObjectUuid = restResponse.getResource();

    LogsCVConfiguration updateBaseline = null;

    url = API_BASE + "/cv-configuration/reset-baseline?cvConfigId=" + generateUuid() + "&accountId=" + accountId
        + "&appId=" + appId;
    target = client.target(url);
    try {
      getRequestBuilderWithAuthHeader(target).post(
          entity(updateBaseline, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
      fail("Did not fail for invalid cvConfig");
    } catch (Exception e) {
      // exepected
    }

    url = API_BASE + "/cv-configuration/reset-baseline?cvConfigId=" + savedObjectUuid + "&accountId=" + accountId
        + "&appId=" + appId;
    target = client.target(url);
    try {
      getRequestBuilderWithAuthHeader(target).post(
          entity(updateBaseline, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
      fail("Did not fail for null payload");
    } catch (Exception e) {
      // exepected
    }

    updateBaseline = new LogsCVConfiguration();
    target = client.target(url);
    try {
      getRequestBuilderWithAuthHeader(target).post(
          entity(updateBaseline, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
      fail("Did not fail for zero baseline");
    } catch (Exception e) {
      // exepected
    }

    updateBaseline.setBaselineStartMinute(16);
    target = client.target(url);
    try {
      getRequestBuilderWithAuthHeader(target).post(
          entity(updateBaseline, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
      fail("Did not fail for  zero baseline end minute");
    } catch (Exception e) {
      // exepected
    }

    updateBaseline.setBaselineEndMinute(20);
    target = client.target(url);
    try {
      getRequestBuilderWithAuthHeader(target).post(
          entity(updateBaseline, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
      fail("Did not fail for invalid baseline");
    } catch (Exception e) {
      // exepected
    }

    for (int i = 0; i < 100; i++) {
      final LearningEngineAnalysisTask learningEngineAnalysisTask =
          LearningEngineAnalysisTask.builder().cvConfigId(savedObjectUuid).state_execution_id(generateUuid()).build();
      learningEngineAnalysisTask.setAppId(appId);
      wingsPersistence.save(learningEngineAnalysisTask);
      LogDataRecord logDataRecord = new LogDataRecord();
      logDataRecord.setCvConfigId(savedObjectUuid);
      logDataRecord.setAppId(appId);
      logDataRecord.setStateExecutionId(generateUuid());
      wingsPersistence.save(logDataRecord);
      wingsPersistence.save(LogMLAnalysisRecord.builder()
                                .cvConfigId(savedObjectUuid)
                                .appId(appId)
                                .stateExecutionId(generateUuid())
                                .logCollectionMinute(i)
                                .build());
    }

    assertEquals(100,
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
            .filter(LearningEngineAnalysisTaskKeys.cvConfigId, savedObjectUuid)
            .filter("appId", appId)
            .asList()
            .size());

    assertEquals(100,
        wingsPersistence.createQuery(LogDataRecord.class)
            .filter(LogDataRecordKeys.cvConfigId, savedObjectUuid)
            .filter("appId", appId)
            .asList()
            .size());

    assertEquals(100,
        wingsPersistence.createQuery(LogMLAnalysisRecord.class)
            .filter(LogMLAnalysisRecordKeys.cvConfigId, savedObjectUuid)
            .filter("appId", appId)
            .asList()
            .size());

    updateBaseline.setBaselineEndMinute(38);
    target = client.target(url);
    final RestResponse<String> updateResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(updateBaseline, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    assertNotEquals(savedObjectUuid, updateResponse.getResource());
    savedObjectUuid = updateResponse.getResource();

    sleep(ofMillis(5000));
    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;

    target = client.target(url);
    RestResponse<LogsCVConfiguration> getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<LogsCVConfiguration>>() {});
    assertEquals(updateBaseline.getBaselineStartMinute(), getRequestResponse.getResource().getBaselineStartMinute());
    assertEquals(updateBaseline.getBaselineEndMinute(), getRequestResponse.getResource().getBaselineEndMinute());

    assertEquals(0,
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
            .filter(LearningEngineAnalysisTaskKeys.cvConfigId, savedObjectUuid)
            .filter("appId", appId)
            .asList()
            .size());

    assertEquals(0,
        wingsPersistence.createQuery(LogDataRecord.class)
            .filter(LogDataRecordKeys.cvConfigId, savedObjectUuid)
            .filter("appId", appId)
            .asList()
            .size());

    assertEquals(16,
        wingsPersistence.createQuery(LogMLAnalysisRecord.class)
            .filter(LogMLAnalysisRecordKeys.cvConfigId, savedObjectUuid)
            .filter("appId", appId)
            .asList()
            .size());
    wingsPersistence.createQuery(LogMLAnalysisRecord.class)
        .filter(LogMLAnalysisRecordKeys.cvConfigId, savedObjectUuid)
        .filter("appId", appId)
        .asList()
        .forEach(logMLAnalysisRecord -> assertTrue(logMLAnalysisRecord.isDeprecated()));

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;
    target = client.target(url);
    getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<LogsCVConfiguration>>() {});
    LogsCVConfiguration fetchedObject = getRequestResponse.getResource();

    logsCVConfiguration.setAccountId(accountId);
    logsCVConfiguration.setStateType(StateType.SUMO);

    assertEquals(logsCVConfiguration.getName(), fetchedObject.getName());
    assertEquals(logsCVConfiguration.getAccountId(), fetchedObject.getAccountId());
    assertEquals(logsCVConfiguration.getConnectorId(), fetchedObject.getConnectorId());
    assertEquals(logsCVConfiguration.getEnvId(), fetchedObject.getEnvId());
    assertEquals(logsCVConfiguration.getServiceId(), fetchedObject.getServiceId());
    assertEquals(logsCVConfiguration.getStateType(), fetchedObject.getStateType());
    assertEquals(logsCVConfiguration.getAnalysisTolerance(), fetchedObject.getAnalysisTolerance());
    assertEquals(logsCVConfiguration.isEnabled24x7(), fetchedObject.isEnabled24x7());
    assertEquals(logsCVConfiguration.getComparisonStrategy(), fetchedObject.getComparisonStrategy());
    assertEquals(logsCVConfiguration.getContextId(), fetchedObject.getContextId());
    assertEquals(logsCVConfiguration.isWorkflowConfig(), fetchedObject.isWorkflowConfig());
    assertEquals(logsCVConfiguration.isAlertEnabled(), fetchedObject.isAlertEnabled());
    assertEquals(logsCVConfiguration.getAlertThreshold(), fetchedObject.getAlertThreshold(), 0.0);
    assertEquals(logsCVConfiguration.getSnoozeStartTime(), fetchedObject.getSnoozeStartTime());
    assertEquals(logsCVConfiguration.getSnoozeEndTime(), fetchedObject.getSnoozeEndTime());
    assertEquals(logsCVConfiguration.getQuery(), fetchedObject.getQuery());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testLogsConfigurationUpdateAlert() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    String url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + SUMO;
    logger.info("POST " + url);
    WebTarget target = client.target(url);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(logsCVConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    String savedObjectUuid = restResponse.getResource();

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;
    target = client.target(url);
    RestResponse<LogsCVConfiguration> getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<LogsCVConfiguration>>() {});
    LogsCVConfiguration fetchedObject = getRequestResponse.getResource();
    assertFalse(fetchedObject.isAlertEnabled());
    assertEquals(0.1, fetchedObject.getAlertThreshold(), 0.0);
    assertEquals(0, fetchedObject.getSnoozeStartTime());
    assertEquals(0, fetchedObject.getSnoozeEndTime());

    CVConfiguration cvConfiguration = new CVConfiguration();
    cvConfiguration.setAlertThreshold(0.5);
    cvConfiguration.setAlertEnabled(true);
    cvConfiguration.setSnoozeStartTime(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5));
    cvConfiguration.setSnoozeEndTime(System.currentTimeMillis());

    url = API_BASE + "/cv-configuration/update-alert-setting?accountId=" + accountId + "&cvConfigId=" + savedObjectUuid;
    target = client.target(url);
    RestResponse<Boolean> updateResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(cvConfiguration, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
    assertTrue(updateResponse.getResource());

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;
    target = client.target(url);
    getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<LogsCVConfiguration>>() {});
    fetchedObject = getRequestResponse.getResource();
    assertTrue(fetchedObject.isAlertEnabled());
    assertEquals(0.5, fetchedObject.getAlertThreshold(), 0.0);
    assertEquals(0, fetchedObject.getSnoozeStartTime());
    assertEquals(0, fetchedObject.getSnoozeEndTime());

    url = API_BASE + "/cv-configuration/update-snooze?accountId=" + accountId + "&cvConfigId=" + savedObjectUuid;
    target = client.target(url);
    updateResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(cvConfiguration, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
    assertTrue(updateResponse.getResource());

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;
    target = client.target(url);
    getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<LogsCVConfiguration>>() {});
    fetchedObject = getRequestResponse.getResource();
    assertTrue(fetchedObject.isAlertEnabled());
    assertEquals(0.5, fetchedObject.getAlertThreshold(), 0.0);
    assertEquals(cvConfiguration.getSnoozeStartTime(), fetchedObject.getSnoozeStartTime());
    assertEquals(cvConfiguration.getSnoozeEndTime(), fetchedObject.getSnoozeEndTime());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testListConfigurations() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());
    String testUuid = generateUuid();
    logger.info("testUuid {}", testUuid);
    int numOfApplications = 5;
    int numOfEnvs = 10;

    for (int i = 0; i < numOfApplications; i++) {
      for (int j = 0; j < numOfEnvs; j++) {
        createNewRelicConfig(true);
        newRelicCVServiceConfiguration.setEnvId("env" + j + testUuid);
        newRelicCVServiceConfiguration.setName("NRTestConfig" + testUuid);
        String url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=app" + i + testUuid
            + "&stateType=" + NEW_RELIC;
        WebTarget target = client.target(url);
        getRequestBuilderWithAuthHeader(target).post(
            entity(newRelicCVServiceConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
        url =
            API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=app" + i + testUuid + "&stateType=" + SUMO;
        target = client.target(url);
        createLogsCVConfig(true);
        logsCVConfiguration.setEnvId("env" + j + testUuid);
        logsCVConfiguration.setName("SumoTestConfig" + testUuid);
        getRequestBuilderWithAuthHeader(target).post(
            entity(logsCVConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
      }
    }
    assertEquals(numOfApplications * numOfEnvs,
        wingsPersistence.createQuery(CVConfiguration.class)
            .filter(CVConfigurationKeys.accountId, accountId)
            .filter(CVConfigurationKeys.name, "NRTestConfig" + testUuid)
            .asList()
            .size());
    assertEquals(numOfApplications * numOfEnvs,
        wingsPersistence.createQuery(CVConfiguration.class)
            .filter(CVConfigurationKeys.accountId, accountId)
            .filter(CVConfigurationKeys.name, "SumoTestConfig" + testUuid)
            .asList()
            .size());

    // ask for all the cvConfigs for 1 app
    String url =
        API_BASE + "/cv-configuration/list-cv-configurations?accountId=" + accountId + "&appIds=app0" + testUuid;
    WebTarget target = client.target(url);
    RestResponse<List<CVConfiguration>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<CVConfiguration>>>() {});
    List<CVConfiguration> cvConfigurations = restResponse.getResource();
    assertEquals(numOfEnvs * 2, cvConfigurations.size());
    for (int i = 0; i < numOfEnvs; i++) {
      assertEquals("app0" + testUuid, cvConfigurations.get(i * 2).getAppId());
      assertEquals("env" + i + testUuid, cvConfigurations.get(i * 2).getEnvId());
      assertEquals("app0" + testUuid, cvConfigurations.get(i * 2 + 1).getAppId());
      assertEquals("env" + i + testUuid, cvConfigurations.get(i * 2 + 1).getEnvId());
    }

    // ask for all the cvConfigs for 2 apps
    url = API_BASE + "/cv-configuration/list-cv-configurations?accountId=" + accountId + "&appIds=app0" + testUuid
        + "&appIds=app1" + testUuid;
    target = client.target(url);
    restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<CVConfiguration>>>() {});
    cvConfigurations = restResponse.getResource();
    assertEquals(numOfEnvs * 2 * 2, cvConfigurations.size());
    for (int i = 0; i < 2; i++) {
      for (int j = 0; j < numOfEnvs; j++) {
        final int index = i * numOfEnvs * 2 + j * 2;
        assertEquals("failed for " + i + ":" + j + " index:" + index, "app" + i + testUuid,
            cvConfigurations.get(index).getAppId());
        assertEquals("failed for " + i + ":" + j + " index:" + index, "env" + j + testUuid,
            cvConfigurations.get(index).getEnvId());
        assertEquals("failed for " + i + ":" + j + " index:" + index, "app" + i + testUuid,
            cvConfigurations.get(index + 1).getAppId());
        assertEquals("failed for " + i + ":" + j + " index:" + index, "env" + j + testUuid,
            cvConfigurations.get(index + 1).getEnvId());
      }
    }

    // ask for all the cvConfigs for 2 apps and 2 envs
    url = API_BASE + "/cv-configuration/list-cv-configurations?accountId=" + accountId + "&appIds=app0" + testUuid
        + "&appIds=app1" + testUuid + "&envIds=env0" + testUuid + "&envIds=env1" + testUuid;
    target = client.target(url);
    restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<CVConfiguration>>>() {});
    cvConfigurations = restResponse.getResource();
    assertEquals(8, cvConfigurations.size());

    for (int i = 0; i < 2; i++) {
      for (int j = 0; j < 2; j++) {
        final int index = i * 2 * 2 + j * 2;
        assertEquals("failed for " + i + ":" + j + " index:" + index, "app" + i + testUuid,
            cvConfigurations.get(index).getAppId());
        assertEquals("failed for " + i + ":" + j + " index:" + index, "env" + j + testUuid,
            cvConfigurations.get(index).getEnvId());
        assertEquals("failed for " + i + ":" + j + " index:" + index, "app" + i + testUuid,
            cvConfigurations.get(index + 1).getAppId());
        assertEquals("failed for " + i + ":" + j + " index:" + index, "env" + j + testUuid,
            cvConfigurations.get(index + 1).getEnvId());
      }
    }
  }
}
