package software.wings.verification;

import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.apache.commons.lang3.reflect.FieldUtils;
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
import software.wings.verification.log.LogsCVConfiguration;
import software.wings.verification.log.LogsCVConfiguration.LogsCVConfigurationYaml;

public class LogsCVConfigurationYamlHandlerTest extends CategoryTest {
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

  LogsCVConfigurationYamlHandler yamlHandler = new LogsCVConfigurationYamlHandler();

  @Before
  public void setup() throws IllegalAccessException {
    accountId = generateUUID();
    envId = generateUUID();
    serviceId = generateUUID();
    appId = generateUUID();
    connectorId = generateUUID();

    MockitoAnnotations.initMocks(this);
    FieldUtils.writeField(yamlHandler, "yamlHelper", yamlHelper, true);
    FieldUtils.writeField(yamlHandler, "cvConfigurationService", cvConfigurationService, true);
    FieldUtils.writeField(yamlHandler, "appService", appService, true);
    FieldUtils.writeField(yamlHandler, "environmentService", environmentService, true);
    FieldUtils.writeField(yamlHandler, "serviceResourceService", serviceResourceService, true);
    FieldUtils.writeField(yamlHandler, "settingsService", settingsService, true);

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
  }

  private void setBasicInfo(LogsCVConfiguration cvServiceConfiguration) {
    cvServiceConfiguration.setStateType(StateType.SUMO);
    cvServiceConfiguration.setAccountId(accountId);
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setAppId(appId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setName("TestAppDConfig");
  }

  private LogsCVConfigurationYaml buildYaml() {
    LogsCVConfigurationYaml yaml = new LogsCVConfigurationYaml();
    yaml.setType(StateType.SUMO.name());
    yaml.setName("TestAppDConfig");
    yaml.setAccountId(accountId);
    yaml.setServiceName(serviceName);
    yaml.setEnvName(envName);
    yaml.setConnectorName(connectorName);
    yaml.setHarnessApplicationName(appName);
    yaml.setQuery("query");
    yaml.setBaselineStartMinute(16);
    yaml.setBaselineEndMinute(30);
    return yaml;
  }

  @Test
  @Category(UnitTests.class)
  public void testToYaml() {
    final String appId = "appId";
    LogsCVConfiguration cvServiceConfiguration = new LogsCVConfiguration();
    setBasicInfo(cvServiceConfiguration);
    cvServiceConfiguration.setQuery(generateUUID());
    cvServiceConfiguration.setBaselineStartMinute(16);
    cvServiceConfiguration.setBaselineEndMinute(30);

    LogsCVConfigurationYaml yaml = yamlHandler.toYaml(cvServiceConfiguration, appId);

    assertEquals("Name should be same", cvServiceConfiguration.getName(), yaml.getName());
    assertEquals("AccountId should be same", cvServiceConfiguration.getAccountId(), yaml.getAccountId());
    assertEquals("ServiceName should be same", serviceName, yaml.getServiceName());
    assertEquals("envId should be same", envName, yaml.getEnvName());
    assertEquals("query should be same", cvServiceConfiguration.getQuery(), yaml.getQuery());
    assertEquals(cvServiceConfiguration.getBaselineStartMinute(), yaml.getBaselineStartMinute());
    assertEquals(cvServiceConfiguration.getBaselineEndMinute(), yaml.getBaselineEndMinute());
  }

  @Test
  @Category(UnitTests.class)
  public void testUpsert() {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    ChangeContext<LogsCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId("accountId").withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    changeContext.setYaml(buildYaml());
    LogsCVConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);

    assertEquals("name should match", "TestAppDConfig", bean.getName());
    assertEquals("appId should match", appId, bean.getAppId());
    assertEquals("envId should match", envId, bean.getEnvId());
    assertEquals("serviceId should match", serviceId, bean.getServiceId());
    assertEquals("query", bean.getQuery());
    assertEquals(16, bean.getBaselineStartMinute());
    assertEquals(30, bean.getBaselineEndMinute());
    assertThat(bean.getUuid()).isNotNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testUpsertAlreadyExisting() {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    LogsCVConfiguration cvConfig = new LogsCVConfiguration();
    cvConfig.setUuid("testUUID");
    when(cvConfigurationService.getConfiguration("TestAppDConfig", appId, envId)).thenReturn(cvConfig);
    ChangeContext<LogsCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId("accountId").withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    changeContext.setYaml(buildYaml());
    LogsCVConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);

    assertEquals("name should match", "TestAppDConfig", bean.getName());
    assertEquals("UUID should match", cvConfig.getUuid(), bean.getUuid());
  }
}
