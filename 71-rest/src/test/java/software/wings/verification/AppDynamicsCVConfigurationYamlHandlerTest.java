package software.wings.verification;

import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import io.harness.exception.WingsException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration.AppDynamicsCVConfigurationYaml;

import java.util.Arrays;
import java.util.HashSet;

public class AppDynamicsCVConfigurationYamlHandlerTest extends WingsBaseTest {
  @Mock YamlHelper yamlHelper;
  @Mock CVConfigurationService cvConfigurationService;
  @Mock EnvironmentService environmentService;
  @Mock ServiceResourceService serviceResourceService;
  @Mock AppService appService;
  @Mock SettingsService settingsService;
  @Mock AppdynamicsService appdynamicsService;

  AppDynamicsCVConfigurationYamlHandler yamlHandler = new AppDynamicsCVConfigurationYamlHandler();

  private String envId;
  private String serviceId;
  private String appId;
  private String connectorId;
  private String accountId;
  private String tierId;

  private String envName = "EnvName";
  private String appName = "AppName";
  private String serviceName = "serviceName";
  private String connectorName = "appDConnector";
  private String tierName = "tierName";

  @Before
  public void setup() throws Exception {
    accountId = generateUUID();
    envId = generateUUID();
    serviceId = generateUUID();
    appId = generateUUID();
    connectorId = generateUUID();
    tierId = "123456";

    MockitoAnnotations.initMocks(this);
    setInternalState(yamlHandler, "yamlHelper", yamlHelper);
    setInternalState(yamlHandler, "cvConfigurationService", cvConfigurationService);
    setInternalState(yamlHandler, "appService", appService);
    setInternalState(yamlHandler, "environmentService", environmentService);
    setInternalState(yamlHandler, "serviceResourceService", serviceResourceService);
    setInternalState(yamlHandler, "settingsService", settingsService);
    setInternalState(yamlHandler, "appdynamicsService", appdynamicsService);

    Environment env = Environment.Builder.anEnvironment().withUuid(envId).withName(envName).build();
    when(environmentService.getEnvironmentByName(appId, envName)).thenReturn(env);
    when(environmentService.get(appId, envId)).thenReturn(env);

    Service service = Service.builder().uuid(serviceId).name(serviceName).build();
    when(serviceResourceService.get(appId, serviceId)).thenReturn(service);
    when(serviceResourceService.getServiceByName(appId, serviceName)).thenReturn(service);

    Application app = Application.Builder.anApplication().withName(appName).withUuid(appId).build();
    when(appService.get(appId)).thenReturn(app);
    when(appService.getAppByName(accountId, appName)).thenReturn(app);

    when(appdynamicsService.getApplications(connectorId))
        .thenReturn(Arrays.asList(NewRelicApplication.builder().id(1234).name(appName).build()));
    when(appdynamicsService.getTiers(connectorId, 1234))
        .thenReturn(
            new HashSet<>(Arrays.asList(AppdynamicsTier.builder().id(Long.valueOf(tierId)).name(tierName).build())));

    SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withName(connectorName).withUuid(connectorId).build();
    when(settingsService.getSettingAttributeByName(accountId, connectorName)).thenReturn(settingAttribute);
    when(settingsService.get(connectorId)).thenReturn(settingAttribute);
  }

  private void setBasicInfo(AppDynamicsCVServiceConfiguration cvServiceConfiguration) {
    cvServiceConfiguration.setStateType(StateType.APP_DYNAMICS);
    cvServiceConfiguration.setAccountId(accountId);
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setAppId(appId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setName("TestAppDConfig");
  }

  private AppDynamicsCVConfigurationYaml buildYaml() {
    AppDynamicsCVConfigurationYaml yaml =
        AppDynamicsCVConfigurationYaml.builder().appDynamicsApplicationName(appName).tierName(tierName).build();
    yaml.setName("TestAppDConfig");
    yaml.setAccountId(accountId);
    yaml.setServiceName(serviceName);
    yaml.setEnvName(envName);
    yaml.setConnectorName(connectorName);
    yaml.setHarnessApplicationName(appName);
    return yaml;
  }

  @Test
  public void testToYaml() {
    final String appId = "appId";
    AppDynamicsCVServiceConfiguration cvServiceConfiguration =
        AppDynamicsCVServiceConfiguration.builder().tierId(tierId).appDynamicsApplicationId("1234").build();
    setBasicInfo(cvServiceConfiguration);

    AppDynamicsCVConfigurationYaml yaml = yamlHandler.toYaml(cvServiceConfiguration, appId);

    assertEquals("Name should be same", cvServiceConfiguration.getName(), yaml.getName());
    assertEquals("AccountId should be same", cvServiceConfiguration.getAccountId(), yaml.getAccountId());
    assertEquals("serviceId should be same", serviceName, yaml.getServiceName());
    assertEquals("EnvId should be same", envName, yaml.getEnvName());
    assertEquals("TierId should be same", tierName, yaml.getTierName());
    assertEquals("AppD applicationID should be same", appName, yaml.getAppDynamicsApplicationName());
  }

  @Test
  public void testUpsert() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    ChangeContext<AppDynamicsCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId(accountId).withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    changeContext.setYaml(buildYaml());
    AppDynamicsCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);

    assertEquals("name should match", "TestAppDConfig", bean.getName());
    assertEquals("appId should match", appId, bean.getAppId());
    assertEquals("envId should match", envId, bean.getEnvId());
    assertEquals("serviceId should match", serviceId, bean.getServiceId());
  }

  @Test(expected = WingsException.class)
  public void testUpsertBadApplicationID() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    ChangeContext<AppDynamicsCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId(accountId).withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    AppDynamicsCVConfigurationYaml yaml = buildYaml();
    yaml.setAppDynamicsApplicationName("dummyWrongName");
    changeContext.setYaml(yaml);
    AppDynamicsCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);
  }

  @Test(expected = WingsException.class)
  public void testUpsertBadTierName() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    ChangeContext<AppDynamicsCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId(accountId).withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    AppDynamicsCVConfigurationYaml yaml = buildYaml();
    yaml.setTierName("dummyWrongName");
    changeContext.setYaml(yaml);
    AppDynamicsCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);
  }

  @Test
  public void testUpsertAlreadyExisting() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    AppDynamicsCVServiceConfiguration cvConfig = AppDynamicsCVServiceConfiguration.builder().build();
    cvConfig.setUuid("testUUID");
    when(cvConfigurationService.getConfiguration("TestAppDConfig", appId, envId)).thenReturn(cvConfig);
    ChangeContext<AppDynamicsCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId(accountId).withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    changeContext.setYaml(buildYaml());
    AppDynamicsCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);

    assertEquals("name should match", "TestAppDConfig", bean.getName());
    assertEquals("UUID should match", cvConfig.getUuid(), bean.getUuid());
  }
}
