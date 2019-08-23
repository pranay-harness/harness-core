package software.wings.verification;

import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
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
import software.wings.verification.datadog.DatadogCVServiceConfiguration;
import software.wings.verification.datadog.DatadogCVServiceConfiguration.DatadogCVConfigurationYaml;

import java.util.HashMap;
import java.util.Map;

public class DatadogCVConfigurationYamlHandlerTest extends CategoryTest {
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
    Map<String, String> dockerMetrics = new HashMap<>();
    dockerMetrics.put("cluster:harness-test", "kubernetes.cpu.usage.total");

    DatadogCVConfigurationYaml yaml = DatadogCVConfigurationYaml.builder().dockerMetrics(dockerMetrics).build();
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
    Map<String, String> dockerMetrics = new HashMap<>();
    dockerMetrics.put("cluster:harness-test", "kubernetes.cpu.usage.total");

    DatadogCVServiceConfiguration cvServiceConfiguration =
        DatadogCVServiceConfiguration.builder().dockerMetrics(dockerMetrics).build();
    setBasicInfo(cvServiceConfiguration);

    DatadogCVConfigurationYaml yaml = yamlHandler.toYaml(cvServiceConfiguration, appId);

    assertThat(yaml.getName()).isEqualTo(cvServiceConfiguration.getName());
    assertThat(yaml.getAccountId()).isEqualTo(cvServiceConfiguration.getAccountId());
    assertThat(yaml.getServiceName()).isEqualTo(serviceName);
    assertThat(yaml.getEnvName()).isEqualTo(envName);
    assertEquals("ApplicationFilter should be same",
        cvServiceConfiguration.getDockerMetrics().keySet().iterator().next(),
        yaml.getDockerMetrics().keySet().iterator().next());
    assertEquals("Metrics value shouldbe same", cvServiceConfiguration.getDockerMetrics().values(),
        yaml.getDockerMetrics().values());
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

    assertThat(bean.getName()).isEqualTo("TestDDConfig");
    assertThat(bean.getAppId()).isEqualTo(appId);
    assertThat(bean.getEnvId()).isEqualTo(envId);
    assertThat(bean.getServiceId()).isEqualTo(serviceId);
    assertThat(bean.getUuid()).isNotNull();
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
    yaml.setDockerMetrics(null);
    changeContext.setYaml(yaml);
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
    Map<String, String> dockerMetrics = new HashMap<>();
    dockerMetrics.put("cluster:harness-test", "kubernetes.cpu.usage123");
    yaml.setDockerMetrics(dockerMetrics);
    changeContext.setYaml(yaml);
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
    Map<String, String> dockerMetrics = new HashMap<>();
    dockerMetrics.put("cluster:harness-test", "kubernetes.cpu.usage.total");
    yaml.setDockerMetrics(dockerMetrics);
    yaml.setDatadogServiceName("todolist");
    DatadogCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);

    assertThat(bean.getDatadogServiceName()).isEqualTo("todolist");
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

    assertThat(bean.getName()).isEqualTo("TestDDConfig");
    assertThat(bean.getUuid()).isEqualTo(cvConfig.getUuid());
  }
}
