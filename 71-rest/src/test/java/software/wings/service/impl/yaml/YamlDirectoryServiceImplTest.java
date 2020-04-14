package software.wings.service.impl.yaml;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.appmanifest.StoreType.HelmChartRepo;
import static software.wings.beans.yaml.YamlConstants.APPLICATION_TEMPLATE_LIBRARY_FOLDER;
import static software.wings.beans.yaml.YamlConstants.GLOBAL_TEMPLATE_LIBRARY_FOLDER;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.common.TemplateConstants.SHELL_SCRIPT;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.CollectionUtils;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.PcfConfig;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.common.TemplateConstants;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.yaml.YamlVersion;
import software.wings.yaml.directory.AccountLevelYamlNode;
import software.wings.yaml.directory.AppLevelYamlNode;
import software.wings.yaml.directory.DirectoryNode;
import software.wings.yaml.directory.DirectoryPath;
import software.wings.yaml.directory.FolderNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class YamlDirectoryServiceImplTest extends WingsBaseTest {
  @Mock private YamlGitService yamlGitSyncService;
  @Mock private TemplateService templateService;
  @Mock private AppService appService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private ApplicationManifestService applicationManifestService;
  @Mock private ServiceResourceService serviceResourceService;

  @InjectMocks private YamlDirectoryServiceImpl yamlDirectoryService = new YamlDirectoryServiceImpl();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_doTemplateLibraryForApp() {
    final String accountid = "accountid";
    final String appid = "appid";
    final Application application = Application.Builder.anApplication().accountId(accountid).appId(appid).build();
    DirectoryPath directoryPath = new DirectoryPath("/Setup/Applications/app");
    TemplateFolder templateFolder = TemplateFolder.builder().uuid("root_folder").name("harness").build();
    doReturn(templateFolder)
        .when(templateService)
        .getTemplateTree(anyString(), anyString(), anyString(), eq(TemplateConstants.TEMPLATE_TYPES_WITH_YAML_SUPPORT));
    final Template template1 = Template.builder().name("template1").folderId("root_folder").type(SHELL_SCRIPT).build();
    final Template template2 = Template.builder().folderId("root_folder").name("template2").type(SHELL_SCRIPT).build();
    PageResponse<Template> pageResponse = aPageResponse().withResponse(asList(template1, template2)).build();
    doReturn(pageResponse).when(templateService).list(any(PageRequest.class));

    final FolderNode folderNode = yamlDirectoryService.doTemplateLibraryForApp(application, directoryPath);
    assertThat(folderNode.getName()).isEqualTo(APPLICATION_TEMPLATE_LIBRARY_FOLDER);
    assertThat(folderNode.getAccountId()).isEqualTo(accountid);
    final List<DirectoryNode> children = folderNode.getChildren();
    assertThat(children.size()).isEqualTo(1);
    assertThat(children.get(0).getName()).isEqualTo("harness");
    final List<DirectoryNode> leaves = ((FolderNode) children.get(0)).getChildren();
    assertThat(leaves.size()).isEqualTo(2);
    final AppLevelYamlNode templateNode = (AppLevelYamlNode) leaves.get(0);
    assertThat(templateNode.getName()).isEqualTo("template1.yaml");
    assertThat(templateNode.getAccountId()).isEqualTo(accountid);
    assertThat(templateNode.getAppId()).isEqualTo(appid);
    final AppLevelYamlNode templateNode2 = (AppLevelYamlNode) leaves.get(1);

    assertThat(templateNode2.getName()).isEqualTo("template2.yaml");
    assertThat(templateNode2.getAccountId()).isEqualTo(accountid);
    assertThat(templateNode2.getAppId()).isEqualTo(appid);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_doTemplateLibraryForApp_GLOBAL() {
    final String accountid = "accountid";
    DirectoryPath directoryPath = new DirectoryPath("/Setup");

    final Template template1 = Template.builder().name("template1").folderId("root_folder").type(SHELL_SCRIPT).build();
    final Template template2 =
        Template.builder().folderId("child_folder").name("child_folder_template").type(SHELL_SCRIPT).build();
    PageResponse<Template> pageResponse = aPageResponse().withResponse(asList(template1, template2)).build();
    doReturn(pageResponse).when(templateService).list(any(PageRequest.class));

    TemplateFolder childFolder = TemplateFolder.builder().uuid("child_folder").name("child_folder_1").build();
    TemplateFolder templateFolder = TemplateFolder.builder()
                                        .uuid("root_folder")
                                        .name("harness")
                                        .children(Collections.singletonList(childFolder))
                                        .build();
    doReturn(templateFolder)
        .when(templateService)
        .getTemplateTree(anyString(), anyString(), anyString(), eq(TemplateConstants.TEMPLATE_TYPES_WITH_YAML_SUPPORT));

    final FolderNode folderNode = yamlDirectoryService.doTemplateLibrary(accountid, directoryPath.clone(),
        GLOBAL_APP_ID, GLOBAL_TEMPLATE_LIBRARY_FOLDER, YamlVersion.Type.GLOBAL_TEMPLATE_LIBRARY);
    assertThat(folderNode.getName()).isEqualTo(GLOBAL_TEMPLATE_LIBRARY_FOLDER);
    assertThat(folderNode.getAccountId()).isEqualTo(accountid);
    assertThat(folderNode.getDirectoryPath().getPath()).isEqualTo("/Setup/" + GLOBAL_TEMPLATE_LIBRARY_FOLDER);
    final List<DirectoryNode> children = folderNode.getChildren();
    assertThat(children.size()).isEqualTo(1);
    assertThat(children.get(0).getName()).isEqualTo("harness");
    final List<DirectoryNode> harnessChildren = ((FolderNode) children.get(0)).getChildren();
    assertThat(harnessChildren.size()).isEqualTo(2);
    final AccountLevelYamlNode templateNode = (AccountLevelYamlNode) harnessChildren.get(1);
    assertThat(templateNode.getAccountId()).isEqualTo(accountid);
    assertThat(templateNode.getName()).isEqualTo("template1.yaml");
    final FolderNode childFolderNode = (FolderNode) harnessChildren.get(0);
    assertThat(childFolderNode.getAccountId()).isEqualTo(accountid);
    assertThat(childFolderNode.getName()).isEqualTo("child_folder_1");

    assertThat(childFolderNode.getChildren().size()).isEqualTo(1);

    final AccountLevelYamlNode template2Node = (AccountLevelYamlNode) childFolderNode.getChildren().get(0);
    assertThat(template2Node.getAccountId()).isEqualTo(accountid);
    assertThat(template2Node.getName()).isEqualTo("child_folder_template.yaml");
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetRootByTemplate() {
    Template template = Template.builder().appId(GLOBAL_APP_ID).build();
    String folderName = "Harness/name";
    when(templateService.getTemplateFolderPathString(template)).thenReturn(folderName);
    assertThat(yamlDirectoryService.getRootPathByTemplate(template))
        .isEqualTo(yamlDirectoryService.getRootPath() + PATH_DELIMITER + GLOBAL_TEMPLATE_LIBRARY_FOLDER + PATH_DELIMITER
            + folderName);
    template.setAppId("random");
    Application mockApp = Application.Builder.anApplication().name("random").build();
    when(appService.get("random")).thenReturn(mockApp);
    assertThat(yamlDirectoryService.getRootPathByTemplate(template))
        .isEqualTo(yamlDirectoryService.getRootPathByApp(mockApp) + PATH_DELIMITER + APPLICATION_TEMPLATE_LIBRARY_FOLDER
            + PATH_DELIMITER + folderName);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGenerateEnvHelmOverridesFolder() {
    Environment env = anEnvironment().uuid(ENV_ID).appId(APP_ID).uuid(ENV_ID).build();
    DirectoryPath dirPath = new DirectoryPath("Setup/Applications/Test/Environments/Dev");
    testHelmOverrideFolderIfNonePresent(env, dirPath);
    testHelmOverrideFolderIfOnlyServiceOverridePresent(env, dirPath);
    testHelmOverrideFolderIfOnlyGlobalOverridePresent(env, dirPath);
    testHelmOverrideFolderIfAllTypeOfOverridesPresent(env, dirPath);
  }

  private void testHelmOverrideFolderIfNonePresent(Environment env, DirectoryPath dirPath) {
    doReturn(null)
        .doReturn(Collections.emptyList())
        .when(applicationManifestService)
        .getAllByEnvIdAndKind(anyString(), anyString(), eq(AppManifestKind.HELM_CHART_OVERRIDE));
    doReturn(null)
        .when(applicationManifestService)
        .getByEnvId(anyString(), anyString(), eq(AppManifestKind.HELM_CHART_OVERRIDE));

    assertThat(yamlDirectoryService.generateEnvHelmOverridesFolder(ACCOUNT_ID, env, dirPath)).isNull();
    assertThat(yamlDirectoryService.generateEnvHelmOverridesFolder(ACCOUNT_ID, env, dirPath)).isNull();
  }

  private void testHelmOverrideFolderIfOnlyServiceOverridePresent(Environment env, DirectoryPath dirPath) {
    ApplicationManifest serviceManifest1 =
        ApplicationManifest.builder().envId(ENV_ID).storeType(HelmChartRepo).serviceId("sid-1").build();
    ApplicationManifest serviceManifest2 =
        ApplicationManifest.builder().envId(ENV_ID).storeType(HelmChartRepo).serviceId("sid-2").build();

    doReturn(asList(serviceManifest1, serviceManifest2))
        .when(applicationManifestService)
        .getAllByEnvIdAndKind(anyString(), anyString(), eq(AppManifestKind.HELM_CHART_OVERRIDE));
    doReturn(null)
        .when(applicationManifestService)
        .getByEnvId(anyString(), anyString(), eq(AppManifestKind.HELM_CHART_OVERRIDE));
    doReturn(Service.builder().name("s-1").uuid("sid-1").appId(APP_ID).build())
        .when(serviceResourceService)
        .get(APP_ID, "sid-1", false);
    doReturn(Service.builder().name("s-2").uuid("sid-2").appId(APP_ID).build())
        .when(serviceResourceService)
        .get(APP_ID, "sid-2", false);

    final FolderNode folder = yamlDirectoryService.generateEnvHelmOverridesFolder(ACCOUNT_ID, env, dirPath);

    assertThat(getFolderStructure(folder))
        .containsExactly("Setup/Applications/Test/Environments/Dev/Helm Chart Overrides/Services",
            "Setup/Applications/Test/Environments/Dev/Helm Chart Overrides/Services/s-1",
            "Setup/Applications/Test/Environments/Dev/Helm Chart Overrides/Services/s-1/Index.yaml",
            "Setup/Applications/Test/Environments/Dev/Helm Chart Overrides/Services/s-2",
            "Setup/Applications/Test/Environments/Dev/Helm Chart Overrides/Services/s-2/Index.yaml");
  }

  private void testHelmOverrideFolderIfOnlyGlobalOverridePresent(Environment env, DirectoryPath dirPath) {
    ApplicationManifest globalManifest = ApplicationManifest.builder().envId(ENV_ID).storeType(HelmChartRepo).build();

    doReturn(asList(globalManifest))
        .when(applicationManifestService)
        .getAllByEnvIdAndKind(anyString(), anyString(), eq(AppManifestKind.HELM_CHART_OVERRIDE));
    doReturn(globalManifest)
        .when(applicationManifestService)
        .getByEnvId(anyString(), anyString(), eq(AppManifestKind.HELM_CHART_OVERRIDE));

    final FolderNode folder = yamlDirectoryService.generateEnvHelmOverridesFolder(ACCOUNT_ID, env, dirPath);

    assertThat(getFolderStructure(folder))
        .containsExactly("Setup/Applications/Test/Environments/Dev/Helm Chart Overrides/Index.yaml",
            "Setup/Applications/Test/Environments/Dev/Helm Chart Overrides/Services");
  }

  private void testHelmOverrideFolderIfAllTypeOfOverridesPresent(Environment env, DirectoryPath dirPath) {
    ApplicationManifest serviceManifest1 =
        ApplicationManifest.builder().envId(ENV_ID).storeType(HelmChartRepo).serviceId("sid-1").build();
    ApplicationManifest serviceManifest2 =
        ApplicationManifest.builder().envId(ENV_ID).storeType(HelmChartRepo).serviceId("sid-2").build();
    ApplicationManifest globalManifest = ApplicationManifest.builder().envId(ENV_ID).storeType(HelmChartRepo).build();

    doReturn(asList(serviceManifest1, serviceManifest2, globalManifest))
        .when(applicationManifestService)
        .getAllByEnvIdAndKind(anyString(), anyString(), eq(AppManifestKind.HELM_CHART_OVERRIDE));
    doReturn(globalManifest)
        .when(applicationManifestService)
        .getByEnvId(anyString(), anyString(), eq(AppManifestKind.HELM_CHART_OVERRIDE));
    doReturn(Service.builder().name("s-1").uuid("sid-1").appId(APP_ID).build())
        .when(serviceResourceService)
        .get(APP_ID, "sid-1", false);
    doReturn(Service.builder().name("s-2").uuid("sid-2").appId(APP_ID).build())
        .when(serviceResourceService)
        .get(APP_ID, "sid-2", false);

    final FolderNode folder = yamlDirectoryService.generateEnvHelmOverridesFolder(ACCOUNT_ID, env, dirPath);

    assertThat(getFolderStructure(folder))
        .containsExactly("Setup/Applications/Test/Environments/Dev/Helm Chart Overrides/Index.yaml",
            "Setup/Applications/Test/Environments/Dev/Helm Chart Overrides/Services",
            "Setup/Applications/Test/Environments/Dev/Helm Chart Overrides/Services/s-1",
            "Setup/Applications/Test/Environments/Dev/Helm Chart Overrides/Services/s-1/Index.yaml",
            "Setup/Applications/Test/Environments/Dev/Helm Chart Overrides/Services/s-2",
            "Setup/Applications/Test/Environments/Dev/Helm Chart Overrides/Services/s-2/Index.yaml");
  }

  private List<String> getFolderStructure(FolderNode fn) {
    List<String> structure = new ArrayList<>();
    CollectionUtils.emptyIfNull(fn.getChildren()).forEach(child -> {
      if (child instanceof FolderNode) {
        structure.add(child.getDirectoryPath().getPath());
        structure.addAll(getFolderStructure((FolderNode) child));
      } else {
        structure.add(child.getDirectoryPath().getPath());
      }
    });
    return structure;
  }

  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testPathForSpotInstAndPcfConfig() {
    SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withValue(PcfConfig.builder().build()).build();
    String path = yamlDirectoryService.getRootPathBySettingAttribute(settingAttribute);
    assertThat(path).isEqualTo("Setup/Cloud Providers");

    settingAttribute.setValue(SpotInstConfig.builder().build());
    yamlDirectoryService.getRootPathBySettingAttribute(settingAttribute);
    assertThat(path).isEqualTo("Setup/Cloud Providers");
  }
}