package software.wings.integration.verification;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.sm.StateType.APP_DYNAMICS;
import static software.wings.sm.StateType.CLOUD_WATCH;
import static software.wings.sm.StateType.DATA_DOG;
import static software.wings.sm.StateType.DYNA_TRACE;
import static software.wings.sm.StateType.ELK;
import static software.wings.sm.StateType.NEW_RELIC;
import static software.wings.sm.StateType.PROMETHEUS;
import static software.wings.utils.WingsTestConstants.mockChecker;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.limits.LimitCheckerFactory;
import io.harness.serializer.JsonUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.service.intfc.AppService;
import software.wings.verification.CVConfiguration;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;
import software.wings.verification.cloudwatch.CloudWatchCVServiceConfiguration;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;
import software.wings.verification.dynatrace.DynaTraceCVServiceConfiguration;
import software.wings.verification.log.ElkCVConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;
import software.wings.verification.prometheus.PrometheusCVServiceConfiguration;

import java.util.ArrayList;
import java.util.Collections;
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

    elkCVConfiguration.setQuery("query1");
    elkCVConfiguration.setFormattedQuery(true);
    elkCVConfiguration.setQueryType("TERM");
    elkCVConfiguration.setIndices("index1");
    elkCVConfiguration.setMessageField("message1");
    elkCVConfiguration.setTimestampField("timestamp1");
    elkCVConfiguration.setTimestampFormat("timestamp_format1");
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
    RestResponse<T> getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<T>>() {});
    T fetchedObject = getRequestResponse.getResource();
    if (fetchedObject instanceof AppDynamicsCVServiceConfiguration) {
      AppDynamicsCVServiceConfiguration obj = (AppDynamicsCVServiceConfiguration) fetchedObject;
      assertEquals(savedObjectUuid, obj.getUuid());
      assertEquals(accountId, obj.getAccountId());
      assertEquals(appId, obj.getAppId());
      assertEquals(envId, obj.getEnvId());
      assertEquals(serviceId, obj.getServiceId());
      assertEquals(APP_DYNAMICS, obj.getStateType());
      assertEquals(appDynamicsApplicationId, obj.getAppDynamicsApplicationId());
      assertEquals(AnalysisTolerance.HIGH, obj.getAnalysisTolerance());
    }
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
    RestResponse<T> getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<T>>() {});
    T fetchedObject = getRequestResponse.getResource();
    if (fetchedObject instanceof DatadogCVServiceConfiguration) {
      DatadogCVServiceConfiguration obj = (DatadogCVServiceConfiguration) fetchedObject;
      assertEquals(savedObjectUuid, obj.getUuid());
      assertEquals(accountId, obj.getAccountId());
      assertEquals(appId, obj.getAppId());
      assertEquals(envId, obj.getEnvId());
      assertEquals(serviceId, obj.getServiceId());
      assertEquals(DATA_DOG, obj.getStateType());
      assertEquals(AnalysisTolerance.HIGH, obj.getAnalysisTolerance());
      assertEquals("trace.servlet.request.errors, system.mem.used, system.cpu.iowait", obj.getMetrics());
    }

    // Test PUT API for Datadog
    datadogCVServiceConfiguration.setName("Datadog Config");
    datadogCVServiceConfiguration.setEnabled24x7(false);
    datadogCVServiceConfiguration.setMetrics("system.mem.used, system.cpu.iowait");
    datadogCVServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);

    // Call PUT
    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId + "&appId=" + appId
        + "&stateType=" + DATA_DOG + "&serviceConfigurationId=" + savedObjectUuid;
    target = client.target(url);
    getRequestBuilderWithAuthHeader(target).put(
        entity(datadogCVServiceConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});

    // Call GET
    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;
    target = client.target(url);
    getRequestResponse = getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<T>>() {});
    fetchedObject = getRequestResponse.getResource();

    // Assert
    if (fetchedObject instanceof DatadogCVServiceConfiguration) {
      DatadogCVServiceConfiguration obj = (DatadogCVServiceConfiguration) fetchedObject;
      assertEquals("Datadog Confid", obj.getName());
      assertFalse(obj.isEnabled24x7());
      assertEquals("system.mem.used, system.cpu.iowait", obj.getMetrics());
      assertEquals(AnalysisTolerance.MEDIUM, obj.getAnalysisTolerance());
    }
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
    RestResponse<T> getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<T>>() {});
    T fetchedObject = getRequestResponse.getResource();
    if (fetchedObject instanceof PrometheusCVServiceConfiguration) {
      PrometheusCVServiceConfiguration obj = (PrometheusCVServiceConfiguration) fetchedObject;
      assertEquals(savedObjectUuid, obj.getUuid());
      assertEquals(accountId, obj.getAccountId());
      assertEquals(appId, obj.getAppId());
      assertEquals(envId, obj.getEnvId());
      assertEquals(serviceId, obj.getServiceId());
      assertEquals(PROMETHEUS, obj.getStateType());
      assertEquals(prometheusCVServiceConfiguration.getTimeSeriesToAnalyze(), obj.getTimeSeriesToAnalyze());
    }
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
    RestResponse<T> getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<T>>() {});
    T fetchedObject = getRequestResponse.getResource();
    if (fetchedObject instanceof DynaTraceCVServiceConfiguration) {
      DynaTraceCVServiceConfiguration obj = (DynaTraceCVServiceConfiguration) fetchedObject;
      assertEquals(savedObjectUuid, obj.getUuid());
      assertEquals(accountId, obj.getAccountId());
      assertEquals(appId, obj.getAppId());
      assertEquals(envId, obj.getEnvId());
      assertEquals(serviceId, obj.getServiceId());
      assertEquals(DYNA_TRACE, obj.getStateType());
      assertEquals(dynaTraceCVServiceConfiguration.getServiceMethods(), obj.getServiceMethods());
      assertEquals(AnalysisTolerance.HIGH, obj.getAnalysisTolerance());
    }
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
    RestResponse<T> getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<T>>() {});
    T fetchedObject = getRequestResponse.getResource();
    if (fetchedObject instanceof CloudWatchCVServiceConfiguration) {
      CloudWatchCVServiceConfiguration obj = (CloudWatchCVServiceConfiguration) fetchedObject;
      assertEquals(savedObjectUuid, obj.getUuid());
      assertEquals(accountId, obj.getAccountId());
      assertEquals(appId, obj.getAppId());
      assertEquals(envId, obj.getEnvId());
      assertEquals(serviceId, obj.getServiceId());
      assertEquals(CLOUD_WATCH, obj.getStateType());
      assertEquals(cloudWatchCVServiceConfiguration.getLoadBalancerMetrics(), obj.getLoadBalancerMetrics());
    }
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

    assertEquals("query1", fetchedObject.getQuery());
    assertEquals(true, fetchedObject.isFormattedQuery());
    assertEquals("TERM", fetchedObject.getQueryType());
    assertEquals("index1", fetchedObject.getIndices());
    assertEquals("message1", fetchedObject.getMessageField());
    assertEquals("timestamp1", fetchedObject.getTimestampField());
    assertEquals("timestamp_format1", fetchedObject.getTimestampFormat());

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
    assertEquals(ELK, obj.getStateType());
    assertEquals(AnalysisTolerance.MEDIUM, obj.getAnalysisTolerance());
    assertEquals("Config 1", obj.getName());

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId + "&appId=" + appId
        + "&stateType=" + ELK + "&serviceConfigurationId=" + savedObjectUuid;
    target = client.target(url);
    elkCVServiceConfiguration.setName("Config 2");
    elkCVServiceConfiguration.setEnabled24x7(false);

    elkCVServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.LOW);
    elkCVServiceConfiguration.setQuery("query2");
    elkCVServiceConfiguration.setFormattedQuery(false);
    elkCVServiceConfiguration.setQueryType("MATCH");
    elkCVServiceConfiguration.setIndices("index2");
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
    assertEquals("Config 2", fetchedObject.getName());
    assertEquals("query2", fetchedObject.getQuery());
    assertEquals(false, fetchedObject.isFormattedQuery());
    assertEquals("MATCH", fetchedObject.getQueryType());
    assertEquals("index2", fetchedObject.getIndices());
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

    // Save 2 cvConfigs with the same appId but different envIds
    String url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + NEW_RELIC;
    WebTarget target = client.target(url);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(newRelicCVServiceConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + APP_DYNAMICS;
    target = client.target(url);
    restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(appDynamicsCVServiceConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});

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
}
