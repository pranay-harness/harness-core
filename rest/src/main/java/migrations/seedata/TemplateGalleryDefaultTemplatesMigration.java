package migrations.seedata;

import static java.lang.String.format;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;

import com.google.inject.Inject;

import migrations.SeedDataMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.template.TemplateGallery;
import software.wings.dl.HIterator;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.template.TemplateGalleryService;

/**
 * Created by anubhaw on 8/20/18.
 */
public class TemplateGalleryDefaultTemplatesMigration implements SeedDataMigration {
  private static final Logger logger = LoggerFactory.getLogger(TemplateGalleryDefaultTemplatesMigration.class);
  @Inject private TemplateGalleryService templateGalleryService;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      boolean rootTemplateGalleryDoesNotExist = wingsPersistence.createQuery(TemplateGallery.class)
                                                    .field(TemplateGallery.NAME_KEY)
                                                    .equal(HARNESS_GALLERY)
                                                    .field("accountId")
                                                    .equal(GLOBAL_ACCOUNT_ID)
                                                    .getKey()
          == null;
      if (rootTemplateGalleryDoesNotExist) {
        logger.error("TemplateGalleryDefaultTemplatesMigration root template gallery not found");
        templateGalleryService.loadHarnessGallery(); // takes care of copying templates to individual accounts
        templateGalleryService.copyHarnessTemplates();
      } else {
        // in case previous migration failed while copying Harness templates
        copyHarnessTemplateToAccounts();
      }
    } catch (Exception ex) {
      logger.error("TemplateGalleryDefaultTemplatesMigration failed", ex);
    }
  }

  private void copyHarnessTemplateToAccounts() {
    try (HIterator<Account> records = new HIterator<>(wingsPersistence.createQuery(Account.class).fetch())) {
      while (records.hasNext()) {
        Account account = records.next();
        try {
          logger.info("TemplateGalleryDefaultTemplatesMigration started for account [{}]", account.getUuid());
          boolean templateGalleryDoesNotExist = wingsPersistence.createQuery(TemplateGallery.class)
                                                    .field(TemplateGallery.NAME_KEY)
                                                    .equal(account.getAccountName())
                                                    .field("accountId")
                                                    .equal(account.getUuid())
                                                    .getKey()
              == null;
          if (templateGalleryDoesNotExist) {
            if (!GLOBAL_ACCOUNT_ID.equals(account.getUuid())) {
              templateGalleryService.copyHarnessTemplatesToAccount(account.getUuid(), account.getAccountName());
            }
          } else {
            logger.info("TemplateGalleryDefaultTemplatesMigration gallery already exists for account [{}]. do nothing",
                account.getUuid());
          }
          logger.info("TemplateGalleryDefaultTemplatesMigration finished for account [{}]", account.getUuid());
        } catch (Exception ex) {
          logger.error(
              format("TemplateGalleryDefaultTemplatesMigration failed for account [%s]", account.getUuid()), ex);
        }
      }
    }
  }
}
