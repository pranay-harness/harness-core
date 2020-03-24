package software.wings.service.impl.yaml;

import static io.harness.event.handler.impl.Constants.ACCOUNT_ID;
import static io.harness.rule.OwnerRule.ABHINAV;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.common.TemplateConstants.SSH;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.EntityType;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.List;

public class YamlGitServiceTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private TemplateService templateService;
  @InjectMocks @Inject private YamlGitServiceImpl yamlGitService;
  @Inject protected TemplateGalleryService templateGalleryService;
  @Inject private TemplateFolderService templateFolderService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    templateGalleryService.loadHarnessGallery();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSyncForTemplates() {
    TemplateFolder rootLevelFolder = templateFolderService.getRootLevelFolder(
        GLOBAL_ACCOUNT_ID, templateGalleryService.getByAccount(GLOBAL_ACCOUNT_ID).getUuid());

    TemplateFolder templateFolder = TemplateFolder.builder()
                                        .appId(GLOBAL_APP_ID)
                                        .accountId(GLOBAL_ACCOUNT_ID)
                                        .parentId(rootLevelFolder.getUuid())
                                        .name("test folder")
                                        .build();
    templateFolder = templateFolderService.save(templateFolder);

    BaseTemplate baseTemplate = SshCommandTemplate.builder().build();
    Template template = Template.builder()
                            .appId(GLOBAL_APP_ID)
                            .name("test template")
                            .uuid("uuidtemplate")
                            .version(1)
                            .accountId(GLOBAL_ACCOUNT_ID)
                            .folderId(templateFolder.getUuid())
                            .type(SSH)
                            .templateObject(baseTemplate)
                            .build();
    templateService.save(template);

    YamlGitConfig yamlGitConfig = YamlGitConfig.builder()
                                      .accountId(GLOBAL_ACCOUNT_ID)
                                      .entityId(GLOBAL_ACCOUNT_ID)
                                      .enabled(true)
                                      .syncMode(YamlGitConfig.SyncMode.BOTH)
                                      .entityType(EntityType.ACCOUNT)
                                      .build();
    wingsPersistence.save(yamlGitConfig);
    yamlGitService.syncForTemplates(GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID);
    YamlChangeSet yamlChangeSet = wingsPersistence.createQuery(YamlChangeSet.class).get();
    assertThat(yamlChangeSet).isNotNull();
    boolean isPresent = false;
    String filePath = "Setup/Template Library/" + rootLevelFolder.getName() + "/test folder/test template.yaml";
    for (GitFileChange gitFileChange : yamlChangeSet.getGitFileChanges()) {
      if (gitFileChange.getFilePath().equals(filePath)) {
        isPresent = true;
        break;
      }
    }
    assertThat(isPresent).isEqualTo(true);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testListGitSync() {
    String firstCommitName = "commit1";
    String secondCommitName = "commit2";
    String firstCommitUpdatedName = "updated-commit1";
    GitSyncError gitSyncError = GitSyncError.builder().accountId(ACCOUNT_ID).gitCommitId(firstCommitName).build();
    wingsPersistence.save(gitSyncError);
    wingsPersistence.save(GitSyncError.builder().accountId(ACCOUNT_ID).gitCommitId(secondCommitName).build());

    RestResponse<List<GitSyncError>> response = yamlGitService.listGitSyncErrors(ACCOUNT_ID);
    assertThat(response.getResource().get(0).getGitCommitId()).isEqualTo(firstCommitName);
    assertThat(response.getResource().get(1).getGitCommitId()).isEqualTo(secondCommitName);

    gitSyncError.setGitCommitId(firstCommitUpdatedName);
    wingsPersistence.save(gitSyncError);

    response = yamlGitService.listGitSyncErrors(ACCOUNT_ID);
    assertThat(response.getResource().get(0).getGitCommitId()).isEqualTo(firstCommitUpdatedName);
    assertThat(response.getResource().get(1).getGitCommitId()).isEqualTo(secondCommitName);
  }
}
