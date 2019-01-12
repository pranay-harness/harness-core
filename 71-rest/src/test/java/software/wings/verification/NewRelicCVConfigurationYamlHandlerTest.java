package software.wings.verification;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration.NewRelicCVConfigurationYaml;

public class NewRelicCVConfigurationYamlHandlerTest {
  @Mock YamlHelper yamlHelper;
  @Mock CVConfigurationService cvConfigurationService;
  NewRelicCVConfigurationYamlHandler yamlHandler = new NewRelicCVConfigurationYamlHandler();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    setInternalState(yamlHandler, "yamlHelper", yamlHelper);
    setInternalState(yamlHandler, "cvConfigurationService", cvConfigurationService);
  }

  private void setBasicInfo(NewRelicCVServiceConfiguration cvServiceConfiguration) {
    cvServiceConfiguration.setStateType(StateType.APP_DYNAMICS);
    cvServiceConfiguration.setAccountId("accountId");
    cvServiceConfiguration.setServiceId("serviceId");
    cvServiceConfiguration.setEnvId("envId");
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setName("TestAppDConfig");
  }

  private NewRelicCVConfigurationYaml buildYaml() {
    NewRelicCVConfigurationYaml yaml = NewRelicCVConfigurationYaml.builder().applicationId("1234").build();
    yaml.setName("TestAppDConfig");
    yaml.setAccountId("accountId");
    yaml.setServiceId("serviceId");
    yaml.setEnvId("envId");
    return yaml;
  }

  @Test
  public void testToYaml() {
    final String appId = "appId";
    NewRelicCVServiceConfiguration cvServiceConfiguration =
        NewRelicCVServiceConfiguration.builder().applicationId("1234").build();
    setBasicInfo(cvServiceConfiguration);

    NewRelicCVConfigurationYaml yaml = yamlHandler.toYaml(cvServiceConfiguration, appId);

    assertEquals("Name should be same", cvServiceConfiguration.getName(), yaml.getName());
    assertEquals("AccountId should be same", cvServiceConfiguration.getAccountId(), yaml.getAccountId());
    assertEquals("serviceId should be same", cvServiceConfiguration.getServiceId(), yaml.getServiceId());
    assertEquals("EnvId should be same", cvServiceConfiguration.getEnvId(), yaml.getEnvId());
    assertEquals(
        "NewRelic applicationID should be same", cvServiceConfiguration.getApplicationId(), yaml.getApplicationId());
  }

  @Test
  public void testUpsert() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn("appId");
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn("envId");
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    ChangeContext<NewRelicCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId("accountId").withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    changeContext.setYaml(buildYaml());
    NewRelicCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);

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

    NewRelicCVServiceConfiguration cvConfig = NewRelicCVServiceConfiguration.builder().build();
    cvConfig.setUuid("testUUID");
    when(cvConfigurationService.getConfiguration("TestAppDConfig", "appId", "envId")).thenReturn(cvConfig);
    ChangeContext<NewRelicCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId("accountId").withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    changeContext.setYaml(buildYaml());
    NewRelicCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);

    assertEquals("name should match", "TestAppDConfig", bean.getName());
    assertEquals("UUID should match", cvConfig.getUuid(), bean.getUuid());
  }
}
