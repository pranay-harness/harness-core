package migrations.seedata;

import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static org.slf4j.LoggerFactory.getLogger;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.common.TemplateConstants.POWER_SHELL_IIS_APP_V5_INSTALL_PATH;
import static software.wings.common.TemplateConstants.POWER_SHELL_IIS_V6_INSTALL_PATH;
import static software.wings.common.TemplateConstants.POWER_SHELL_IIS_WEBSITE_V5_INSTALL_PATH;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import migrations.SeedDataMigration;
import org.slf4j.Logger;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateGallery;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;

import java.io.IOException;

public class IISInstallCommandV5Migration implements SeedDataMigration {
  private static final Logger logger = getLogger(IISInstallCommandV5Migration.class);
  @Inject private TemplateService templateService;
  @Inject private TemplateFolderService templateFolderService;
  @Inject private TemplateGalleryService templateGalleryService;

  @Override
  public void migrate() {
    try {
      logger.info("Migrating Install Command for IIS to V6");
      updateExistingInstallCommand(POWER_SHELL_IIS_V6_INSTALL_PATH, "iis");

      logger.info("Migrating Install Website Command for IIS to V5");
      updateExistingInstallCommand(POWER_SHELL_IIS_WEBSITE_V5_INSTALL_PATH, "iiswebsite");

      logger.info("Migrating Install App Command for IIS to V5");
      updateExistingInstallCommand(POWER_SHELL_IIS_APP_V5_INSTALL_PATH, "iisapp");
    } catch (WingsException e) {
      ExceptionLogger.logProcessedMessages(e, MANAGER, logger);
      logger.error("Migration failed: ", e);
    } catch (Exception e) {
      logger.error("Migration failed: ", e);
    }
  }

  public void updateExistingInstallCommand(String commandType, String keyword) throws IOException {
    TemplateGallery harnessTemplateGallery = templateGalleryService.get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    if (harnessTemplateGallery == null) {
      logger.info("Harness global gallery does not exist. Not copying templates.");
      return;
    }

    Template globalTemplate = templateService.convertYamlToTemplate(commandType);
    globalTemplate.setAccountId(GLOBAL_ACCOUNT_ID);
    globalTemplate.setAppId(GLOBAL_APP_ID);
    logger.info("Folder path for global account id: {}", globalTemplate.getFolderPath());
    TemplateFolder destTemplateFolder = templateFolderService.getByFolderPath(
        GLOBAL_ACCOUNT_ID, globalTemplate.getFolderPath(), harnessTemplateGallery.getUuid());
    if (destTemplateFolder != null) {
      logger.info("Template folder found for global account");

      // Get existing template
      Template existingTemplate = templateService.fetchTemplateByKeywordForAccountGallery(GLOBAL_ACCOUNT_ID, keyword);
      if (existingTemplate != null) {
        logger.info("IIS Install template found in Global account");
        globalTemplate.setUuid(existingTemplate.getUuid());
        globalTemplate.setGalleryId(harnessTemplateGallery.getUuid());
        globalTemplate.setVersion(null);
        globalTemplate.setFolderId(existingTemplate.getFolderId());
        globalTemplate = templateService.update(globalTemplate);
        logger.info("Global IIS Install template updated in account [{}]", GLOBAL_ACCOUNT_ID);
        templateGalleryService.copyNewVersionFromGlobalToAllAccounts(globalTemplate, keyword);
      } else {
        logger.error("IIS Install template not found in Global account");
      }
    } else {
      logger.error("Template folder doesn't exist for account " + GLOBAL_ACCOUNT_ID);
    }
  }
}
