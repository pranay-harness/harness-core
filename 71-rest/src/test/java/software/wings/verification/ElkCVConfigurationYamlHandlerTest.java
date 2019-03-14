package software.wings.verification;

import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.log.ElkCVConfiguration;
import software.wings.verification.log.ElkCVConfiguration.ElkCVConfigurationYaml;
import software.wings.verification.log.LogsCVConfiguration;
import software.wings.verification.log.LogsCVConfiguration.LogsCVConfigurationYaml;

public class ElkCVConfigurationYamlHandlerTest {
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

  ElkCVConfigurationYamlHandler yamlHandler = new ElkCVConfigurationYamlHandler();

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

  private void setBasicInfo(LogsCVConfiguration cvServiceConfiguration) {
    cvServiceConfiguration.setStateType(StateType.ELK);
    cvServiceConfiguration.setAccountId(accountId);
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setAppId(appId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setName("TestAppDConfig");
  }

  private ElkCVConfigurationYaml buildYaml() {
    ElkCVConfigurationYaml yaml = new ElkCVConfigurationYaml();
    yaml.setType(StateType.ELK.name());
    yaml.setName("TestAppDConfig");
    yaml.setAccountId(accountId);
    yaml.setServiceName(serviceName);
    yaml.setEnvName(envName);
    yaml.setConnectorName(connectorName);
    yaml.setHarnessApplicationName(appName);
    yaml.setQuery("query1");
    yaml.setBaselineStartMinute(16);
    yaml.setBaselineEndMinute(30);
    yaml.setQueryType(ElkQueryType.MATCH.name());
    yaml.setIndex("index1");
    yaml.setHostnameField("hostName1");
    yaml.setMessageField("message1");
    yaml.setTimestampField("timestamp1");
    yaml.setTimestampFormat("format1");
    return yaml;
  }

  @Test
  public void testToYaml() {
    final String appId = "appId";
    ElkCVConfiguration cvServiceConfiguration = new ElkCVConfiguration();
    setBasicInfo(cvServiceConfiguration);
    cvServiceConfiguration.setQuery(generateUUID());
    cvServiceConfiguration.setBaselineStartMinute(16);
    cvServiceConfiguration.setBaselineEndMinute(30);
    cvServiceConfiguration.setQueryType(ElkQueryType.MATCH);
    cvServiceConfiguration.setIndex(generateUUID());
    cvServiceConfiguration.setHostnameField(generateUUID());
    cvServiceConfiguration.setMessageField(generateUUID());
    cvServiceConfiguration.setTimestampField(generateUUID());
    cvServiceConfiguration.setTimestampFormat(generateUUID());

    ElkCVConfigurationYaml yaml = (ElkCVConfigurationYaml) yamlHandler.toYaml(cvServiceConfiguration, appId);

    assertEquals("Name should be same", cvServiceConfiguration.getName(), yaml.getName());
    assertEquals("AccountId should be same", cvServiceConfiguration.getAccountId(), yaml.getAccountId());
    assertEquals("ServiceName should be same", serviceName, yaml.getServiceName());
    assertEquals("envId should be same", envName, yaml.getEnvName());
    assertEquals("query should be same", cvServiceConfiguration.getQuery(), yaml.getQuery());
    assertEquals(cvServiceConfiguration.getBaselineStartMinute(), yaml.getBaselineStartMinute());
    assertEquals(cvServiceConfiguration.getBaselineEndMinute(), yaml.getBaselineEndMinute());
    assertEquals(cvServiceConfiguration.getQueryType(), ElkQueryType.valueOf(yaml.getQueryType()));
    assertEquals(cvServiceConfiguration.getIndex(), yaml.getIndex());
    assertEquals(cvServiceConfiguration.getHostnameField(), yaml.getHostnameField());
    assertEquals(cvServiceConfiguration.getMessageField(), yaml.getMessageField());
    assertEquals(cvServiceConfiguration.getTimestampField(), yaml.getTimestampField());
    assertEquals(cvServiceConfiguration.getTimestampFormat(), yaml.getTimestampFormat());
  }

  @Test
  public void testUpsert() {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    ChangeContext<LogsCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId("accountId").withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    changeContext.setYaml(buildYaml());
    ElkCVConfiguration bean = (ElkCVConfiguration) yamlHandler.upsertFromYaml(changeContext, null);

    assertEquals("name should match", "TestAppDConfig", bean.getName());
    assertEquals("appId should match", appId, bean.getAppId());
    assertEquals("envId should match", envId, bean.getEnvId());
    assertEquals("serviceId should match", serviceId, bean.getServiceId());
    assertEquals("query1", bean.getQuery());
    assertEquals(16, bean.getBaselineStartMinute());
    assertEquals(30, bean.getBaselineEndMinute());
    assertEquals(ElkQueryType.MATCH, bean.getQueryType());
    assertEquals("index1", bean.getIndex());
    assertEquals("hostName1", bean.getHostnameField());
    assertEquals("message1", bean.getMessageField());
    assertEquals("timestamp1", bean.getTimestampField());
    assertEquals("format1", bean.getTimestampFormat());
  }

  @Test
  public void testUpsertAlreadyExisting() {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    ElkCVConfiguration cvConfig = new ElkCVConfiguration();
    cvConfig.setUuid("testUUID");
    when(cvConfigurationService.getConfiguration("TestAppDConfig", appId, envId)).thenReturn(cvConfig);
    ChangeContext<LogsCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId("accountId").withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    changeContext.setYaml(buildYaml());
    ElkCVConfiguration bean = (ElkCVConfiguration) yamlHandler.upsertFromYaml(changeContext, null);

    assertEquals("name should match", "TestAppDConfig", bean.getName());
    assertEquals("UUID should match", cvConfig.getUuid(), bean.getUuid());
  }
}
