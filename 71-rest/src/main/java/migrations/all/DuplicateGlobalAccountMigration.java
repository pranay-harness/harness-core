package migrations.all;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.service.intfc.template.TemplateGalleryService;

@Slf4j
public class DuplicateGlobalAccountMigration implements Migration {
  @Inject private TemplateGalleryService templateGalleryService;
  @Override
  public void migrate() {
    logger.info("Deleting template gallery for Account Name: Global");
    templateGalleryService.deleteAccountGalleryByName(GLOBAL_ACCOUNT_ID, "Global");
    logger.info("Finished deleting template gallery for Account Name: Global");
  }
}
