package software.wings.integration.verification;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.sm.StateType.APP_DYNAMICS;
import static software.wings.sm.StateType.CLOUD_WATCH;
import static software.wings.sm.StateType.DATA_DOG;
import static software.wings.sm.StateType.DYNA_TRACE;
import static software.wings.sm.StateType.ELK;
import static software.wings.sm.StateType.NEW_RELIC;
import static software.wings.sm.StateType.PROMETHEUS;
import static software.wings.sm.StateType.SUMO;
import static software.wings.utils.WingsTestConstants.mockChecker;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.limits.LimitCheckerFactory;
import io.harness.rest.RestResponse;
import io.harness.rule.RepeatRule.Repeat;
import io.harness.serializer.JsonUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.intfc.AppService;
import software.wings.verification.CVConfiguration;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;
import software.wings.verification.cloudwatch.CloudWatchCVServiceConfiguration;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;
import software.wings.verification.dynatrace.DynaTraceCVServiceConfiguration;
import software.wings.verification.log.ElkCVConfiguration;
import software.wings.verification.log.LogsCVConfiguration;
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
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * @author Vaibhav Tulsyan
 * 05/Oct/2018
 */
public class CVConfigurationIntegrationTest extends BaseIntegrationTest {
  @Rule public ExpectedException thrown = ExpectedException.none();
  private String appId, envId, serviceId, appDynamicsApplicationId;
  @Inject private WingsPersistence wingsPersistence;
  @Inject @InjectMocks private AppService appService;
  @Mock private LimitCheckerFactory limitCheckerFactory;
  private NewRelicCVServiceConfiguration newRelicCVServiceConfiguration;
  private AppDynamicsCVServiceConfiguration appDynamicsCVServiceConfiguration;
  private DynaTraceCVServiceConfiguration dynaTraceCVServiceConfiguration;
  private PrometheusCVServiceConfiguration prometheusCVServiceConfiguration;
  private DatadogCVServiceConfiguration datadogCVServiceConfiguration;
  private CloudWatchCVServiceConfiguration cloudWatchCVServiceConfiguration;
  private ElkCVConfiguration elkCVConfiguration;
  private LogsCVConfiguration logsCVConfiguration;

  private SettingAttribute settingAttribute;
  private String settingAttributeId;
  private Service service;

  @Before
  public void setUp() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    loginAdminUser();

    appId = appService.save(anApplication().withName(generateUuid()).withAccountId(accountId).build()).getUuid();
    envId = generateUuid();
    appDynamicsApplicationId = generateUuid();

    settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                           .withAccountId(accountId)
                           .withName("someSettingAttributeName")
                           .withCategory(Category.CONNECTOR)
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

    List<CloudWatchMetric> ec2Metrics = new ArrayList<>();
    ec2Metrics.add(
        new CloudWatchMetric("CPUUtilization", "CPU Usage", "InstanceId", "Host name expression", "VALUE", true));

    cloudWatchCVServiceConfiguration.setLoadBalancerMetrics(loadBalancerMetricsByLoadBalancer);
    cloudWatchCVServiceConfiguration.setRegion("us-east-2");
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
    datadogCVServiceConfiguration.setAppId(appId);
    datadogCVServiceConfiguration.setEnvId(envId);
    datadogCVServiceConfiguration.setServiceId(serviceId);
    datadogCVServiceConfiguration.setEnabled24x7(true);
    datadogCVServiceConfiguration.setConnectorId(generateUuid());
    datadogCVServiceConfiguration.setStateType(DATA_DOG);
    datadogCVServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.HIGH);
    datadogCVServiceConfiguration.setDatadogServiceName(generateUuid());
    datadogCVServiceConfiguration.setMetrics("trace.servlet.request.errors, system.mem.used, system.cpu.iowait");
  }

  private void createElkCVConfig(boolean enabled24x7) {
    elkCVConfiguration = new ElkCVConfiguration();
    elkCVConfiguration.setName("Config 1");
    elkCVConfiguration.setAppId(appId);
    elkCVConfiguration.setEnvId(envId);
    elkCVConfiguration.setServiceId(serviceId);
    elkCVConfiguration.setEnabled24x7(enabled24x7);
    elkCVConfiguration.setConnectorId(settingAttributeId);
    elkCVConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);
    elkCVConfiguration.setBaselineStartMinute(100);
    elkCVConfiguration.setBaselineEndMinute(200);

    elkCVConfiguration.setQuery("query1");
    elkCVConfiguration.setFormattedQuery(true);
    elkCVConfiguration.setQueryType(ElkQueryType.TERM);
    elkCVConfiguration.setIndex("index1");
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

    logsCVConfiguration.setQuery("query1");
    logsCVConfiguration.setFormattedQuery(true);
  }

  @Test
  public void testSaveConfiguration() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    String id = wingsPersistence.save(newRelicCVServiceConfiguration);
    NewRelicCVServiceConfiguration obj = wingsPersistence.get(NewRelicCVServiceConfiguration.class, id);
    assertEquals(id, obj.getUuid());
  }

  @Test
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
    assertEquals("trace.servlet.request.errors, system.mem.used, system.cpu.iowait", fetchedObject.getMetrics());

    // Test PUT API for Datadog
    datadogCVServiceConfiguration.setName("Datadog Config");
    datadogCVServiceConfiguration.setEnabled24x7(false);
    datadogCVServiceConfiguration.setMetrics("system.mem.used, system.cpu.iowait");
    datadogCVServiceConfiguration.setApplicationFilter("cluster:harness-test");
    datadogCVServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);

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
    assertEquals("system.mem.used, system.cpu.iowait", fetchedObject.getMetrics());
    assertEquals(AnalysisTolerance.MEDIUM, fetchedObject.getAnalysisTolerance());
  }

  @Test
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
    assertEquals("someSettingAttributeName", elkCVServiceConfiguration.getConnectorName());
    assertEquals("someServiceName", elkCVServiceConfiguration.getServiceName());
    assertEquals(91, fetchedObject.getBaselineStartMinute());
    assertEquals(195, fetchedObject.getBaselineEndMinute());

    assertEquals("query1", fetchedObject.getQuery());
    assertEquals(true, fetchedObject.isFormattedQuery());
    assertEquals(ElkQueryType.TERM, fetchedObject.getQueryType());
    assertEquals("index1", fetchedObject.getIndex());
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
    elkCVServiceConfiguration.setFormattedQuery(false);
    elkCVServiceConfiguration.setQuery("query2");
    elkCVServiceConfiguration.setQueryType(ElkQueryType.MATCH);
    elkCVServiceConfiguration.setIndex("index2");
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
    assertEquals(false, fetchedObject.isFormattedQuery());
    assertEquals(ElkQueryType.MATCH, fetchedObject.getQueryType());
    assertEquals("index2", fetchedObject.getIndex());
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
    assertEquals(true, fetchedObject.isFormattedQuery());
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
    assertEquals(true, obj.isFormattedQuery());
    assertEquals(91, obj.getBaselineStartMinute());
    assertEquals(195, obj.getBaselineEndMinute());

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId + "&appId=" + appId
        + "&stateType=" + SUMO + "&serviceConfigurationId=" + savedObjectUuid;
    target = client.target(url);
    logsCVConfiguration.setName("Config 2");
    logsCVConfiguration.setEnabled24x7(false);

    logsCVConfiguration.setAnalysisTolerance(AnalysisTolerance.LOW);
    logsCVConfiguration.setFormattedQuery(false);
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
    assertEquals(false, fetchedObject.isFormattedQuery());
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
  public void testListConfig() {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    String otherEnvId = generateUuid();

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
    assertEquals(envId, appDObject.getEnvId());

    // This call to list configs should fetch only the new relic config
    url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&envId=" + otherEnvId;
    target = client.target(url);

    RestResponse<List<Object>> listOfConfigsByEnvResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<Object>>>() {});
    List<Object> listOfConfigsByEnv = listOfConfigsByEnvResponse.getResource();

    assertEquals(1, listOfConfigsByEnv.size());

    obj = JsonUtils.asObject(JsonUtils.asJson(listOfConfigsByEnv.get(0)), NewRelicCVServiceConfiguration.class);
    assertEquals(appId, obj.getAppId());
    assertEquals(otherEnvId, obj.getEnvId());

    // This call to list configs should fetch only the app dynamics config
    url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&envId=" + envId;
    target = client.target(url);

    listOfConfigsByEnvResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<Object>>>() {});
    listOfConfigsByEnv = listOfConfigsByEnvResponse.getResource();

    assertEquals(1, listOfConfigsByEnv.size());

    appDObject =
        JsonUtils.asObject(JsonUtils.asJson(listOfConfigsByEnv.get(0)), AppDynamicsCVServiceConfiguration.class);
    assertEquals(appId, appDObject.getAppId());
    assertEquals(envId, appDObject.getEnvId());

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
    assertEquals(envId, appDObject.getEnvId());
    assertEquals(APP_DYNAMICS, appDObject.getStateType());
  }

  @Test
  public <T extends CVConfiguration> void testLogsConfigurationResetBaseline() {
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
          entity(updateBaseline, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
      fail("Did not fail for null payload");
    } catch (Exception e) {
      // exepected
    }

    updateBaseline = new LogsCVConfiguration();
    target = client.target(url);
    try {
      getRequestBuilderWithAuthHeader(target).post(
          entity(updateBaseline, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
      fail("Did not fail for zero baseline");
    } catch (Exception e) {
      // exepected
    }

    updateBaseline.setBaselineStartMinute(16);
    target = client.target(url);
    try {
      getRequestBuilderWithAuthHeader(target).post(
          entity(updateBaseline, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
      fail("Did not fail for  zero baseline end minute");
    } catch (Exception e) {
      // exepected
    }

    updateBaseline.setBaselineEndMinute(20);
    target = client.target(url);
    try {
      getRequestBuilderWithAuthHeader(target).post(
          entity(updateBaseline, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
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
            .filter("cvConfigId", savedObjectUuid)
            .filter("appId", appId)
            .asList()
            .size());

    assertEquals(100,
        wingsPersistence.createQuery(LogDataRecord.class)
            .filter("cvConfigId", savedObjectUuid)
            .filter("appId", appId)
            .asList()
            .size());

    assertEquals(100,
        wingsPersistence.createQuery(LogMLAnalysisRecord.class)
            .filter("cvConfigId", savedObjectUuid)
            .filter("appId", appId)
            .asList()
            .size());

    updateBaseline.setBaselineEndMinute(38);
    target = client.target(url);
    final RestResponse<Boolean> updateResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(updateBaseline, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
    assertTrue(updateResponse.getResource());

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;

    target = client.target(url);
    RestResponse<LogsCVConfiguration> getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<LogsCVConfiguration>>() {});
    assertEquals(updateBaseline.getBaselineStartMinute(), getRequestResponse.getResource().getBaselineStartMinute());
    assertEquals(updateBaseline.getBaselineEndMinute(), getRequestResponse.getResource().getBaselineEndMinute());

    assertEquals(0,
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class)
            .filter("cvConfigId", savedObjectUuid)
            .filter("appId", appId)
            .asList()
            .size());

    assertEquals(0,
        wingsPersistence.createQuery(LogDataRecord.class)
            .filter("cvConfigId", savedObjectUuid)
            .filter("appId", appId)
            .asList()
            .size());

    assertEquals(16,
        wingsPersistence.createQuery(LogMLAnalysisRecord.class)
            .filter("cvConfigId", savedObjectUuid)
            .filter("appId", appId)
            .asList()
            .size());
  }
}
