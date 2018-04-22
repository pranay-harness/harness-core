package software.wings.service.impl;

import static java.time.Duration.ofMinutes;
import static software.wings.beans.Schema.SCHEMA_ID;
import static software.wings.beans.Schema.SchemaBuilder.aSchema;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.inject.Inject;
import com.google.inject.Injector;

import migrations.Migration;
import migrations.MigrationList;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.ErrorCode;
import software.wings.beans.Schema;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.MigrationService;
import software.wings.service.intfc.yaml.YamlGitService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MigrationServiceImpl implements MigrationService {
  private static final Logger logger = LoggerFactory.getLogger(MigrationServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private PersistentLocker persistentLocker;
  @Inject private Injector injector;
  @Inject private YamlGitService yamlGitService;
  @Inject private AccountService accountService;

  @Override
  public void runMigrations() {
    Map<Integer, Class<? extends Migration>> migrations =
        MigrationList.getMigrations().stream().collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    int maxVersion = migrations.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);

    try (
        AcquiredLock lock = persistentLocker.waitToAcquireLock(Schema.class, SCHEMA_ID, ofMinutes(15), ofMinutes(20))) {
      if (lock == null) {
        throw new WingsException(ErrorCode.GENERAL_ERROR)
            .addParam("message", "The persistent lock was not acquired. That very unlikely, but yet it happened.");
      }

      logger.info("[Migration] - Checking for new migrations");
      Schema schema = wingsPersistence.createQuery(Schema.class).get();

      if (schema == null) {
        schema = aSchema().withVersion(maxVersion).build();
        wingsPersistence.save(schema);
      }

      if (schema.getVersion() < maxVersion) {
        logger.info("[Migration] - Updating schema version from {} to {}", schema.getVersion(), maxVersion);
        for (int i = schema.getVersion() + 1; i <= maxVersion; i++) {
          if (migrations.containsKey(i)) {
            logger.info("[Migration] - Migrating to version {}...", i);
            injector.getInstance(migrations.get(i)).migrate();
            schema.setVersion(i);
            wingsPersistence.save(schema);
          }
        }

        logger.info("[Migration] - Migration complete");

        logger.info("Running Git full sync on all the accounts");

        PageRequest<Account> pageRequest = aPageRequest().addFieldsIncluded("_id", "accountName").build();
        List<Account> accountList = accountService.list(pageRequest);
        for (Account account : accountList) {
          try {
            yamlGitService.fullSync(account.getUuid(), false);
          } catch (Exception ex) {
            logger.error(
                "Git full sync failed for account: {}. Reason is: {}", account.getAccountName(), ex.getMessage());
          }
        }

        logger.info("Git full sync on all the accounts completed");

      } else {
        logger.info("[Migration] - Schema is up to date");
      }
    } catch (Exception e) {
      logger.error("[Migration] - Migration failed.", e);
    }
  }
}
