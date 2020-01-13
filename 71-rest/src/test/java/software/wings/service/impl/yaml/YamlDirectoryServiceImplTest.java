package software.wings.service.impl.yaml;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.yaml.YamlConstants.APPLICATION_TEMPLATE_LIBRARY_FOLDER;
import static software.wings.beans.yaml.YamlConstants.GLOBAL_TEMPLATE_LIBRARY_FOLDER;
import static software.wings.common.TemplateConstants.SHELL_SCRIPT;

import io.harness.CategoryTest;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.Application;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.common.TemplateConstants;
import software.wings.service.intfc.template.TemplateService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.yaml.YamlVersion;
import software.wings.yaml.directory.AccountLevelYamlNode;
import software.wings.yaml.directory.AppLevelYamlNode;
import software.wings.yaml.directory.DirectoryNode;
import software.wings.yaml.directory.DirectoryPath;
import software.wings.yaml.directory.FolderNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class YamlDirectoryServiceImplTest extends CategoryTest {
  @Mock private YamlGitService yamlGitSyncService;
  @Mock private TemplateService templateService;

  @InjectMocks private YamlDirectoryServiceImpl yamlDirectoryService = new YamlDirectoryServiceImpl();
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }
  @Test
  @Owner(developers = OwnerRule.ROHIT_KUMAR)
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
    PageResponse<Template> pageResponse = aPageResponse().withResponse(Arrays.asList(template1, template2)).build();
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
  @Owner(developers = OwnerRule.ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_doTemplateLibraryForApp_GLOBAL() {
    final String accountid = "accountid";
    DirectoryPath directoryPath = new DirectoryPath("/Setup");

    final Template template1 = Template.builder().name("template1").folderId("root_folder").type(SHELL_SCRIPT).build();
    final Template template2 =
        Template.builder().folderId("child_folder").name("child_folder_template").type(SHELL_SCRIPT).build();
    PageResponse<Template> pageResponse = aPageResponse().withResponse(Arrays.asList(template1, template2)).build();
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
}