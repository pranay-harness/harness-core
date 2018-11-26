package migrations.seedata;

import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.common.TemplateConstants.POWER_SHELL_COMMANDS;
import static software.wings.common.TemplateConstants.POWER_SHELL_IIS_APP_INSTALL_PATH;
import static software.wings.common.TemplateConstants.POWER_SHELL_IIS_V2_INSTALL_PATH;
import static software.wings.common.TemplateConstants.POWER_SHELL_IIS_WEBSITE_INSTALL_PATH;

import com.google.inject.Inject;

import migrations.SeedDataMigration;
import org.slf4j.Logger;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.TemplateType;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;

import java.io.IOException;

public class IISInstallCommandMigration implements SeedDataMigration {
  private static final Logger logger = getLogger(IISInstallCommandMigration.class);
  private static final String INSTALL_IIS_APPLICATION_TEMPLATE_NAME = "Install IIS Application";
  private static final String INSTALL_IIS_WEBSITE_TEMPLATE_NAME = "Install IIS Website";
  @Inject private TemplateService templateService;
  @Inject private TemplateFolderService templateFolderService;
  @Inject private TemplateGalleryService templateGalleryService;

  @Override
  public void migrate() {
    logger.info("Migrating Install Command for IIS");
    loadNewIISTemplatesToAccounts();
    try {
      updateExistingInstallCommand();
    } catch (Exception e) {
      logger.error("Migration failed: ", e);
    }
  }

  public void loadNewIISTemplatesToAccounts() {
    templateService.loadDefaultTemplates(asList(POWER_SHELL_IIS_WEBSITE_INSTALL_PATH, POWER_SHELL_IIS_APP_INSTALL_PATH),
        GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);

    templateGalleryService.copyHarnessTemplateFromGalleryToAccounts(POWER_SHELL_COMMANDS, TemplateType.SSH,
        INSTALL_IIS_APPLICATION_TEMPLATE_NAME, POWER_SHELL_IIS_APP_INSTALL_PATH);
    templateGalleryService.copyHarnessTemplateFromGalleryToAccounts(POWER_SHELL_COMMANDS, TemplateType.SSH,
        INSTALL_IIS_WEBSITE_TEMPLATE_NAME, POWER_SHELL_IIS_WEBSITE_INSTALL_PATH);
  }

  public void updateExistingInstallCommand() throws IOException {
    TemplateGallery harnessTemplateGallery = templateGalleryService.get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    if (harnessTemplateGallery == null) {
      logger.info("Harness global gallery does not exist. Not copying templates");
      return;
    }
    Template globalTemplate = templateService.convertYamlToTemplate(POWER_SHELL_IIS_V2_INSTALL_PATH);
    globalTemplate.setAppId(GLOBAL_APP_ID);
    globalTemplate.setAccountId(GLOBAL_ACCOUNT_ID);
    TemplateFolder destTemplateFolder =
        templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, globalTemplate.getFolderPath());
    if (destTemplateFolder != null) {
      Template existingTemplate = templateService.fetchTemplateByKeyword(GLOBAL_ACCOUNT_ID, "iis");
      if (existingTemplate != null) {
        globalTemplate.setUuid(existingTemplate.getUuid());
        globalTemplate.setVersion(null);
        globalTemplate.setFolderId(existingTemplate.getFolderId());
        globalTemplate = templateService.update(globalTemplate);
        logger.info("Template updated in account [{}]", GLOBAL_ACCOUNT_ID);
        templateGalleryService.copyNewVersionFromGlobalToAllAccounts(globalTemplate, "iis");
      }
    }
  }
}
