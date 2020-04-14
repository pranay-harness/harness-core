package migrations.all;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;
import software.wings.beans.template.TemplateGallery;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.template.TemplateGalleryService;

@Slf4j
public class AddHarnessCommandLibraryToAccount implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject TemplateGalleryService templateGalleryService;
  private static final String DEBUG_LINE = "HARNESS_GALLERY_ADDITION: ";

  @Override
  public void migrate() {
    logger.info(DEBUG_LINE + "Starting migration for adding new  template gallery");

    try (HIterator<Account> accounts = new HIterator<>(wingsPersistence.createAuthorizedQuery(Account.class).fetch())) {
      while (accounts.hasNext()) {
        Account account = accounts.next();
        if (wingsPersistence.createQuery(TemplateGallery.class)
                .filter(TemplateGallery.GALLERY_KEY, TemplateGallery.GalleryKey.HARNESS_COMMAND_LIBRARY_GALLERY)
                .filter(TemplateGallery.ACCOUNT_ID_KEY, account.getUuid())
                .get()
            == null) {
          try {
            templateGalleryService.saveHarnessCommandLibraryGalleryToAccount(
                account.getUuid(), account.getAccountName());
          } catch (Exception e) {
            logger.error(DEBUG_LINE + "Cannot add harness gallery to account" + account.getUuid(), e);
          }
        }
        logger.info(DEBUG_LINE + "Saved Harness Command library gallery to account {}", account.getUuid());
      }
    }
  }
}
