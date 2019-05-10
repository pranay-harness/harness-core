package software.wings.verification;

import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.metrics.MetricType;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.prometheus.PrometheusCVServiceConfiguration;

import java.util.ArrayList;
import java.util.List;

public class PrometheusCVConfigurationYamlHandlerTest extends WingsBaseTest {
  @Mock YamlHelper yamlHelper;
  @Mock CVConfigurationService cvConfigurationService;
  @Mock EnvironmentService environmentService;
  @Mock ServiceResourceService serviceResourceService;
  @Mock AppService appService;
  @Mock SettingsService settingsService;

  PrometheusCVConfigurationYamlHandler yamlHandler = new PrometheusCVConfigurationYamlHandler();

  private String envId;
  private String serviceId;
  private String appId;
  private String connectorId;
  private String accountId;

  private String envName = "EnvName";
  private String appName = "AppName";
  private String serviceName = "serviceName";
  private String connectorName = "prometheusConnector";

  @Before
  public void setup() throws Exception {
    accountId = generateUUID();
    envId = generateUUID();
    serviceId = generateUUID();
    appId = generateUUID();
    connectorId = generateUUID();

    MockitoAnnotations.initMocks(this);

    Environment env = Environment.Builder.anEnvironment().uuid(envId).name(envName).build();
    when(environmentService.getEnvironmentByName(appId, envName)).thenReturn(env);
    when(environmentService.get(appId, envId)).thenReturn(env);

    Service service = Service.builder().uuid(serviceId).name(serviceName).build();
    when(serviceResourceService.get(appId, serviceId)).thenReturn(service);
    when(serviceResourceService.getServiceByName(appId, serviceName)).thenReturn(service);

    Application app = Application.Builder.anApplication().name(appName).uuid(appId).build();
    when(appService.get(appId)).thenReturn(app);
    when(appService.getAppByName(accountId, appName)).thenReturn(app);

    SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withName(connectorName).withUuid(connectorId).build();
    when(settingsService.getSettingAttributeByName(accountId, connectorName)).thenReturn(settingAttribute);
    when(settingsService.get(connectorId)).thenReturn(settingAttribute);

    FieldUtils.writeField(yamlHandler, "environmentService", environmentService, true);
    FieldUtils.writeField(yamlHandler, "serviceResourceService", serviceResourceService, true);
    FieldUtils.writeField(yamlHandler, "settingsService", settingsService, true);
    FieldUtils.writeField(yamlHandler, "yamlHelper", yamlHelper, true);
    FieldUtils.writeField(yamlHandler, "cvConfigurationService", cvConfigurationService, true);
    FieldUtils.writeField(yamlHandler, "appService", appService, true);
  }

  private void setBasicInfo(PrometheusCVServiceConfiguration cvServiceConfiguration) {
    cvServiceConfiguration.setStateType(StateType.PROMETHEUS);
    cvServiceConfiguration.setAccountId(accountId);
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setAppId(appId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setName("TestPrometheusConfig");
  }

  private PrometheusCVServiceConfiguration.PrometheusCVConfigurationYaml buildYaml(List<TimeSeries> timeSeriesList) {
    PrometheusCVServiceConfiguration.PrometheusCVConfigurationYaml yaml =
        PrometheusCVServiceConfiguration.PrometheusCVConfigurationYaml.builder().timeSeriesList(timeSeriesList).build();
    yaml.setName("TestPrometheusConfig");
    yaml.setAccountId(accountId);
    yaml.setServiceName(serviceName);
    yaml.setEnvName(envName);
    yaml.setConnectorName(connectorName);
    yaml.setHarnessApplicationName(appName);
    return yaml;
  }

  private void mockYamlHelper() {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestPrometheusConfig.yaml")).thenReturn("TestPrometheusConfig");
  }

  @Test
  @Category(UnitTests.class)
  public void testToYaml() {
    final String appId = "appId";
    List<TimeSeries> timeSeriesList = new ArrayList<>();
    String url = "http://35.247.2.110:8080?hostname=$hostName&startTime=$startTime&endTime=$endTime";
    timeSeriesList.add(
        TimeSeries.builder().metricName("Metric1").metricType("Error").txnName("Test1").url(url).build());

    PrometheusCVServiceConfiguration cvServiceConfiguration =
        PrometheusCVServiceConfiguration.builder().timeSeriesToAnalyze(timeSeriesList).build();
    setBasicInfo(cvServiceConfiguration);

    PrometheusCVServiceConfiguration.PrometheusCVConfigurationYaml yaml =
        yamlHandler.toYaml(cvServiceConfiguration, appId);

    assertEquals("Name should be same", cvServiceConfiguration.getName(), yaml.getName());
    assertEquals("AccountId should be same", cvServiceConfiguration.getAccountId(), yaml.getAccountId());
    assertEquals("serviceId should be same", serviceName, yaml.getServiceName());
    assertEquals("envId should be same", envName, yaml.getEnvName());
    assertEquals("Metrics should be same", timeSeriesList, yaml.getTimeSeriesList());
    assertEquals("AppD applicationID should be same", appName, yaml.getHarnessApplicationName());
  }

  @Test
  @Category(UnitTests.class)
  public void testUpsert() throws Exception {
    mockYamlHelper();

    List<TimeSeries> timeSeriesList = new ArrayList<>();
    String url = "http://35.247.2.110:8080?hostname=$hostName&startTime=$startTime&endTime=$endTime";
    timeSeriesList.add(TimeSeries.builder()
                           .metricName("Metric1")
                           .metricType(MetricType.INFRA.name())
                           .txnName("Test1")
                           .url(url)
                           .build());

    ChangeContext<PrometheusCVServiceConfiguration.PrometheusCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId(accountId).withFilePath("TestPrometheusConfig.yaml").build();
    changeContext.setChange(c);
    changeContext.setYaml(buildYaml(timeSeriesList));
    PrometheusCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);

    assertEquals("name should match", "TestPrometheusConfig", bean.getName());
    assertEquals("appId should match", appId, bean.getAppId());
    assertEquals("envId should match", envId, bean.getEnvId());
    assertEquals("serviceId should match", serviceId, bean.getServiceId());
    assertEquals("metrics should match", timeSeriesList, bean.getTimeSeriesToAnalyze());
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void testUpsertBadMetricsEmptyMetrics() throws Exception {
    mockYamlHelper();

    List<TimeSeries> timeSeriesList = new ArrayList<>();

    ChangeContext<PrometheusCVServiceConfiguration.PrometheusCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId(accountId).withFilePath("TestPrometheusConfig.yaml").build();
    changeContext.setChange(c);
    PrometheusCVServiceConfiguration.PrometheusCVConfigurationYaml yaml = buildYaml(timeSeriesList);
    changeContext.setYaml(yaml);
    yamlHandler.upsertFromYaml(changeContext, null);
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void testUpsertBadMetricsBadUrl() throws Exception {
    mockYamlHelper();

    List<TimeSeries> timeSeriesList = new ArrayList<>();
    String url = "http://35.247.2.110:8080?hostname=$hostName";
    timeSeriesList.add(
        TimeSeries.builder().metricName("Metric1").metricType("Error").txnName("Test1").url(url).build());

    ChangeContext<PrometheusCVServiceConfiguration.PrometheusCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId(accountId).withFilePath("TestPrometheusConfig.yaml").build();
    changeContext.setChange(c);
    PrometheusCVServiceConfiguration.PrometheusCVConfigurationYaml yaml = buildYaml(timeSeriesList);
    changeContext.setYaml(yaml);
    yamlHandler.upsertFromYaml(changeContext, null);
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void testUpsertBadMetricsDuplicate() throws Exception {
    mockYamlHelper();

    List<TimeSeries> timeSeriesList = new ArrayList<>();
    String url = "http://35.247.2.110:8080?hostname=$hostName&startTime=$startTime&endTime=$endTime";
    timeSeriesList.add(
        TimeSeries.builder().metricName("Metric1").metricType("Error").txnName("Test1").url(url).build());
    timeSeriesList.add(TimeSeries.builder().metricName("Metric1").metricType("Logs").txnName("Test2").url(url).build());

    ChangeContext<PrometheusCVServiceConfiguration.PrometheusCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId(accountId).withFilePath("TestPrometheusConfig.yaml").build();
    changeContext.setChange(c);
    PrometheusCVServiceConfiguration.PrometheusCVConfigurationYaml yaml = buildYaml(timeSeriesList);
    changeContext.setYaml(yaml);
    yamlHandler.upsertFromYaml(changeContext, null);
  }
}
