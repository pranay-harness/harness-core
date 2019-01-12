package software.wings.verification;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration.AppDynamicsCVConfigurationYaml;

public class AppDynamicsCVConfigurationYamlHandlerTest extends WingsBaseTest {
  @Mock YamlHelper yamlHelper;
  @Mock CVConfigurationService cvConfigurationService;
  AppDynamicsCVConfigurationYamlHandler yamlHandler = new AppDynamicsCVConfigurationYamlHandler();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    setInternalState(yamlHandler, "yamlHelper", yamlHelper);
    setInternalState(yamlHandler, "cvConfigurationService", cvConfigurationService);
  }

  private void setBasicInfo(AppDynamicsCVServiceConfiguration cvServiceConfiguration) {
    cvServiceConfiguration.setStateType(StateType.APP_DYNAMICS);
    cvServiceConfiguration.setAccountId("accountId");
    cvServiceConfiguration.setServiceId("serviceId");
    cvServiceConfiguration.setEnvId("envId");
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setName("TestAppDConfig");
  }

  private AppDynamicsCVConfigurationYaml buildYaml() {
    AppDynamicsCVConfigurationYaml yaml =
        AppDynamicsCVConfigurationYaml.builder().appDynamicsApplicationId("1234").tierId("testTierId").build();
    yaml.setName("TestAppDConfig");
    yaml.setAccountId("accountId");
    yaml.setServiceId("serviceId");
    yaml.setEnvId("envId");
    return yaml;
  }

  @Test
  public void testToYaml() {
    final String appId = "appId";
    AppDynamicsCVServiceConfiguration cvServiceConfiguration =
        AppDynamicsCVServiceConfiguration.builder().tierId("testTier").appDynamicsApplicationId("1234").build();
    setBasicInfo(cvServiceConfiguration);

    AppDynamicsCVConfigurationYaml yaml = yamlHandler.toYaml(cvServiceConfiguration, appId);

    assertEquals("Name should be same", cvServiceConfiguration.getName(), yaml.getName());
    assertEquals("AccountId should be same", cvServiceConfiguration.getAccountId(), yaml.getAccountId());
    assertEquals("serviceId should be same", cvServiceConfiguration.getServiceId(), yaml.getServiceId());
    assertEquals("EnvId should be same", cvServiceConfiguration.getEnvId(), yaml.getEnvId());
    assertEquals("TierId should be same", cvServiceConfiguration.getTierId(), yaml.getTierId());
    assertEquals("AppD applicationID should be same", cvServiceConfiguration.getAppDynamicsApplicationId(),
        yaml.getAppDynamicsApplicationId());
  }

  @Test
  public void testUpsert() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn("appId");
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn("envId");
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    ChangeContext<AppDynamicsCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId("accountId").withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    changeContext.setYaml(buildYaml());
    AppDynamicsCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);

    assertEquals("name should match", "TestAppDConfig", bean.getName());
    assertEquals("appId should match", "appId", bean.getAppId());
    assertEquals("envId should match", "envId", bean.getEnvId());
    assertEquals("serviceId should match", "serviceId", bean.getServiceId());
  }

  @Test
  public void testUpsertAlreadyExisting() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn("appId");
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn("envId");
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    AppDynamicsCVServiceConfiguration cvConfig = AppDynamicsCVServiceConfiguration.builder().build();
    cvConfig.setUuid("testUUID");
    when(cvConfigurationService.getConfiguration("TestAppDConfig", "appId", "envId")).thenReturn(cvConfig);
    ChangeContext<AppDynamicsCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId("accountId").withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    changeContext.setYaml(buildYaml());
    AppDynamicsCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);

    assertEquals("name should match", "TestAppDConfig", bean.getName());
    assertEquals("UUID should match", cvConfig.getUuid(), bean.getUuid());
  }
}
