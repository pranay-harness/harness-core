package software.wings.service.impl.yaml.handler.configfile;

import static io.harness.rule.OwnerRule.INDER;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.UUID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.yaml.handler.YamlHandlerTestBase;

import java.io.IOException;

public class ConfigFileOverrideYamlHandlerTest extends YamlHandlerTestBase {
  @InjectMocks @Inject private ConfigFileOverrideYamlHandler configFileOverrideYamlHandler;
  @InjectMocks @Inject private YamlHelper yamlHelper;

  @Mock private ConfigService configService;
  @Mock private AppService appService;
  @Mock private EnvironmentService environmentService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ServiceTemplateService serviceTemplateService;

  private final String yamlFilePath =
      "Setup/Applications/APP_NAME/Environments/ENV_NAME/Config Files/SERVICE_NAME/file.yaml";

  private void getApplication() {
    Application application =
        Application.Builder.anApplication().uuid(APP_ID).accountId(ACCOUNT_ID).appId(APP_ID).build();
    when(appService.getAppByName(anyString(), anyString())).thenReturn(application);
  }

  private void getEnvironment() {
    Environment environment = Environment.Builder.anEnvironment().uuid(UUID).accountId(ACCOUNT_ID).build();
    when(environmentService.getEnvironmentByName(anyString(), anyString())).thenReturn(environment);
  }

  private void getServiceAndServiceTemplateId() {
    Service service = Service.builder().uuid(UUID).build();
    when(serviceResourceService.getServiceByName(anyString(), anyString())).thenReturn(service);
    ServiceTemplate serviceTemplate = ServiceTemplate.Builder.aServiceTemplate().withUuid(SERVICE_TEMPLATE_ID).build();
    when(serviceTemplateService.get(anyString(), anyString(), anyString())).thenReturn(serviceTemplate);
  }

  private ChangeContext<ConfigFile.OverrideYaml> getChangeContext(String yamlFilePath) throws IOException {
    GitFileChange gitFileChange = GitFileChange.Builder.aGitFileChange()
                                      .withAccountId(ACCOUNT_ID)
                                      .withFilePath(yamlFilePath)
                                      .withFileContent("harnessApiVersion: '1.0'")
                                      .build();
    ChangeContext<ConfigFile.OverrideYaml> changeContext = new ChangeContext<>();
    changeContext.setChange(gitFileChange);
    changeContext.setYamlType(YamlType.CONFIG_FILE_OVERRIDE_DEPRECATED);
    ConfigFile.OverrideYaml yaml =
        (ConfigFile.OverrideYaml) getYaml("harnessApiVersion: '1.0'", ConfigFile.OverrideYaml.class);
    changeContext.setYaml(yaml);
    return changeContext;
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testDeleteIfApplicationNotExist() throws IOException {
    ChangeContext<ConfigFile.OverrideYaml> changeContext = getChangeContext(yamlFilePath);

    configFileOverrideYamlHandler.delete(changeContext);
    verify(appService, times(1)).getAppByName(ACCOUNT_ID, APP_NAME);
    verify(environmentService, never()).getEnvironmentByName(anyString(), anyString());
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testDeleteIfEnvironmentNotExist() throws IOException {
    ChangeContext<ConfigFile.OverrideYaml> changeContext = getChangeContext(yamlFilePath);
    getApplication();

    configFileOverrideYamlHandler.delete(changeContext);
    verify(environmentService, times(1)).getEnvironmentByName(APP_ID, ENV_NAME);
    verify(configService, never()).delete(anyString(), anyString(), any(), anyString());
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testDeleteWithFileOverrideForAService() throws IOException {
    ChangeContext<ConfigFile.OverrideYaml> changeContext = getChangeContext(yamlFilePath);
    getApplication();
    getEnvironment();
    getServiceAndServiceTemplateId();

    configFileOverrideYamlHandler.delete(changeContext);
    verify(configService, times(1))
        .delete(eq(APP_ID), eq(SERVICE_TEMPLATE_ID), eq(EntityType.SERVICE_TEMPLATE), anyString());
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testDeleteWithFileOverrideForAllServices() throws IOException {
    String yamlFilePathForAllServices =
        "Setup/Applications/APP_NAME/Environments/ENV_NAME/Config Files/__all_service__/file.yaml";
    ChangeContext<ConfigFile.OverrideYaml> changeContext = getChangeContext(yamlFilePathForAllServices);
    getApplication();
    getEnvironment();

    configFileOverrideYamlHandler.delete(changeContext);
    verify(configService, times(1)).delete(anyString(), anyString(), eq(EntityType.ENVIRONMENT), anyString());
  }
}
