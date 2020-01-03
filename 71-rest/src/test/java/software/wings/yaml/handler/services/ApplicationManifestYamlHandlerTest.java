package software.wings.yaml.handler.services;

import static io.harness.rule.OwnerRule.ANSHUL;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.HarnessException;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.GitFileConfig;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Builder;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.GitFileConfigHelperService;
import software.wings.service.impl.yaml.handler.service.ApplicationManifestYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.utils.WingsTestConstants;
import software.wings.yaml.handler.BaseYamlHandlerTest;

import java.io.IOException;

public class ApplicationManifestYamlHandlerTest extends BaseYamlHandlerTest {
  @Mock private AppService appService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private SettingsService settingsService;
  @Mock private EnvironmentService environmentService;

  @InjectMocks @Inject private GitFileConfigHelperService gitFileConfigHelperService;
  @InjectMocks @Inject private YamlHelper yamlHelper;
  @InjectMocks @Inject private ApplicationManifestService applicationManifestService;
  @InjectMocks @Inject private ApplicationManifestYamlHandler yamlHandler;

  private ApplicationManifest localApplicationManifest;
  private ApplicationManifest remoteApplicationManifest;

  private static final String CONNECTOR_ID = "CONNECTOR_ID";
  private static final String CONNECTOR_NAME = "CONNECTOR_NAME";
  private String localValidYamlContent = "harnessApiVersion: '1.0'\n"
      + "type: APPLICATION_MANIFEST\n"
      + "storeType: Local";

  private String remoteYamlContent = "harnessApiVersion: '1.0'\n"
      + "type: APPLICATION_MANIFEST\n"
      + "gitFileConfig:\n"
      + "  branch: BRANCH\n"
      + "  connectorName: CONNECTOR_NAME\n"
      + "  filePath: ABC/\n"
      + "  useBranch: true\n"
      + "storeType: Remote";

  private String validYamlFilePath = "Setup/Applications/APP_NAME/Services/SERVICE_NAME/Manifests/Index.yaml";
  private String invalidYamlFilePath = "Setup/Applications/APP_NAME/ServicesInvalid/SERVICE_NAME/Manifests/Index.yaml";

  private String envOverrideLocalValidYamlContent = "harnessApiVersion: '1.0'\n"
      + "type: APPLICATION_MANIFEST_VALUES_ENV_OVERRIDE\n"
      + "storeType: Local";
  private String envOverrideRemoteValidYamlContent = "harnessApiVersion: '1.0'\n"
      + "type: APPLICATION_MANIFEST_VALUES_ENV_OVERRIDE\n"
      + "gitFileConfig:\n"
      + "  branch: BRANCH\n"
      + "  connectorName: CONNECTOR_NAME\n"
      + "  filePath: ABC/\n"
      + "  useBranch: true\n"
      + "storeType: Remote";
  private String envOverrideValidYamlFilePath = "Setup/Applications/APP_NAME/Environments/ENV_NAME/Values/Index.yaml";

  private String envServiceOverrideLocalValidYamlContent = "harnessApiVersion: '1.0'\n"
      + "type: APPLICATION_MANIFEST_VALUES_ENV_SERVICE_OVERRIDE\n"
      + "storeType: Local";
  private String envServiceOverrideRemoteValidYamlContent = "harnessApiVersion: '1.0'\n"
      + "type: APPLICATION_MANIFEST_VALUES_ENV_SERVICE_OVERRIDE\n"
      + "gitFileConfig:\n"
      + "  branch: BRANCH\n"
      + "  connectorName: CONNECTOR_NAME\n"
      + "  filePath: ABC/\n"
      + "  useBranch: true\n"
      + "storeType: Remote";
  private String envServiceOverrideValidYamlFilePath =
      "Setup/Applications/APP_NAME/Environments/ENV_NAME/Values/Services/SERVICE_NAME/Index.yaml";

  @Before
  public void setUp() {
    localApplicationManifest = ApplicationManifest.builder()
                                   .serviceId(WingsTestConstants.SERVICE_ID)
                                   .storeType(StoreType.Local)
                                   .kind(AppManifestKind.VALUES)
                                   .build();
    remoteApplicationManifest = ApplicationManifest.builder()
                                    .serviceId(WingsTestConstants.SERVICE_ID)
                                    .storeType(StoreType.Remote)
                                    .gitFileConfig(GitFileConfig.builder()
                                                       .filePath("ABC/")
                                                       .branch("BRANCH")
                                                       .useBranch(true)
                                                       .connectorId(CONNECTOR_ID)
                                                       .build())
                                    .kind(AppManifestKind.VALUES)
                                    .build();

    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    when(appService.getAppByName(ACCOUNT_ID, APP_NAME))
        .thenReturn(Application.Builder.anApplication().uuid(APP_ID).name(APP_NAME).build());
    when(serviceResourceService.getServiceByName(APP_ID, SERVICE_NAME))
        .thenReturn(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build());
    when(serviceResourceService.exist(any(), any())).thenReturn(true);
    when(environmentService.getEnvironmentByName(APP_ID, ENV_NAME))
        .thenReturn(Environment.Builder.anEnvironment().uuid(ENV_ID).name(ENV_NAME).build());

    SettingAttribute settingAttribute =
        Builder.aSettingAttribute().withName(CONNECTOR_NAME).withUuid(CONNECTOR_ID).build();
    when(settingsService.get(CONNECTOR_ID)).thenReturn(settingAttribute);
    when(settingsService.getByName(ACCOUNT_ID, APP_ID, CONNECTOR_NAME)).thenReturn(settingAttribute);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testCRUDAndGetForLocal() throws HarnessException, IOException {
    ChangeContext<ApplicationManifest.Yaml> changeContext =
        createChangeContext(localValidYamlContent, validYamlFilePath);

    ApplicationManifest.Yaml yamlObject =
        (ApplicationManifest.Yaml) getYaml(localValidYamlContent, ApplicationManifest.Yaml.class);
    changeContext.setYaml(yamlObject);

    ApplicationManifest savedApplicationManifest = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    compareAppManifest(localApplicationManifest, savedApplicationManifest);

    validateYamlContent(localValidYamlContent, localApplicationManifest);

    ApplicationManifest applicationManifestFromGet = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    compareAppManifest(localApplicationManifest, applicationManifestFromGet);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testCRUDAndGetForRemote() throws HarnessException, IOException {
    ChangeContext<ApplicationManifest.Yaml> changeContext = createChangeContext(remoteYamlContent, validYamlFilePath);

    ApplicationManifest.Yaml yamlObject =
        (ApplicationManifest.Yaml) getYaml(remoteYamlContent, ApplicationManifest.Yaml.class);
    changeContext.setYaml(yamlObject);

    ApplicationManifest savedApplicationManifest = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    compareAppManifest(remoteApplicationManifest, savedApplicationManifest);

    validateYamlContent(remoteYamlContent, remoteApplicationManifest);

    ApplicationManifest applicationManifestFromGet = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    compareAppManifest(remoteApplicationManifest, applicationManifestFromGet);
  }

  @Test(expected = WingsException.class)
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testFailures() throws HarnessException, IOException {
    ChangeContext<ApplicationManifest.Yaml> changeContext =
        createChangeContext(localValidYamlContent, invalidYamlFilePath);

    ApplicationManifest.Yaml yamlObject =
        (ApplicationManifest.Yaml) getYaml(localValidYamlContent, ApplicationManifest.Yaml.class);
    changeContext.setYaml(yamlObject);

    yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
  }

  private void validateYamlContent(String yamlFileContent, ApplicationManifest applicationManifest) {
    ApplicationManifest.Yaml yaml = yamlHandler.toYaml(applicationManifest, APP_ID);
    assertThat(yaml).isNotNull();
    assertThat(yaml.getType()).isNotNull();

    String yamlContent = getYamlContent(yaml);
    assertThat(yamlContent).isNotNull();
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertThat(yamlContent).isEqualTo(yamlFileContent);
  }

  private void compareAppManifest(ApplicationManifest lhs, ApplicationManifest rhs) {
    assertThat(rhs.getStoreType()).isEqualTo(lhs.getStoreType());
    assertThat(rhs.getGitFileConfig()).isEqualTo(lhs.getGitFileConfig());
    if (lhs.getGitFileConfig() != null) {
      assertThat(rhs.getGitFileConfig().getConnectorId()).isEqualTo(lhs.getGitFileConfig().getConnectorId());
      assertThat(rhs.getGitFileConfig().getBranch()).isEqualTo(lhs.getGitFileConfig().getBranch());
      assertThat(rhs.getGitFileConfig().getFilePath()).isEqualTo(lhs.getGitFileConfig().getFilePath());
      assertThat(rhs.getGitFileConfig().isUseBranch()).isEqualTo(lhs.getGitFileConfig().isUseBranch());
      assertThat(rhs.getGitFileConfig().getConnectorName()).isEqualTo(lhs.getGitFileConfig().getConnectorName());
    }
  }

  private ChangeContext<ApplicationManifest.Yaml> createChangeContext(String fileContent, String filePath) {
    GitFileChange gitFileChange = new GitFileChange();
    gitFileChange.setFileContent(fileContent);
    gitFileChange.setFilePath(filePath);
    gitFileChange.setAccountId(ACCOUNT_ID);

    ChangeContext<ApplicationManifest.Yaml> changeContext = new ChangeContext<>();
    changeContext.setChange(gitFileChange);
    changeContext.setYamlType(YamlType.APPLICATION_MANIFEST);
    changeContext.setYamlSyncHandler(yamlHandler);

    return changeContext;
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testCRUDAndGetForEnvOverrideWithLocalStoreType() throws HarnessException, IOException {
    ApplicationManifest applicationManifest = ApplicationManifest.builder()
                                                  .envId(WingsTestConstants.ENV_ID)
                                                  .storeType(StoreType.Local)
                                                  .kind(AppManifestKind.VALUES)
                                                  .build();

    ChangeContext<ApplicationManifest.Yaml> changeContext =
        createChangeContext(envOverrideLocalValidYamlContent, envOverrideValidYamlFilePath);

    ApplicationManifest.Yaml yamlObject =
        (ApplicationManifest.Yaml) getYaml(envOverrideLocalValidYamlContent, ApplicationManifest.Yaml.class);
    changeContext.setYaml(yamlObject);

    ApplicationManifest savedApplicationManifest = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    compareAppManifest(applicationManifest, savedApplicationManifest);

    validateYamlContent(envOverrideLocalValidYamlContent, applicationManifest);

    ApplicationManifest applicationManifestFromGet = yamlHandler.get(ACCOUNT_ID, envOverrideValidYamlFilePath);
    compareAppManifest(applicationManifest, applicationManifestFromGet);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testCRUDAndGetForEnvOverrideWithRemoteStoreType() throws HarnessException, IOException {
    ApplicationManifest applicationManifest = ApplicationManifest.builder()
                                                  .envId(WingsTestConstants.ENV_ID)
                                                  .storeType(StoreType.Remote)
                                                  .gitFileConfig(GitFileConfig.builder()
                                                                     .filePath("ABC/")
                                                                     .branch("BRANCH")
                                                                     .useBranch(true)
                                                                     .connectorId(CONNECTOR_ID)
                                                                     .build())
                                                  .kind(AppManifestKind.VALUES)
                                                  .build();

    ChangeContext<ApplicationManifest.Yaml> changeContext =
        createChangeContext(envOverrideRemoteValidYamlContent, envOverrideValidYamlFilePath);

    ApplicationManifest.Yaml yamlObject =
        (ApplicationManifest.Yaml) getYaml(envOverrideRemoteValidYamlContent, ApplicationManifest.Yaml.class);
    changeContext.setYaml(yamlObject);

    ApplicationManifest savedApplicationManifest = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    compareAppManifest(applicationManifest, savedApplicationManifest);

    validateYamlContent(envOverrideRemoteValidYamlContent, applicationManifest);

    ApplicationManifest applicationManifestFromGet = yamlHandler.get(ACCOUNT_ID, envOverrideValidYamlFilePath);
    compareAppManifest(applicationManifest, applicationManifestFromGet);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testCRUDAndGetForEnvServiceOverrideWithLocalStoreType() throws HarnessException, IOException {
    ApplicationManifest applicationManifest = ApplicationManifest.builder()
                                                  .envId(ENV_ID)
                                                  .serviceId(SERVICE_ID)
                                                  .storeType(StoreType.Local)
                                                  .kind(AppManifestKind.VALUES)
                                                  .build();

    ChangeContext<ApplicationManifest.Yaml> changeContext =
        createChangeContext(envServiceOverrideLocalValidYamlContent, envServiceOverrideValidYamlFilePath);

    ApplicationManifest.Yaml yamlObject =
        (ApplicationManifest.Yaml) getYaml(envServiceOverrideLocalValidYamlContent, ApplicationManifest.Yaml.class);
    changeContext.setYaml(yamlObject);

    ApplicationManifest savedApplicationManifest = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    compareAppManifest(applicationManifest, savedApplicationManifest);

    validateYamlContent(envServiceOverrideLocalValidYamlContent, applicationManifest);

    ApplicationManifest applicationManifestFromGet = yamlHandler.get(ACCOUNT_ID, envServiceOverrideValidYamlFilePath);
    compareAppManifest(applicationManifest, applicationManifestFromGet);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testCRUDAndGetForEnvServiceOverrideWithRemoteStoreType() throws HarnessException, IOException {
    ApplicationManifest applicationManifest = ApplicationManifest.builder()
                                                  .envId(WingsTestConstants.ENV_ID)
                                                  .serviceId(SERVICE_ID)
                                                  .storeType(StoreType.Remote)
                                                  .gitFileConfig(GitFileConfig.builder()
                                                                     .filePath("ABC/")
                                                                     .branch("BRANCH")
                                                                     .useBranch(true)
                                                                     .connectorId(CONNECTOR_ID)
                                                                     .build())
                                                  .kind(AppManifestKind.VALUES)
                                                  .build();

    ChangeContext<ApplicationManifest.Yaml> changeContext =
        createChangeContext(envServiceOverrideRemoteValidYamlContent, envServiceOverrideValidYamlFilePath);

    ApplicationManifest.Yaml yamlObject =
        (ApplicationManifest.Yaml) getYaml(envServiceOverrideRemoteValidYamlContent, ApplicationManifest.Yaml.class);
    changeContext.setYaml(yamlObject);

    ApplicationManifest savedApplicationManifest = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    compareAppManifest(applicationManifest, savedApplicationManifest);

    validateYamlContent(envServiceOverrideRemoteValidYamlContent, applicationManifest);

    ApplicationManifest applicationManifestFromGet = yamlHandler.get(ACCOUNT_ID, envServiceOverrideValidYamlFilePath);
    compareAppManifest(applicationManifest, applicationManifestFromGet);
  }
}
