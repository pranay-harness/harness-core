package software.wings.service.impl.template;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.template.TemplateType.SSH;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.common.TemplateConstants.JBOSS_COMMANDS;
import static software.wings.common.TemplateConstants.LOAD_BALANCERS;
import static software.wings.common.TemplateConstants.TOMCAT_COMMANDS;
import static software.wings.common.TemplateConstants.TOMCAT_WAR_INSTALL_PATH;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_DESC_CHANGED;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_FOLDER_DEC;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_FOLDER_NAME;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_GALLERY;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_GALLERY_DESC;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.INVALID_NAME;

import io.harness.exception.InvalidRequestException;
import org.junit.Test;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateGallery;

import java.util.Arrays;
import javax.validation.ConstraintViolationException;

public class TemplateFolderServiceTest extends TemplateBaseTest {
  @Test(expected = InvalidRequestException.class)
  public void shouldNotDeleteRootFolder() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    templateFolderService.delete(parentFolder.getUuid());
  }

  @Test
  public void shouldSaveTemplateFolder() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder myTemplateFolder = templateFolderService.save(constructTemplateBuilder(parentFolder.getUuid()));

    assertThat(myTemplateFolder).isNotNull();
    assertThat(myTemplateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(myTemplateFolder.getKeywords()).contains(TEMPLATE_FOLDER_NAME.toLowerCase());
    assertThat(myTemplateFolder.getKeywords()).contains(TEMPLATE_FOLDER_DEC.toLowerCase());
    assertThat(myTemplateFolder.getPathId()).isNotEmpty();
    assertThat(myTemplateFolder.getPathId().split("/")[0]).isEqualTo(parentFolder.getUuid());
  }

  @Test
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
  public void shouldLoadTemplateFolders() {
    TemplateFolder templateFolder = templateFolderService.getTemplateTree(GLOBAL_ACCOUNT_ID, null, null);
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getChildren()).isNotEmpty();
    assertThat(templateFolder).extracting(TemplateFolder::getName).contains(HARNESS_GALLERY);
  }

  @Test
  public void shouldGetGlobalTemplateTree() {
    TemplateFolder templateFolder = templateFolderService.getTemplateTree(GLOBAL_ACCOUNT_ID, null, null);
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder).extracting(TemplateFolder::getName).contains(HARNESS_GALLERY);
    assertThat(templateFolder.getChildren())
        .isNotEmpty()
        .extracting(templateFolder1 -> templateFolder1.getName().contains(TOMCAT_COMMANDS));
    assertThat(templateFolder.getChildren())
        .isNotEmpty()
        .extracting(templateFolder1 -> templateFolder1.getName().contains(JBOSS_COMMANDS));
  }

  @Test
  public void shouldGetGlobalTemplateRootTree() {
    TemplateFolder templateFolder = templateFolderService.getTemplateTree(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY, null);
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getChildren()).isNotEmpty();
    assertThat(templateFolder).extracting(TemplateFolder::getName).contains(HARNESS_GALLERY);
    assertThat(templateFolder.getChildren())
        .isNotEmpty()
        .extracting(templateFolder1 -> templateFolder1.getName())
        .contains(TOMCAT_COMMANDS);
    assertThat(templateFolder.getChildren())
        .isNotEmpty()
        .extracting(templateFolder1 -> templateFolder1.getName())
        .contains(JBOSS_COMMANDS);
  }

  @Test
  public void shouldGetGlobalTemplateTreeByKeyword() {
    TemplateFolder templateFolder = templateFolderService.getTemplateTree(GLOBAL_ACCOUNT_ID, TOMCAT_COMMANDS, null);
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getChildren()).isNotEmpty();
    assertThat(templateFolder).extracting(TemplateFolder::getName).contains(HARNESS_GALLERY);
    assertThat(templateFolder.getChildren()).isNotEmpty();

    assertThat(templateFolder.getChildren())
        .extracting(templateFolder1 -> templateFolder1.getName())
        .contains(TOMCAT_COMMANDS);
    assertThat(templateFolder.getChildren())
        .extracting(templateFolder1 -> templateFolder1.getName())
        .doesNotContain(LOAD_BALANCERS);
  }

  @Test
  public void shouldLoadDefaultCommandTemplates() {
    templateService.loadDefaultTemplates(SSH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder templateFolder =
        templateFolderService.getTemplateTree(GLOBAL_ACCOUNT_ID, null, Arrays.asList(SSH.name()));
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getChildren()).isNotEmpty();
    assertThat(templateFolder).extracting(TemplateFolder::getName).contains(HARNESS_GALLERY);
    assertThat(templateFolder.getChildren()).isNotEmpty();

    assertThat(templateFolder.getChildren())
        .extracting(templateFolder1 -> templateFolder1.getName())
        .contains(TOMCAT_COMMANDS);
    assertThat(templateFolder.getChildren())
        .extracting(templateFolder1 -> templateFolder1.getName())
        .doesNotContain(LOAD_BALANCERS);
  }

  @Test
  public void shouldGetGlobalTemplateTreeByKeywordAndTypes() {
    templateService.loadDefaultTemplates(SSH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder templateFolder =
        templateFolderService.getTemplateTree(GLOBAL_ACCOUNT_ID, "Install", Arrays.asList(SSH.name()));
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getAccountId()).isNotNull().isEqualTo(GLOBAL_ACCOUNT_ID);
    assertThat(templateFolder.getChildren()).isNotEmpty();
    assertThat(templateFolder).extracting(TemplateFolder::getName).contains(HARNESS_GALLERY);
    assertThat(templateFolder.getChildren()).isNotEmpty();

    assertThat(templateFolder.getChildren())
        .extracting(templateFolder1 -> templateFolder1.getName())
        .contains(TOMCAT_COMMANDS);
    assertThat(templateFolder.getChildren())
        .extracting(templateFolder1 -> templateFolder1.getName())
        .doesNotContain(LOAD_BALANCERS);
  }

  @Test
  public void shouldGetGlobalTemplateTreeByKeywordAndTypesNotMatching() {
    templateService.loadDefaultTemplates(SSH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder templateFolder =
        templateFolderService.getTemplateTree(GLOBAL_ACCOUNT_ID, "Install3", Arrays.asList(SSH.name()));
    assertThat(templateFolder).isNull();
  }

  @Test
  public void shouldCopyHarnessTemplateFolders() {
    TemplateGallery templateGallery = templateGalleryService.get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    templateService.loadDefaultTemplates(SSH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);

    templateFolderService.copyHarnessTemplateFolders(templateGallery.getUuid(), ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder templateFolder = templateFolderService.getTemplateTree(ACCOUNT_ID, null, null);

    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getAccountId()).isNotNull().isEqualTo(ACCOUNT_ID);
    assertThat(templateFolder).extracting(TemplateFolder::getName).contains(HARNESS_GALLERY);
    assertThat(templateFolder.getChildren())
        .isNotEmpty()
        .extracting(templateFolder1 -> templateFolder1.getName().contains(TOMCAT_COMMANDS));
    assertThat(templateFolder.getChildren())
        .isNotEmpty()
        .extracting(templateFolder1 -> templateFolder1.getName().contains(LOAD_BALANCERS));
  }

  @Test
  public void shouldGetAccountTemplateTree() {
    TemplateGallery templateGallery = templateGalleryService.get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    templateService.loadDefaultTemplates(SSH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    templateFolderService.copyHarnessTemplateFolders(templateGallery.getUuid(), ACCOUNT_ID, HARNESS_GALLERY);

    TemplateFolder templateFolder = templateFolderService.getTemplateTree(ACCOUNT_ID, null, null);
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getAccountId()).isNotNull().isEqualTo(ACCOUNT_ID);
    assertThat(templateFolder).extracting(TemplateFolder::getName).contains(HARNESS_GALLERY);
    assertThat(templateFolder.getChildren())
        .isNotEmpty()
        .extracting(templateFolder1 -> templateFolder1.getName().contains(TOMCAT_COMMANDS));
    assertThat(templateFolder.getChildren())
        .isNotEmpty()
        .extracting(templateFolder1 -> templateFolder1.getName().contains(LOAD_BALANCERS));
  }

  @Test
  public void shouldLoadTomcatStandardInstallCommand() {
    TemplateGallery templateGallery = templateGalleryService.get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    templateGalleryService.save(TemplateGallery.builder()
                                    .name(TEMPLATE_GALLERY)
                                    .accountId(ACCOUNT_ID)
                                    .description(TEMPLATE_GALLERY_DESC)
                                    .appId(GLOBAL_APP_ID)
                                    .keywords(asList("CD"))
                                    .build());
    templateService.loadDefaultTemplates(SSH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    templateFolderService.copyHarnessTemplateFolders(templateGallery.getUuid(), ACCOUNT_ID, TEMPLATE_GALLERY);
    templateService.loadYaml(SSH, TOMCAT_WAR_INSTALL_PATH, ACCOUNT_ID, TEMPLATE_GALLERY);
    TemplateFolder templateFolder =
        templateFolderService.getTemplateTree(ACCOUNT_ID, "Install", Arrays.asList(SSH.name()));
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getAccountId()).isNotNull().isEqualTo(ACCOUNT_ID);
    assertThat(templateFolder.getChildren()).isNotEmpty();
    assertThat(templateFolder).extracting(TemplateFolder::getName).contains(TEMPLATE_GALLERY);
    assertThat(templateFolder.getChildren()).isNotEmpty();

    assertThat(templateFolder.getChildren())
        .extracting(templateFolder1 -> templateFolder1.getName())
        .contains(TOMCAT_COMMANDS);

    assertThat(templateFolder.getChildren())
        .extracting(templateFolder1 -> templateFolder1.getName())
        .doesNotContain(LOAD_BALANCERS);
  }

  @Test
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
    return TemplateFolder.builder()
        .name(TEMPLATE_FOLDER_NAME)
        .description(TEMPLATE_FOLDER_DEC)
        .parentId(parentId)
        .appId(GLOBAL_APP_ID)
        .accountId(GLOBAL_ACCOUNT_ID)
        .build();
  }
}
