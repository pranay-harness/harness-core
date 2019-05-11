package migrations.seedata;

import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static org.slf4j.LoggerFactory.getLogger;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import migrations.SeedDataMigration;
import org.slf4j.Logger;
import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.template.TemplateGalleryService;

public class ReImportTemplatesMigration implements SeedDataMigration {
  private static final Logger logger = getLogger(ReImportTemplatesMigration.class);
  private static final String ACCOUNT_ID = "Ke-E1FX2SO2ZAL2TXqpLjg";
  @Inject private TemplateGalleryService templateGalleryService;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      Account account = wingsPersistence.get(Account.class, ACCOUNT_ID);
      if (account == null) {
        logger.info("Specified account not found. Not copying templates.");
        return;
      }
      templateGalleryService.copyHarnessTemplatesToAccountV2(account.getUuid(), account.getAccountName());
    } catch (WingsException e) {
      ExceptionLogger.logProcessedMessages(e, MANAGER, logger);
      logger.error("Migration failed: ", e);
    } catch (Exception e) {
      logger.error("Migration failed: ", e);
    }
  }
}
