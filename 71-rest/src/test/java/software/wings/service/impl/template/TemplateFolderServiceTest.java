package software.wings.service.impl.template;

import static io.harness.rule.OwnerRule.AADITI;
import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.template.TemplateType.SSH;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.common.TemplateConstants.JBOSS_COMMANDS;
import static software.wings.common.TemplateConstants.LOAD_BALANCERS;
import static software.wings.common.TemplateConstants.PATH_DELIMITER;
import static software.wings.common.TemplateConstants.TOMCAT_COMMANDS;
import static software.wings.common.TemplateConstants.TOMCAT_WAR_INSTALL_PATH;
import static software.wings.utils.TemplateTestConstants.GLOBAL_FOLDER;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_DESC_CHANGED;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_FOLDER_DEC;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_FOLDER_NAME;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_FOLDER_NAME_2;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_GALLERY;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_GALLERY_DESC;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.INVALID_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_NAME;

import com.google.common.collect.ImmutableSet;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.EntityType;
import software.wings.beans.FeatureName;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.common.TemplateConstants;
import software.wings.utils.WingsTestConstants;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.Arrays;
import javax.validation.ConstraintViolationException;

public class TemplateFolderServiceTest extends TemplateBaseTestHelper {
  @Test(expected = InvalidRequestException.class)
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldNotDeleteRootFolder() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    templateFolderService.delete(parentFolder.getUuid());
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldSaveTemplateFolder() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder myTemplateFolder = templateFolderService.save(constructTemplateBuilder(parentFolder.getUuid()));

    assertThat(myTemplateFolder).isNotNull();
    assertThat(myTemplateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(myTemplateFolder.getAppId()).isEqualTo(GLOBAL_APP_ID);
    assertThat(myTemplateFolder.getKeywords()).contains(TEMPLATE_FOLDER_NAME.toLowerCase());
    assertThat(myTemplateFolder.getKeywords()).contains(TEMPLATE_FOLDER_DEC.toLowerCase());
    assertThat(myTemplateFolder.getPathId()).isNotEmpty();
    assertThat(myTemplateFolder.getPathId().split("/")[0]).isEqualTo(parentFolder.getUuid());
  }

  @Test(expected = ConstraintViolationException.class)
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldNotSaveInvalidNameTemplateFolder() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder templateFolder = constructTemplateBuilder(parentFolder.getUuid());
    templateFolder.setName(WingsTestConstants.INVALID_NAME);
    TemplateFolder myTemplateFolder = templateFolderService.save(templateFolder);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetTemplateFolder() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder myTemplateFolder = templateFolderService.save(constructTemplateBuilder(parentFolder.getUuid()));

    assertThat(myTemplateFolder).isNotNull();
    assertThat(myTemplateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(myTemplateFolder.getKeywords()).contains(TEMPLATE_FOLDER_NAME.toLowerCase());
    assertThat(myTemplateFolder.getKeywords()).contains(TEMPLATE_FOLDER_DEC.toLowerCase());
    assertThat(myTemplateFolder.getPathId()).isNotEmpty();
    assertThat(myTemplateFolder.getPathId().split("/")[0]).isEqualTo(parentFolder.getUuid());

    TemplateFolder savedTemplateFolder = templateFolderService.get(myTemplateFolder.getUuid());
    assertThat(savedTemplateFolder).isNotNull();
    assertThat(savedTemplateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(savedTemplateFolder.getKeywords()).contains(TEMPLATE_FOLDER_NAME.toLowerCase());
    assertThat(savedTemplateFolder.getKeywords()).contains(TEMPLATE_FOLDER_DEC.toLowerCase());
    assertThat(savedTemplateFolder.getPathId()).isNotEmpty();
    assertThat(savedTemplateFolder.getPathId().split("/")[0]).isEqualTo(parentFolder.getUuid());
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldDeleteTemplateFolder() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder myTemplateFolder = templateFolderService.save(constructTemplateBuilder(parentFolder.getUuid()));

    assertThat(myTemplateFolder).isNotNull();
    assertThat(myTemplateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(myTemplateFolder.getKeywords()).contains(TEMPLATE_FOLDER_NAME.toLowerCase());
    assertThat(myTemplateFolder.getKeywords()).contains(TEMPLATE_FOLDER_DEC.toLowerCase());
    assertThat(myTemplateFolder.getPathId()).isNotEmpty();
    assertThat(myTemplateFolder.getPathId().split("/")[0]).isEqualTo(parentFolder.getUuid());

    assertThat(templateFolderService.delete(myTemplateFolder.getUuid())).isTrue();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldUpdateTemplateFolder() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder myTemplateFolder = templateFolderService.save(constructTemplateBuilder(parentFolder.getUuid()));

    assertThat(myTemplateFolder).isNotNull();
    assertThat(myTemplateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(myTemplateFolder.getGalleryId()).isNotEmpty();
    assertThat(myTemplateFolder.getPathId()).isNotEmpty();
    assertThat(myTemplateFolder.getPathId().split("/")[0]).isEqualTo(parentFolder.getUuid());

    myTemplateFolder.setDescription(TEMPLATE_DESC_CHANGED);
    TemplateFolder updatedTemplateFolder = templateFolderService.update(myTemplateFolder);

    assertThat(updatedTemplateFolder).isNotNull();
    assertThat(updatedTemplateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(updatedTemplateFolder.getKeywords()).contains(TEMPLATE_DESC_CHANGED.toLowerCase());
    assertThat(updatedTemplateFolder.getPathId()).isNotEmpty();
    assertThat(updatedTemplateFolder.getPathId().split("/")[0]).isEqualTo(parentFolder.getUuid());
  }

  @Test(expected = ConstraintViolationException.class)
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldNotUpdateWithInvalidNameTemplateFolder() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder myTemplateFolder = templateFolderService.save(constructTemplateBuilder(parentFolder.getUuid()));

    assertThat(myTemplateFolder).isNotNull();
    assertThat(myTemplateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);

    myTemplateFolder.setDescription(TEMPLATE_DESC_CHANGED);
    myTemplateFolder.setName(INVALID_NAME);

    templateFolderService.update(myTemplateFolder);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldLoadTemplateFolders() {
    TemplateFolder templateFolder = templateFolderService.getTemplateTree(GLOBAL_ACCOUNT_ID, null, null);
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getChildren()).isNotEmpty();
    assertThat(templateFolder).extracting(TemplateFolder::getName).isEqualTo(HARNESS_GALLERY);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetGlobalTemplateTree() {
    TemplateFolder templateFolder = templateFolderService.getTemplateTree(GLOBAL_ACCOUNT_ID, null, null);
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder).extracting(TemplateFolder::getName).isEqualTo(HARNESS_GALLERY);
    assertThat(templateFolder.getChildren())
        .isNotEmpty()
        .extracting(templateFolder1 -> templateFolder1.getName().contains(TOMCAT_COMMANDS));
    assertThat(templateFolder.getChildren())
        .isNotEmpty()
        .extracting(templateFolder1 -> templateFolder1.getName().contains(JBOSS_COMMANDS));
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetGlobalTemplateRootTree() {
    TemplateFolder templateFolder = templateFolderService.getTemplateTree(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY, null);
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getChildren()).isNotEmpty();
    assertThat(templateFolder).extracting(TemplateFolder::getName).isEqualTo(HARNESS_GALLERY);
    assertThat(templateFolder.getChildren()).isNotEmpty().extracting(TemplateFolder::getName).contains(TOMCAT_COMMANDS);
    assertThat(templateFolder.getChildren())
        .isNotEmpty()
        .extracting(TemplateFolder::getName)
        .doesNotContain(JBOSS_COMMANDS);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetGlobalTemplateTreeByKeyword() {
    TemplateFolder templateFolder = templateFolderService.getTemplateTree(GLOBAL_ACCOUNT_ID, TOMCAT_COMMANDS, null);
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getChildren()).isNotEmpty();
    assertThat(templateFolder).extracting(TemplateFolder::getName).isEqualTo(HARNESS_GALLERY);
    assertThat(templateFolder.getChildren()).isNotEmpty();

    assertThat(templateFolder.getChildren()).extracting(TemplateFolder::getName).contains(TOMCAT_COMMANDS);
    assertThat(templateFolder.getChildren()).extracting(TemplateFolder::getName).doesNotContain(LOAD_BALANCERS);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldLoadDefaultCommandTemplates() {
    templateService.loadDefaultTemplates(SSH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder templateFolder =
        templateFolderService.getTemplateTree(GLOBAL_ACCOUNT_ID, null, Arrays.asList(SSH.name()));
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getChildren()).isNotEmpty();
    assertThat(templateFolder).extracting(TemplateFolder::getName).isEqualTo(HARNESS_GALLERY);
    assertThat(templateFolder.getChildren()).isNotEmpty();

    assertThat(templateFolder.getChildren()).extracting(TemplateFolder::getName).contains(TOMCAT_COMMANDS);
    assertThat(templateFolder.getChildren()).extracting(TemplateFolder::getName).doesNotContain(LOAD_BALANCERS);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetGlobalTemplateTreeByKeywordAndTypes() {
    templateService.loadDefaultTemplates(SSH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder templateFolder =
        templateFolderService.getTemplateTree(GLOBAL_ACCOUNT_ID, "Install", Arrays.asList(SSH.name()));
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getAccountId()).isNotNull().isEqualTo(GLOBAL_ACCOUNT_ID);
    assertThat(templateFolder.getChildren()).isNotEmpty();
    assertThat(templateFolder).extracting(TemplateFolder::getName).isEqualTo(HARNESS_GALLERY);
    assertThat(templateFolder.getChildren()).isNotEmpty();

    assertThat(templateFolder.getChildren()).extracting(TemplateFolder::getName).contains(TOMCAT_COMMANDS);
    assertThat(templateFolder.getChildren()).extracting(TemplateFolder::getName).doesNotContain(LOAD_BALANCERS);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetGlobalTemplateTreeByKeywordAndTypesNotMatching() {
    templateService.loadDefaultTemplates(SSH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder templateFolder =
        templateFolderService.getTemplateTree(GLOBAL_ACCOUNT_ID, "Install3", Arrays.asList(SSH.name()));
    assertThat(templateFolder).isNull();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldCopyHarnessTemplateFolders() {
    TemplateGallery templateGallery = templateGalleryService.get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    templateService.loadDefaultTemplates(SSH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);

    templateFolderService.copyHarnessTemplateFolders(templateGallery.getUuid(), ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder templateFolder = templateFolderService.getTemplateTree(ACCOUNT_ID, null, null);

    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getAccountId()).isNotNull().isEqualTo(ACCOUNT_ID);
    assertThat(templateFolder).extracting(TemplateFolder::getName).isEqualTo(HARNESS_GALLERY);
    assertThat(templateFolder.getChildren())
        .isNotEmpty()
        .extracting(templateFolder1 -> templateFolder1.getName().contains(TOMCAT_COMMANDS));
    assertThat(templateFolder.getChildren())
        .isNotEmpty()
        .extracting(templateFolder1 -> templateFolder1.getName().contains(LOAD_BALANCERS));
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetAccountTemplateTree() {
    TemplateGallery templateGallery = templateGalleryService.get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    templateService.loadDefaultTemplates(SSH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    templateFolderService.copyHarnessTemplateFolders(templateGallery.getUuid(), ACCOUNT_ID, HARNESS_GALLERY);

    TemplateFolder templateFolder = templateFolderService.getTemplateTree(ACCOUNT_ID, null, null);
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getAccountId()).isNotNull().isEqualTo(ACCOUNT_ID);
    assertThat(templateFolder).extracting(TemplateFolder::getName).isEqualTo(HARNESS_GALLERY);
    assertThat(templateFolder.getChildren())
        .isNotEmpty()
        .extracting(templateFolder1 -> templateFolder1.getName().contains(TOMCAT_COMMANDS));
    assertThat(templateFolder.getChildren())
        .isNotEmpty()
        .extracting(templateFolder1 -> templateFolder1.getName().contains(LOAD_BALANCERS));
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldLoadTomcatStandardInstallCommand() {
    TemplateGallery templateGallery = templateGalleryService.get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    templateGalleryService.save(TemplateGallery.builder()
                                    .name(TEMPLATE_GALLERY)
                                    .accountId(ACCOUNT_ID)
                                    .description(TEMPLATE_GALLERY_DESC)
                                    .appId(GLOBAL_APP_ID)
                                    .keywords(ImmutableSet.of("CD"))
                                    .build());
    templateService.loadDefaultTemplates(SSH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    templateFolderService.copyHarnessTemplateFolders(templateGallery.getUuid(), ACCOUNT_ID, TEMPLATE_GALLERY);
    templateService.loadYaml(SSH, TOMCAT_WAR_INSTALL_PATH, ACCOUNT_ID, TEMPLATE_GALLERY);
    TemplateFolder templateFolder =
        templateFolderService.getTemplateTree(ACCOUNT_ID, "Install", Arrays.asList(SSH.name()));
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getAccountId()).isNotNull().isEqualTo(ACCOUNT_ID);
    assertThat(templateFolder.getChildren()).isNotEmpty();
    assertThat(templateFolder).extracting(TemplateFolder::getName).isEqualTo(TEMPLATE_GALLERY);
    assertThat(templateFolder.getChildren()).isNotEmpty();

    assertThat(templateFolder.getChildren()).extracting(TemplateFolder::getName).contains(TOMCAT_COMMANDS);

    assertThat(templateFolder.getChildren()).extracting(TemplateFolder::getName).doesNotContain(LOAD_BALANCERS);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetRootFolder() {
    TemplateGallery templateGallery = templateGalleryService.get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    templateService.loadDefaultTemplates(SSH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    templateFolderService.copyHarnessTemplateFolders(templateGallery.getUuid(), ACCOUNT_ID, HARNESS_GALLERY);

    TemplateFolder templateFolder = templateFolderService.getByFolderPath(ACCOUNT_ID, HARNESS_GALLERY);

    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getName()).isEqualTo(HARNESS_GALLERY);
    assertThat(templateFolder.getPathId()).isNull();
    assertThat(templateFolder.getKeywords()).contains(HARNESS_GALLERY.toLowerCase());
    assertThat(templateFolder.getAccountId()).isNotNull().isEqualTo(ACCOUNT_ID);
    assertThat(templateFolder.getChildren()).isEmpty();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetFolderByPath() {
    TemplateGallery templateGallery = templateGalleryService.get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    templateService.loadDefaultTemplates(SSH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    templateFolderService.copyHarnessTemplateFolders(templateGallery.getUuid(), ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder templateFolder =
        templateFolderService.getByFolderPath(ACCOUNT_ID, HARNESS_GALLERY + "/" + TOMCAT_COMMANDS);

    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getName()).isEqualTo(TOMCAT_COMMANDS);
    assertThat(templateFolder.getKeywords()).contains(TOMCAT_COMMANDS.toLowerCase());
    assertThat(templateFolder.getAccountId()).isNotNull().isEqualTo(ACCOUNT_ID);
    assertThat(templateFolder.getPathId()).isNotNull();

    TemplateFolder parentFolder = templateFolderService.getByFolderPath(ACCOUNT_ID, HARNESS_GALLERY);
    assertThat(parentFolder.getName()).isEqualTo(HARNESS_GALLERY);
    assertThat(parentFolder.getPathId()).isNull();
    assertThat(templateFolder.getPathId().split("/")[0]).isEqualTo(parentFolder.getUuid());
  }

  private TemplateFolder constructTemplateBuilder(String parentId) {
    return constructTemplateBuilder(parentId, GLOBAL_APP_ID);
  }

  private TemplateFolder constructTemplateBuilder(String parentId, String appId) {
    return TemplateFolder.builder()
        .name(TEMPLATE_FOLDER_NAME)
        .description(TEMPLATE_FOLDER_DEC)
        .parentId(parentId)
        .appId(appId)
        .accountId(GLOBAL_ACCOUNT_ID)
        .build();
  }

  private TemplateFolder constructTemplateBuilder(String folderName, String parentId, String appId) {
    return TemplateFolder.builder()
        .name(folderName)
        .description(TEMPLATE_FOLDER_DEC)
        .parentId(parentId)
        .appId(appId)
        .accountId(GLOBAL_ACCOUNT_ID)
        .build();
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldSaveTemplateFolderAtApplicationLevel() {
    TemplateFolder myTemplateFolder = createAppLevelTemplateFolder();
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldSaveTemplateFoldersWithSameNameAccountAndApplicationLevel() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder myAppTemplateFolder =
        templateFolderService.save(constructTemplateBuilder(parentFolder.getUuid(), APP_ID));

    assertThat(myAppTemplateFolder).isNotNull();
    assertThat(myAppTemplateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(myAppTemplateFolder.getAppId()).isEqualTo(APP_ID);
    assertThat(myAppTemplateFolder.getKeywords()).contains(TEMPLATE_FOLDER_NAME.toLowerCase());
    assertThat(myAppTemplateFolder.getKeywords()).contains(TEMPLATE_FOLDER_DEC.toLowerCase());
    assertThat(myAppTemplateFolder.getPathId()).isNotEmpty();
    assertThat(myAppTemplateFolder.getPathId().split("/")[0]).isEqualTo(parentFolder.getUuid());

    TemplateFolder myAccountTemplateFolder =
        templateFolderService.save(constructTemplateBuilder(parentFolder.getUuid()));

    assertThat(myAccountTemplateFolder).isNotNull();
    assertThat(myAccountTemplateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(myAccountTemplateFolder.getAppId()).isEqualTo(GLOBAL_APP_ID);
    assertThat(myAccountTemplateFolder.getKeywords()).contains(TEMPLATE_FOLDER_NAME.toLowerCase());
    assertThat(myAccountTemplateFolder.getKeywords()).contains(TEMPLATE_FOLDER_DEC.toLowerCase());
    assertThat(myAccountTemplateFolder.getPathId()).isNotEmpty();
    assertThat(myAccountTemplateFolder.getPathId().split("/")[0]).isEqualTo(parentFolder.getUuid());
  }

  @Test(expected = WingsException.class)
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldNotSaveTemplateFoldersWithSameNameApplicationLevel() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder myAppTemplateFolder =
        templateFolderService.save(constructTemplateBuilder(parentFolder.getUuid(), APP_ID));

    assertThat(myAppTemplateFolder).isNotNull();
    assertThat(myAppTemplateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(myAppTemplateFolder.getAppId()).isEqualTo(APP_ID);
    assertThat(myAppTemplateFolder.getKeywords()).contains(TEMPLATE_FOLDER_NAME.toLowerCase());
    assertThat(myAppTemplateFolder.getKeywords()).contains(TEMPLATE_FOLDER_DEC.toLowerCase());
    assertThat(myAppTemplateFolder.getPathId()).isNotEmpty();
    assertThat(myAppTemplateFolder.getPathId().split("/")[0]).isEqualTo(parentFolder.getUuid());

    templateFolderService.save(constructTemplateBuilder(parentFolder.getUuid(), APP_ID));
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldUpdateTemplateFolderAtApplicationLevel() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder myTemplateFolder =
        templateFolderService.save(constructTemplateBuilder(parentFolder.getUuid(), APP_ID));

    assertThat(myTemplateFolder).isNotNull();
    assertThat(myTemplateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(myTemplateFolder.getAppId()).isEqualTo(APP_ID);
    assertThat(myTemplateFolder.getGalleryId()).isNotEmpty();
    assertThat(myTemplateFolder.getPathId()).isNotEmpty();
    assertThat(myTemplateFolder.getPathId().split("/")[0]).isEqualTo(parentFolder.getUuid());

    myTemplateFolder.setDescription(TEMPLATE_DESC_CHANGED);
    TemplateFolder updatedTemplateFolder = templateFolderService.update(myTemplateFolder);

    assertThat(updatedTemplateFolder).isNotNull();
    assertThat(updatedTemplateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(myTemplateFolder.getAppId()).isEqualTo(APP_ID);
    assertThat(updatedTemplateFolder.getKeywords()).contains(TEMPLATE_DESC_CHANGED.toLowerCase());
    assertThat(updatedTemplateFolder.getPathId()).isNotEmpty();
    assertThat(updatedTemplateFolder.getPathId().split("/")[0]).isEqualTo(parentFolder.getUuid());
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldDeleteTemplateFolderApplicationLevel() {
    TemplateFolder myTemplateFolder = createAppLevelTemplateFolder();

    assertThat(templateFolderService.delete(myTemplateFolder.getUuid())).isTrue();
  }

  private TemplateFolder createAppLevelTemplateFolder() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder myTemplateFolder =
        templateFolderService.save(constructTemplateBuilder(parentFolder.getUuid(), APP_ID));

    assertThat(myTemplateFolder).isNotNull();
    assertThat(myTemplateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(myTemplateFolder.getAppId()).isEqualTo(APP_ID);
    assertThat(myTemplateFolder.getKeywords()).contains(TEMPLATE_FOLDER_NAME.toLowerCase());
    assertThat(myTemplateFolder.getKeywords()).contains(TEMPLATE_FOLDER_DEC.toLowerCase());
    assertThat(myTemplateFolder.getPathId()).isNotEmpty();
    assertThat(myTemplateFolder.getPathId().split("/")[0]).isEqualTo(parentFolder.getUuid());
    return myTemplateFolder;
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetTemplateFolderForApplication() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder myTemplateFolder = createAppLevelTemplateFolder();
    TemplateFolder savedTemplateFolder = templateFolderService.get(myTemplateFolder.getUuid());
    assertThat(savedTemplateFolder).isNotNull();
    assertThat(savedTemplateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(myTemplateFolder.getAppId()).isEqualTo(APP_ID);
    assertThat(savedTemplateFolder.getKeywords()).contains(TEMPLATE_FOLDER_NAME.toLowerCase());
    assertThat(savedTemplateFolder.getKeywords()).contains(TEMPLATE_FOLDER_DEC.toLowerCase());
    assertThat(savedTemplateFolder.getPathId()).isNotEmpty();
    assertThat(savedTemplateFolder.getPathId().split("/")[0]).isEqualTo(parentFolder.getUuid());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldNotMoveFolderFromApplicationToAccount() {
    TemplateFolder myTemplateFolder = createAppLevelTemplateFolder();
    myTemplateFolder.setAppId(GLOBAL_APP_ID);
    templateFolderService.update(myTemplateFolder);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetAccountTemplateTreeUsingGlobalAppId() {
    TemplateGallery templateGallery = templateGalleryService.get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    templateService.loadDefaultTemplates(SSH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    templateFolderService.copyHarnessTemplateFolders(templateGallery.getUuid(), ACCOUNT_ID, HARNESS_GALLERY);

    TemplateFolder templateFolder = templateFolderService.getTemplateTree(ACCOUNT_ID, GLOBAL_APP_ID, null, null);
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getAccountId()).isNotNull().isEqualTo(ACCOUNT_ID);
    assertThat(templateFolder).extracting(TemplateFolder::getName).isEqualTo(HARNESS_GALLERY);
    assertThat(templateFolder.getChildren())
        .isNotEmpty()
        .extracting(templateFolder1 -> templateFolder1.getName().contains(TOMCAT_COMMANDS));
    assertThat(templateFolder.getChildren())
        .isNotEmpty()
        .extracting(templateFolder1 -> templateFolder1.getName().contains(LOAD_BALANCERS));
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetAppTemplateTree() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    templateFolderService.save(constructTemplateBuilder(GLOBAL_FOLDER, parentFolder.getUuid(), GLOBAL_APP_ID));
    templateFolderService.save(constructTemplateBuilder(parentFolder.getUuid(), APP_ID));
    templateFolderService.save(constructTemplateBuilder(TEMPLATE_FOLDER_NAME_2, parentFolder.getUuid(), APP_ID));
    TemplateFolder templateFolder = templateFolderService.getTemplateTree(GLOBAL_ACCOUNT_ID, APP_ID, null, null);
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getAccountId()).isNotNull().isEqualTo(GLOBAL_ACCOUNT_ID);
    assertThat(templateFolder).extracting(TemplateFolder::getName).isEqualTo(HARNESS_GALLERY);
    assertThat(templateFolder.getChildren())
        .isNotEmpty()
        .extracting(templateFolder1 -> templateFolder1.getName().contains(TEMPLATE_FOLDER_NAME));
    assertThat(templateFolder.getChildren())
        .isNotEmpty()
        .extracting(templateFolder1 -> templateFolder1.getName().contains(TEMPLATE_FOLDER_NAME_2));
    assertThat(templateFolder.getChildren())
        .isNotEmpty()
        .extracting(TemplateFolder::getName)
        .doesNotContain(GLOBAL_FOLDER);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldGetAppTemplateTreeByKeyword() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    templateFolderService.save(constructTemplateBuilder(GLOBAL_FOLDER, parentFolder.getUuid(), GLOBAL_APP_ID));
    templateFolderService.save(TemplateFolder.builder()
                                   .name(TEMPLATE_FOLDER_NAME)
                                   .description(TEMPLATE_DESC_CHANGED)
                                   .parentId(parentFolder.getUuid())
                                   .appId(APP_ID)
                                   .accountId(GLOBAL_ACCOUNT_ID)
                                   .build());
    templateFolderService.save(constructTemplateBuilder(TEMPLATE_FOLDER_NAME_2, parentFolder.getUuid(), APP_ID));

    TemplateFolder templateFolder = templateFolderService.getTemplateTree(GLOBAL_ACCOUNT_ID, "super", null);
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getChildren()).isNotEmpty();
    assertThat(templateFolder).extracting(TemplateFolder::getName).isEqualTo(HARNESS_GALLERY);
    assertThat(templateFolder.getChildren().size()).isEqualTo(1);
    assertThat(templateFolder.getChildren().get(0)).extracting(TemplateFolder::getName).isEqualTo(TEMPLATE_FOLDER_NAME);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldGetAppLevelFolderByPath() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder accountLevelFolder =
        templateFolderService.save(constructTemplateBuilder(parentFolder.getUuid(), GLOBAL_APP_ID));
    TemplateFolder appLevelFolder =
        templateFolderService.save(constructTemplateBuilder(parentFolder.getUuid(), APP_ID));
    templateFolderService.save(constructTemplateBuilder(TEMPLATE_FOLDER_NAME_2, parentFolder.getUuid(), APP_ID));
    TemplateFolder templateFolder =
        templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY + "/" + TEMPLATE_FOLDER_NAME);

    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(templateFolder.getKeywords()).contains(TEMPLATE_FOLDER_NAME.toLowerCase());
    assertThat(templateFolder.getAccountId()).isNotNull().isEqualTo(GLOBAL_ACCOUNT_ID);
    assertThat(templateFolder.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(templateFolder.getPathId()).isNotNull();
    assertThat(templateFolder.getUuid()).isEqualTo(accountLevelFolder.getUuid());

    templateFolder =
        templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, APP_ID, HARNESS_GALLERY + "/" + TEMPLATE_FOLDER_NAME);

    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(templateFolder.getKeywords()).contains(TEMPLATE_FOLDER_NAME.toLowerCase());
    assertThat(templateFolder.getAccountId()).isNotNull().isEqualTo(GLOBAL_ACCOUNT_ID);
    assertThat(templateFolder.getAppId()).isNotNull().isEqualTo(APP_ID);
    assertThat(templateFolder.getPathId()).isNotNull();
    assertThat(templateFolder.getUuid()).isEqualTo(appLevelFolder.getUuid());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void shouldDeleteTemplateFolderAndGenerateYamlPush() {
    YamlGitConfig yamlGitConfig = YamlGitConfig.builder()
                                      .entityType(EntityType.ACCOUNT)
                                      .entityId(GLOBAL_ACCOUNT_ID)
                                      .accountId(GLOBAL_ACCOUNT_ID)
                                      .enabled(true)
                                      .syncMode(YamlGitConfig.SyncMode.BOTH)
                                      .build();
    wingsPersistence.save(yamlGitConfig);
    featureFlagService.enableAccount(FeatureName.TEMPLATE_YAML_SUPPORT, GLOBAL_ACCOUNT_ID);
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder myTemplateFolder = templateFolderService.save(constructTemplateBuilder(parentFolder.getUuid()));
    TemplateFolder childFolder = templateFolderService.save(
        constructTemplateBuilder("Child Folder", myTemplateFolder.getUuid(), myTemplateFolder.getAppId()));
    templateService.save(Template.builder()
                             .folderId(childFolder.getUuid())
                             .accountId(GLOBAL_ACCOUNT_ID)
                             .appId(GLOBAL_APP_ID)
                             .type(TemplateConstants.SSH)
                             .templateObject(SshCommandTemplate.builder().build())
                             .version(1)
                             .name(TEMPLATE_NAME)
                             .build());
    templateFolderService.delete(childFolder.getUuid());
    assertThat(wingsPersistence.createQuery(YamlChangeSet.class).get().getGitFileChanges().get(0).getFilePath())
        .isEqualTo("Setup/Template Library/" + parentFolder.getName() + PATH_DELIMITER + myTemplateFolder.getName()
            + PATH_DELIMITER + childFolder.getName() + PATH_DELIMITER + TEMPLATE_NAME + ".yaml");
  }
}
