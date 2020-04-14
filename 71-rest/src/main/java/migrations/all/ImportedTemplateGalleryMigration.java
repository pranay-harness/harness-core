package migrations.all;

import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.template.TemplateGallery.GALLERY_KEY;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.beans.Account;
import software.wings.beans.template.TemplateGallery;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.template.TemplateGalleryService;

@Slf4j
public class ImportedTemplateGalleryMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject TemplateGalleryService templateGalleryService;
  private static final String DEBUG_LINE = "GLOBAL_COMMAND_GALLERY: ";

  @Override
  public void migrate() {
    logger.info(DEBUG_LINE + "Starting migration for adding field in template gallery");

    try (HIterator<Account> accounts = new HIterator<>(wingsPersistence.createAuthorizedQuery(Account.class).fetch())) {
      while (accounts.hasNext()) {
        Account account = accounts.next();
        UpdateOperations<TemplateGallery> updateOperations =
            wingsPersistence.createUpdateOperations(TemplateGallery.class)
                .set(GALLERY_KEY, templateGalleryService.getAccountGalleryKey());

        Query<TemplateGallery> query = wingsPersistence.createQuery(TemplateGallery.class)
                                           .filter(ACCOUNT_ID_KEY, account.getUuid())
                                           .field(GALLERY_KEY)
                                           .doesNotExist();
        UpdateResults result = wingsPersistence.update(query, updateOperations);
        logger.info("Updated account gallery to have gallery type for account %s ");
        logger.info(DEBUG_LINE + "Gallery type update for already existing gallery for account {} resulted in {}",
            account.getUuid(), result);
      }
    }
  }
}
