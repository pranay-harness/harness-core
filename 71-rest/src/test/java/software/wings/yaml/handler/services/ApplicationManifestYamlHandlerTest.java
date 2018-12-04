package software.wings.yaml.handler.services;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.Application;
import software.wings.beans.GitFileConfig;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Builder;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.service.impl.GitFileConfigHelperService;
import software.wings.service.impl.yaml.handler.service.ApplicationManifestYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.utils.WingsTestConstants;
import software.wings.yaml.handler.BaseYamlHandlerTest;

import java.io.IOException;

public class ApplicationManifestYamlHandlerTest extends BaseYamlHandlerTest {
  @Mock private AppService appService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private SettingsService settingsService;

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

  @Before
  public void setUp() {
    localApplicationManifest =
        ApplicationManifest.builder().serviceId(WingsTestConstants.SERVICE_ID).storeType(StoreType.Local).build();
    remoteApplicationManifest = ApplicationManifest.builder()
                                    .serviceId(WingsTestConstants.SERVICE_ID)
                                    .storeType(StoreType.Remote)
                                    .gitFileConfig(GitFileConfig.builder()
                                                       .filePath("ABC/")
                                                       .branch("BRANCH")
                                                       .useBranch(true)
                                                       .connectorId(CONNECTOR_ID)
                                                       .build())
                                    .build();

    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    when(appService.getAppByName(ACCOUNT_ID, APP_NAME))
        .thenReturn(Application.Builder.anApplication().withUuid(APP_ID).withName(APP_NAME).build());
    when(serviceResourceService.getServiceByName(APP_ID, SERVICE_NAME))
        .thenReturn(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build());
    when(serviceResourceService.exist(any(), any())).thenReturn(true);

    SettingAttribute settingAttribute =
        Builder.aSettingAttribute().withName(CONNECTOR_NAME).withUuid(CONNECTOR_ID).build();
    when(settingsService.get(CONNECTOR_ID)).thenReturn(settingAttribute);
    when(settingsService.getByName(ACCOUNT_ID, APP_ID, CONNECTOR_NAME)).thenReturn(settingAttribute);
  }

  @Test
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
    assertNotNull(yaml);
    assertNotNull(yaml.getType());

    String yamlContent = getYamlContent(yaml);
    assertNotNull(yamlContent);
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertEquals(yamlFileContent, yamlContent);
  }

  private void compareAppManifest(ApplicationManifest lhs, ApplicationManifest rhs) {
    assertEquals(lhs.getStoreType(), rhs.getStoreType());
    assertEquals(lhs.getGitFileConfig(), rhs.getGitFileConfig());
    if (lhs.getGitFileConfig() != null) {
      assertEquals(lhs.getGitFileConfig().getConnectorId(), rhs.getGitFileConfig().getConnectorId());
      assertEquals(lhs.getGitFileConfig().getBranch(), rhs.getGitFileConfig().getBranch());
      assertEquals(lhs.getGitFileConfig().getFilePath(), rhs.getGitFileConfig().getFilePath());
      assertEquals(lhs.getGitFileConfig().isUseBranch(), rhs.getGitFileConfig().isUseBranch());
      assertEquals(lhs.getGitFileConfig().getConnectorName(), rhs.getGitFileConfig().getConnectorName());
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
}
