package software.wings.verification;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.VerificationOperationException;
import io.harness.rule.OwnerRule.Owner;
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
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.dynatrace.DynaTraceCVServiceConfiguration;
import software.wings.verification.dynatrace.DynaTraceCVServiceConfiguration.DynaTraceCVConfigurationYaml;

public class DynatraceCVConfigurationYamlHandlerTest extends WingsBaseTest {
  @Mock YamlHelper yamlHelper;
  @Mock CVConfigurationService cvConfigService;
  @Mock EnvironmentService envService;
  @Mock ServiceResourceService serviceService;
  @Mock AppService appService;
  @Mock SettingsService settingsService;

  @Inject DynatraceCVConfigurationYamlHandler yamlHandler;

  private String envId;
  private String serviceId;
  private String appId;
  private String connectorId;
  private String accountId;

  private String envName = "EnvName";
  private String appName = "AppName";
  private String serviceName = "serviceName";
  private String connectorName = "dynaTraceConnector";

  @Before
  public void setup() throws Exception {
    accountId = generateUuid();
    envId = generateUuid();
    serviceId = generateUuid();
    appId = generateUuid();
    connectorId = generateUuid();

    MockitoAnnotations.initMocks(this);
    FieldUtils.writeField(yamlHandler, "yamlHelper", yamlHelper, true);
    FieldUtils.writeField(yamlHandler, "cvConfigurationService", cvConfigService, true);
    FieldUtils.writeField(yamlHandler, "appService", appService, true);
    FieldUtils.writeField(yamlHandler, "environmentService", envService, true);
    FieldUtils.writeField(yamlHandler, "serviceResourceService", serviceService, true);
    FieldUtils.writeField(yamlHandler, "settingsService", settingsService, true);

    Environment env = Environment.Builder.anEnvironment().uuid(envId).name(envName).build();
    when(envService.getEnvironmentByName(appId, envName)).thenReturn(env);
    when(envService.get(appId, envId)).thenReturn(env);

    Service service = Service.builder().uuid(serviceId).name(serviceName).build();
    when(serviceService.getWithDetails(appId, serviceId)).thenReturn(service);
    when(serviceService.getServiceByName(appId, serviceName)).thenReturn(service);

    Application app = Application.Builder.anApplication().name(appName).uuid(appId).build();
    when(appService.get(appId)).thenReturn(app);
    when(appService.getAppByName(accountId, appName)).thenReturn(app);

    SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withName(connectorName).withUuid(connectorId).build();
    when(settingsService.getSettingAttributeByName(accountId, connectorName)).thenReturn(settingAttribute);
    when(settingsService.get(connectorId)).thenReturn(settingAttribute);
  }

  private void setBasicInfo(DynaTraceCVServiceConfiguration cvServiceConfiguration) {
    cvServiceConfiguration.setStateType(StateType.DYNA_TRACE);
    cvServiceConfiguration.setAccountId(accountId);
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setAppId(appId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setName("TestDynaTraceConfig");
  }

  private DynaTraceCVConfigurationYaml buildYaml() {
    DynaTraceCVConfigurationYaml yaml = DynaTraceCVConfigurationYaml.builder().serviceMethods("service1").build();
    yaml.setName("TestDynaTraceConfig");
    yaml.setAccountId(accountId);
    yaml.setServiceName(serviceName);
    yaml.setEnvName(envName);
    yaml.setConnectorName(connectorName);
    yaml.setHarnessApplicationName(appName);
    return yaml;
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testToYaml() {
    final String appId = "appId";
    DynaTraceCVServiceConfiguration cvServiceConfiguration =
        DynaTraceCVServiceConfiguration.builder().serviceMethods("service1").build();
    setBasicInfo(cvServiceConfiguration);

    DynaTraceCVConfigurationYaml yaml = yamlHandler.toYaml(cvServiceConfiguration, appId);

    assertThat(yaml.getName()).isEqualTo(cvServiceConfiguration.getName());
    assertThat(yaml.getAccountId()).isEqualTo(cvServiceConfiguration.getAccountId());
    assertThat(yaml.getServiceName()).isEqualTo(serviceName);
    assertThat(yaml.getEnvName()).isEqualTo(envName);
    assertThat(yaml.getServiceMethods().split(",")).isEqualTo(cvServiceConfiguration.getServiceMethods().split("\n"));
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testToYamlMultipleServiceMethods() {
    DynaTraceCVServiceConfiguration cvServiceConfiguration =
        DynaTraceCVServiceConfiguration.builder().serviceMethods("service1\nservice2\nservice3").build();
    setBasicInfo(cvServiceConfiguration);

    DynaTraceCVConfigurationYaml yaml = yamlHandler.toYaml(cvServiceConfiguration, appId);

    assertThat(yaml.getName()).isEqualTo(cvServiceConfiguration.getName());
    assertThat(yaml.getAccountId()).isEqualTo(cvServiceConfiguration.getAccountId());
    assertThat(yaml.getServiceName()).isEqualTo(serviceName);
    assertThat(yaml.getEnvName()).isEqualTo(envName);
    assertThat(yaml.getServiceMethods().split(",")).isEqualTo(cvServiceConfiguration.getServiceMethods().split("\n"));
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testUpsert() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestDynaTraceConfig.yaml")).thenReturn("TestDynaTraceConfig");

    ChangeContext<DynaTraceCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId(accountId).withFilePath("TestDynaTraceConfig.yaml").build();
    changeContext.setChange(c);
    changeContext.setYaml(buildYaml());
    DynaTraceCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);

    assertThat(bean.getName()).isEqualTo("TestDynaTraceConfig");
    assertThat(bean.getAppId()).isEqualTo(appId);
    assertThat(bean.getEnvId()).isEqualTo(envId);
    assertThat(bean.getServiceId()).isEqualTo(serviceId);
    assertThat(bean.getUuid()).isNotNull();
    assertThat(bean.getServiceMethods()).isEqualTo("service1");
  }

  @Test(expected = VerificationOperationException.class)
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testUpsertEmptyServiceMethod() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestDynaTraceConfig.yaml")).thenReturn("TestDynaTraceConfig");

    ChangeContext<DynaTraceCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId(accountId).withFilePath("TestDynaTraceConfig.yaml").build();
    changeContext.setChange(c);
    DynaTraceCVConfigurationYaml yaml = buildYaml();
    yaml.setServiceMethods(null);
    changeContext.setYaml(yaml);
    DynaTraceCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testUpsertMultipleServiceMethods() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestDynaTraceConfig.yaml")).thenReturn("TestDynaTraceConfig");

    ChangeContext<DynaTraceCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId(accountId).withFilePath("TestDynaTraceConfig.yaml").build();
    changeContext.setChange(c);
    DynaTraceCVConfigurationYaml yaml = buildYaml();
    yaml.setServiceMethods("service1,service2,service3");
    changeContext.setYaml(yaml);
    DynaTraceCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);

    assertThat(bean.getName()).isEqualTo("TestDynaTraceConfig");
    assertThat(bean.getAppId()).isEqualTo(appId);
    assertThat(bean.getEnvId()).isEqualTo(envId);
    assertThat(bean.getServiceId()).isEqualTo(serviceId);
    assertThat(bean.getUuid()).isNotNull();
    assertThat(bean.getServiceMethods()).isEqualTo("service1\nservice2\nservice3");
  }
}
