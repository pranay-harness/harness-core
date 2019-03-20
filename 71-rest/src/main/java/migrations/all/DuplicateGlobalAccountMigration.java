package migrations.all;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;

import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.intfc.template.TemplateGalleryService;

public class DuplicateGlobalAccountMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(DuplicateGlobalAccountMigration.class);

  @Inject private TemplateGalleryService templateGalleryService;
  @Override
  public void migrate() {
    logger.info("Deleting template gallery for Account Name: Global");
    templateGalleryService.deleteAccountGalleryByName(GLOBAL_ACCOUNT_ID, "Global");
    logger.info("Finished deleting template gallery for Account Name: Global");
  }
}
