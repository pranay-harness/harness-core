package software.wings.service.impl.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.waiter.WaitNotifyEngine;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.APMFetchConfig;
import software.wings.WingsBaseTest;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.AwsConfig;
import software.wings.beans.DatadogConfig;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.apm.APMDataCollectionInfo;
import software.wings.service.impl.appdynamics.AppdynamicsDataCollectionInfo;
import software.wings.service.impl.cloudwatch.CloudWatchDataCollectionInfo;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;
import software.wings.service.impl.prometheus.PrometheusDataCollectionInfo;
import software.wings.service.intfc.CloudWatchService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.StateType;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;
import software.wings.verification.cloudwatch.CloudWatchCVServiceConfiguration;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;
import software.wings.verification.prometheus.PrometheusCVServiceConfiguration;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Praveen 9/6/18
 */
public class APMVerificationServiceImplTest extends WingsBaseTest {
  @Mock private SettingsService mockSettingsService;
  @Mock private DelegateProxyFactory mockDelegateProxyFactory;
  @Mock private APMDelegateService mockAPMDelegateService;
  @Mock private DelegateService mockDelegateService;
  @Mock private WaitNotifyEngine mockWaitNotifyEngine;
  @Mock private SecretManager mockSecretManager;
  @Mock private CloudWatchService cloudWatchService;
  @InjectMocks ContinuousVerificationServiceImpl service;
  @Inject WingsPersistence wingsPersistence;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    setInternalState(service, "wingsPersistence", wingsPersistence);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetNodeDataValidCase() {
    APMVerificationConfig config = new APMVerificationConfig();
    config.setValidationUrl("this is a testurl");
    config.setUrl("this is a test url");
    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(config);

    String dummyResponseString = "{ 'key1':'value1'}";

    APMFetchConfig fetchConfig = APMFetchConfig.builder().url("testFetchURL.com").build();

    // setup
    when(mockSettingsService.get(anyString())).thenReturn(attribute);
    when(mockDelegateProxyFactory.get(any(), any())).thenReturn(mockAPMDelegateService);
    when(mockAPMDelegateService.fetch(any(APMValidateCollectorConfig.class))).thenReturn(dummyResponseString);

    // execute
    VerificationNodeDataSetupResponse response =
        service.getMetricsWithDataForNode("accountId", "serverConfigId", fetchConfig, StateType.APM_VERIFICATION);

    // verify
    assertNotNull(response);
    assertTrue(response.getLoadResponse().isLoadPresent());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetNodeDataValidNoLoad() {
    APMVerificationConfig config = new APMVerificationConfig();
    config.setValidationUrl("this is a testurl");
    config.setUrl("this is a test url");
    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(config);

    String dummyResponseString = "{}";

    APMFetchConfig fetchConfig = APMFetchConfig.builder().url("testFetchURL.com").build();

    // setup
    when(mockSettingsService.get(anyString())).thenReturn(attribute);
    when(mockDelegateProxyFactory.get(any(), any())).thenReturn(mockAPMDelegateService);
    when(mockAPMDelegateService.fetch(any(APMValidateCollectorConfig.class))).thenReturn(dummyResponseString);

    // execute
    VerificationNodeDataSetupResponse response =
        service.getMetricsWithDataForNode("accountId", "serverConfigId", fetchConfig, StateType.APM_VERIFICATION);

    // verify
    assertNotNull(response);
    assertFalse(response.getLoadResponse().isLoadPresent());
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void testGetNodeDataNullServerConfigId() {
    APMVerificationConfig config = new APMVerificationConfig();
    config.setValidationUrl("this is a testurl");
    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(config);

    String dummyResponseString = "{}";

    APMFetchConfig fetchConfig = APMFetchConfig.builder().url("testFetchURL.com").build();

    // setup
    when(mockSettingsService.get(anyString())).thenReturn(attribute);
    when(mockDelegateProxyFactory.get(any(), any())).thenReturn(mockAPMDelegateService);
    when(mockAPMDelegateService.fetch(any(APMValidateCollectorConfig.class))).thenReturn(dummyResponseString);

    // execute
    VerificationNodeDataSetupResponse response =
        service.getMetricsWithDataForNode("accountId", null, fetchConfig, StateType.APM_VERIFICATION);
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void testGetNodeDataNullFetchConfig() {
    APMVerificationConfig config = new APMVerificationConfig();
    config.setValidationUrl("this is a testurl");
    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(config);

    String dummyResponseString = "{}";

    APMFetchConfig fetchConfig = APMFetchConfig.builder().url("testFetchURL.com").build();

    // setup
    when(mockSettingsService.get(anyString())).thenReturn(attribute);
    when(mockDelegateProxyFactory.get(any(), any())).thenReturn(mockAPMDelegateService);
    when(mockAPMDelegateService.fetch(any(APMValidateCollectorConfig.class))).thenReturn(dummyResponseString);

    // execute
    VerificationNodeDataSetupResponse response =
        service.getMetricsWithDataForNode("accountId", "serverId", null, StateType.APM_VERIFICATION);
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void testGetNodeDataExceptionWhileFetch() {
    APMVerificationConfig config = new APMVerificationConfig();
    config.setValidationUrl("this is a testurl");
    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(config);

    String dummyResponseString = "{}";

    APMFetchConfig fetchConfig = APMFetchConfig.builder().url("testFetchURL.com").build();

    // setup
    when(mockSettingsService.get(anyString())).thenReturn(attribute);
    when(mockDelegateProxyFactory.get(any(), any())).thenReturn(mockAPMDelegateService);
    when(mockAPMDelegateService.fetch(any(APMValidateCollectorConfig.class))).thenThrow(new WingsException(""));

    // execute
    VerificationNodeDataSetupResponse response =
        service.getMetricsWithDataForNode("accountId", "serverId", null, StateType.APM_VERIFICATION);
  }

  @Test
  @Category(UnitTests.class)
  public void testCreateDelegateTaskDatadog() {
    // Setup
    DatadogConfig dConfg = DatadogConfig.builder().accountId("accountId").url("datadogUrl").build();
    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(dConfg);

    DatadogCVServiceConfiguration config =
        DatadogCVServiceConfiguration.builder().metrics("docker.mem.rss,kubernetes.memory.usage").build();
    config.setConnectorId("connectorId");
    config.setUuid("cvConfigId");
    config.setAppId("appId");
    config.setServiceId("serviceId");
    config.setStateType(StateType.DATA_DOG);
    wingsPersistence.save(config);

    when(mockSettingsService.get(anyString())).thenReturn(attribute);
    when(mockWaitNotifyEngine.waitForAll(anyObject(), anyString())).thenReturn("waitId");
    when(mockSecretManager.getEncryptionDetails(dConfg, "appId", null)).thenReturn(new ArrayList<>());

    // execute behavior
    boolean response = service.collect247Data("cvConfigId", StateType.DATA_DOG, 1540419553000l, 1540420454000l);

    // verify
    assertTrue(response);
    ArgumentCaptor<DelegateTask> taskCaptor = ArgumentCaptor.forClass(DelegateTask.class);

    verify(mockWaitNotifyEngine).waitForAll(anyObject(), anyString());
    verify(mockDelegateService).queueTask(taskCaptor.capture());
    assertEquals("Task type should match", taskCaptor.getValue().getData().getTaskType(),
        TaskType.APM_24_7_METRIC_DATA_COLLECTION_TASK.name());
    APMDataCollectionInfo dataCollectionInfo =
        (APMDataCollectionInfo) taskCaptor.getValue().getData().getParameters()[0];
    assertEquals(
        "State type in datacollectionInfo should match", dataCollectionInfo.getStateType(), StateType.DATA_DOG);
    assertEquals("Start time should match", dataCollectionInfo.getStartTime(), 1540419553000l);
    assertEquals("Time duration should match", dataCollectionInfo.getDataCollectionTotalTime(), 15);
  }

  @Test
  @Category(UnitTests.class)
  public void testCreateAppD24x7Task() {
    // Setup
    AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder().build();
    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(appDynamicsConfig);

    AppDynamicsCVServiceConfiguration config =
        AppDynamicsCVServiceConfiguration.builder().appDynamicsApplicationId("1234").tierId("5678").build();
    config.setConnectorId("connectorId");
    config.setUuid("cvConfigId");
    config.setAppId("appId");
    config.setServiceId("serviceId");
    config.setStateType(StateType.APP_DYNAMICS);
    wingsPersistence.save(config);

    when(mockSettingsService.get(anyString())).thenReturn(attribute);
    when(mockWaitNotifyEngine.waitForAll(anyObject(), anyString())).thenReturn("waitId");
    when(mockSecretManager.getEncryptionDetails(appDynamicsConfig, "appId", null)).thenReturn(new ArrayList<>());

    // execute behavior
    boolean response = service.collect247Data("cvConfigId", StateType.APP_DYNAMICS, 1540419553000l, 1540420454000l);

    // verify
    assertTrue(response);
    ArgumentCaptor<DelegateTask> taskCaptor = ArgumentCaptor.forClass(DelegateTask.class);

    verify(mockWaitNotifyEngine).waitForAll(anyObject(), anyString());
    verify(mockDelegateService).queueTask(taskCaptor.capture());
    assertEquals("Task type should match", taskCaptor.getValue().getData().getTaskType(),
        TaskType.APPDYNAMICS_COLLECT_24_7_METRIC_DATA.name());
    AppdynamicsDataCollectionInfo dataCollectionInfo =
        (AppdynamicsDataCollectionInfo) taskCaptor.getValue().getData().getParameters()[0];
    assertEquals(dataCollectionInfo.getAppDynamicsConfig(), appDynamicsConfig);
    assertEquals(dataCollectionInfo.getCollectionTime(), 15);
    assertEquals(dataCollectionInfo.getStartTime(), 1540419553000l);
    assertEquals(dataCollectionInfo.getTimeSeriesMlAnalysisType(), TimeSeriesMlAnalysisType.PREDICTIVE);
  }

  @Test
  @Category(UnitTests.class)
  public void testCreateNewRelic24x7Task() {
    NewRelicConfig nrConfig = NewRelicConfig.builder().build();
    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(nrConfig);

    NewRelicCVServiceConfiguration config = NewRelicCVServiceConfiguration.builder().applicationId("1234").build();
    config.setConnectorId("connectorId");
    config.setUuid("cvConfigId");
    config.setAppId("appId");
    config.setServiceId("serviceId");
    config.setStateType(StateType.NEW_RELIC);
    wingsPersistence.save(config);

    when(mockSettingsService.get(anyString())).thenReturn(attribute);
    when(mockWaitNotifyEngine.waitForAll(anyObject(), anyString())).thenReturn("waitId");
    when(mockSecretManager.getEncryptionDetails(nrConfig, "appId", null)).thenReturn(new ArrayList<>());

    // execute behavior
    boolean response = service.collect247Data("cvConfigId", StateType.NEW_RELIC, 1540419553000l, 1540420454000l);

    // verify
    assertTrue(response);
    ArgumentCaptor<DelegateTask> taskCaptor = ArgumentCaptor.forClass(DelegateTask.class);

    verify(mockWaitNotifyEngine).waitForAll(anyObject(), anyString());
    verify(mockDelegateService).queueTask(taskCaptor.capture());
    assertEquals("Task type should match", taskCaptor.getValue().getData().getTaskType(),
        TaskType.NEWRELIC_COLLECT_24_7_METRIC_DATA.name());
    NewRelicDataCollectionInfo dataCollectionInfo =
        (NewRelicDataCollectionInfo) taskCaptor.getValue().getData().getParameters()[0];
    assertEquals(dataCollectionInfo.getNewRelicConfig(), nrConfig);
    assertEquals(dataCollectionInfo.getCollectionTime(), 15);
    assertEquals(dataCollectionInfo.getStartTime(), 1540419553000l);
    assertEquals(dataCollectionInfo.getTimeSeriesMlAnalysisType(), TimeSeriesMlAnalysisType.PREDICTIVE);
  }

  @Test
  @Category(UnitTests.class)
  public void testCreate24x7TaskBadState() {
    boolean response = service.collect247Data("cvConfigId", StateType.HTTP, 1540419553000l, 1540420454000l);
    assertFalse(response);
  }

  @Test
  @Category(UnitTests.class)
  public void testCreate24x7TaskPrometheus() {
    PrometheusConfig nrConfig = PrometheusConfig.builder().build();
    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(nrConfig);

    PrometheusCVServiceConfiguration config = PrometheusCVServiceConfiguration.builder().build();
    config.setConnectorId("connectorId");
    config.setUuid("cvConfigId");
    config.setAppId("appId");
    config.setServiceId("serviceId");
    config.setStateType(StateType.PROMETHEUS);
    wingsPersistence.save(config);

    when(mockSettingsService.get(anyString())).thenReturn(attribute);
    when(mockWaitNotifyEngine.waitForAll(anyObject(), anyString())).thenReturn("waitId");
    when(mockSecretManager.getEncryptionDetails(nrConfig, "appId", null)).thenReturn(new ArrayList<>());

    // execute behavior
    boolean response = service.collect247Data("cvConfigId", StateType.PROMETHEUS, 1540419553000l, 1540420454000l);

    // verify
    assertTrue(response);
    ArgumentCaptor<DelegateTask> taskCaptor = ArgumentCaptor.forClass(DelegateTask.class);

    verify(mockWaitNotifyEngine).waitForAll(anyObject(), anyString());
    verify(mockDelegateService).queueTask(taskCaptor.capture());
    assertEquals("Task type should match", taskCaptor.getValue().getData().getTaskType(),
        TaskType.PROMETHEUS_COLLECT_24_7_METRIC_DATA.name());
    PrometheusDataCollectionInfo dataCollectionInfo =
        (PrometheusDataCollectionInfo) taskCaptor.getValue().getData().getParameters()[0];
    assertEquals(dataCollectionInfo.getCollectionTime(), 15);
    assertEquals(dataCollectionInfo.getStartTime(), 1540419553000l);
    assertEquals(dataCollectionInfo.getTimeSeriesMlAnalysisType(), TimeSeriesMlAnalysisType.PREDICTIVE);
  }

  @Test
  @Category(UnitTests.class)
  public void testCreate24x7TaskCloudWatch() {
    AwsConfig awsConfig = AwsConfig.builder().build();
    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(awsConfig);
    CloudWatchCVServiceConfiguration config = CloudWatchCVServiceConfiguration.builder().build();
    config.setConnectorId("connectorId");
    config.setUuid("cvConfigId");
    config.setAppId("appId");
    config.setServiceId("serviceId");
    config.setStateType(StateType.CLOUD_WATCH);
    wingsPersistence.save(config);
    when(mockSettingsService.get(anyString())).thenReturn(attribute);
    when(mockWaitNotifyEngine.waitForAll(anyObject(), anyString())).thenReturn("waitId");
    when(mockSecretManager.getEncryptionDetails(awsConfig, "appId", null)).thenReturn(new ArrayList<>());
    when(cloudWatchService.createLambdaFunctionNames(anyList())).thenReturn(new HashMap());
    // execute behavior
    boolean response = service.collect247Data("cvConfigId", StateType.CLOUD_WATCH, 1540419553000l, 1540420454000l);
    // verify
    assertTrue(response);
    ArgumentCaptor<DelegateTask> taskCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(mockWaitNotifyEngine).waitForAll(anyObject(), anyString());
    verify(mockDelegateService).queueTask(taskCaptor.capture());
    assertEquals("Task type should match", taskCaptor.getValue().getData().getTaskType(),
        TaskType.CLOUD_WATCH_COLLECT_24_7_METRIC_DATA.name());
    CloudWatchDataCollectionInfo dataCollectionInfo =
        (CloudWatchDataCollectionInfo) taskCaptor.getValue().getData().getParameters()[0];
    assertEquals(dataCollectionInfo.getCollectionTime(), 15);
    assertEquals(dataCollectionInfo.getStartTime(), 1540419553000l);
    assertEquals(dataCollectionInfo.getAnalysisComparisonStrategy(), AnalysisComparisonStrategy.PREDICTIVE);
  }
}
