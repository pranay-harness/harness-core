package migrations.seedata;

import static io.harness.exception.WingsException.ExecutionContext.MANAGER;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import lombok.extern.slf4j.Slf4j;
import migrations.SeedDataMigration;
import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.template.TemplateGalleryService;

@Slf4j
public class ReImportTemplatesMigration implements SeedDataMigration {
  private static final String ACCOUNT_ID = "-czOfo4UTPumhprgLZkDYg"; // Bitcentral Inc.
  @Inject private TemplateGalleryService templateGalleryService;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      Account account = wingsPersistence.get(Account.class, ACCOUNT_ID);
      if (account == null) {
        log.info("Specified account not found. Not copying templates.");
        return;
      }
      templateGalleryService.copyHarnessTemplatesToAccountV2(account.getUuid(), account.getAccountName());
    } catch (WingsException e) {
      ExceptionLogger.logProcessedMessages(e, MANAGER, log);
      log.error("Migration failed: ", e);
    } catch (Exception e) {
      log.error("Migration failed: ", e);
    }
  }
}
