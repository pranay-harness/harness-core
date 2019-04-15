package software.wings.verification;

import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;
import software.wings.verification.datadog.DatadogCVServiceConfiguration.DatadogCVConfigurationYaml;

public class DatadogCVConfigurationYamlHandlerTest {
  @Mock YamlHelper yamlHelper;
  @Mock CVConfigurationService cvConfigurationService;
  @Mock EnvironmentService environmentService;
  @Mock ServiceResourceService serviceResourceService;
  @Mock AppService appService;
  @Mock SettingsService settingsService;

  private String envId;
  private String serviceId;
  private String appId;
  private String connectorId;
  private String accountId;

  private String envName = "EnvName";
  private String appName = "AppName";
  private String serviceName = "serviceName";
  private String connectorName = "newRelicConnector";

  DatadogCvConfigurationYamlHandler yamlHandler = new DatadogCvConfigurationYamlHandler();

  @Before
  public void setup() {
    accountId = generateUUID();
    envId = generateUUID();
    serviceId = generateUUID();
    appId = generateUUID();
    connectorId = generateUUID();

    MockitoAnnotations.initMocks(this);
    setInternalState(yamlHandler, "yamlHelper", yamlHelper);
    setInternalState(yamlHandler, "cvConfigurationService", cvConfigurationService);
    setInternalState(yamlHandler, "appService", appService);
    setInternalState(yamlHandler, "environmentService", environmentService);
    setInternalState(yamlHandler, "serviceResourceService", serviceResourceService);
    setInternalState(yamlHandler, "settingsService", settingsService);

    Environment env = Environment.Builder.anEnvironment().withUuid(envId).withName(envName).build();
    when(environmentService.getEnvironmentByName(appId, envName)).thenReturn(env);
    when(environmentService.get(appId, envId)).thenReturn(env);

    Service service = Service.builder().uuid(serviceId).name(serviceName).build();
    when(serviceResourceService.get(appId, serviceId)).thenReturn(service);
    when(serviceResourceService.getServiceByName(appId, serviceName)).thenReturn(service);

    Application app = Application.Builder.anApplication().withName(appName).withUuid(appId).build();
    when(appService.get(appId)).thenReturn(app);
    when(appService.getAppByName(accountId, appName)).thenReturn(app);

    SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withName(connectorName).withUuid(connectorId).build();
    when(settingsService.getSettingAttributeByName(accountId, connectorName)).thenReturn(settingAttribute);
    when(settingsService.get(connectorId)).thenReturn(settingAttribute);
  }

  private void setBasicInfo(DatadogCVServiceConfiguration cvServiceConfiguration) {
    cvServiceConfiguration.setStateType(StateType.NEW_RELIC);
    cvServiceConfiguration.setAccountId(accountId);
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setAppId(appId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setName("TestDDConfig");
  }

  private DatadogCVConfigurationYaml buildYaml() {
    DatadogCVConfigurationYaml yaml = DatadogCVConfigurationYaml.builder()
                                          .applicationFilter("cluster:harness-test")
                                          .metrics("kubernetes.cpu.usage.total")
                                          .build();
    yaml.setName("TestDDConfig");
    yaml.setAccountId(accountId);
    yaml.setServiceName(serviceName);
    yaml.setEnvName(envName);
    yaml.setConnectorName(connectorName);
    yaml.setHarnessApplicationName(appName);
    return yaml;
  }

  @Test
  @Category(UnitTests.class)
  public void testToYaml() {
    final String appId = "appId";
    DatadogCVServiceConfiguration cvServiceConfiguration = DatadogCVServiceConfiguration.builder()
                                                               .applicationFilter("cluster:harness-test")
                                                               .metrics("kubernetes.cpu.usage.total")
                                                               .build();
    setBasicInfo(cvServiceConfiguration);

    DatadogCVConfigurationYaml yaml = yamlHandler.toYaml(cvServiceConfiguration, appId);

    assertEquals("Name should be same", cvServiceConfiguration.getName(), yaml.getName());
    assertEquals("AccountId should be same", cvServiceConfiguration.getAccountId(), yaml.getAccountId());
    assertEquals("ServiceName should be same", serviceName, yaml.getServiceName());
    assertEquals("envId should be same", envName, yaml.getEnvName());
    assertEquals(
        "ApplicationFilter should be same", cvServiceConfiguration.getApplicationFilter(), yaml.getApplicationFilter());
    assertEquals("Metrics value shouldbe same", cvServiceConfiguration.getMetrics(), yaml.getMetrics());
  }

  @Test
  @Category(UnitTests.class)
  public void testUpsert() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestDDConfig.yaml")).thenReturn("TestDDConfig");

    ChangeContext<DatadogCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId("accountId").withFilePath("TestDDConfig.yaml").build();
    changeContext.setChange(c);
    changeContext.setYaml(buildYaml());
    DatadogCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);

    assertEquals("name should match", "TestDDConfig", bean.getName());
    assertEquals("appId should match", appId, bean.getAppId());
    assertEquals("envId should match", envId, bean.getEnvId());
    assertEquals("serviceId should match", serviceId, bean.getServiceId());
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void testUpsertMissingMetrics() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    ChangeContext<DatadogCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId("accountId").withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    DatadogCVConfigurationYaml yaml = buildYaml();
    changeContext.setYaml(yaml);
    yaml.setMetrics(null);
    DatadogCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void testUpsertIncorrectMetrics() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    ChangeContext<DatadogCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId("accountId").withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    DatadogCVConfigurationYaml yaml = buildYaml();
    changeContext.setYaml(yaml);
    yaml.setMetrics("mytestKubernetesMetric");
    DatadogCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void testUpsertMissingAppFilter() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    ChangeContext<DatadogCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId("accountId").withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    DatadogCVConfigurationYaml yaml = buildYaml();
    changeContext.setYaml(yaml);
    yaml.setApplicationFilter(null);
    DatadogCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);
  }

  @Test
  @Category(UnitTests.class)
  public void testUpsertMissingAppFilterHasServiceName() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    ChangeContext<DatadogCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId("accountId").withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    DatadogCVConfigurationYaml yaml = buildYaml();
    changeContext.setYaml(yaml);
    yaml.setApplicationFilter(null);
    yaml.setDatadogServiceName("todolist");
    DatadogCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);

    assertEquals("Service name matches", "todolist", bean.getDatadogServiceName());
  }

  @Test
  @Category(UnitTests.class)
  public void testUpsertAlreadyExisting() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestDDConfig.yaml")).thenReturn("TestDDConfig");

    DatadogCVServiceConfiguration cvConfig = DatadogCVServiceConfiguration.builder().build();
    cvConfig.setUuid("testUUID");
    when(cvConfigurationService.getConfiguration("TestDDConfig", appId, envId)).thenReturn(cvConfig);
    ChangeContext<DatadogCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId("accountId").withFilePath("TestDDConfig.yaml").build();
    changeContext.setChange(c);
    changeContext.setYaml(buildYaml());
    DatadogCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);

    assertEquals("name should match", "TestDDConfig", bean.getName());
    assertEquals("UUID should match", cvConfig.getUuid(), bean.getUuid());
  }
}
