package software.wings.service.impl;

import static io.harness.persistence.HQuery.excludeAuthority;
import static java.time.Duration.ofMinutes;
import static java.util.Arrays.asList;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.ID_KEY;
import static software.wings.beans.Schema.SCHEMA_ID;
import static software.wings.beans.Schema.SchemaBuilder.aSchema;
import static software.wings.dl.MongoHelper.setUnset;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Injector;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.eraro.ErrorCode;
import io.harness.persistence.HIterator;
import migrations.Migration;
import migrations.MigrationBackgroundList;
import migrations.MigrationList;
import migrations.SeedDataMigration;
import migrations.SeedDataMigrationList;
import org.apache.commons.lang3.tuple.Pair;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.DelegateConfiguration;
import software.wings.beans.Schema;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;
import software.wings.service.intfc.MigrationService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.utils.Misc;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MigrationServiceImpl implements MigrationService {
  private static final Logger logger = LoggerFactory.getLogger(MigrationServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private PersistentLocker persistentLocker;
  @Inject private Injector injector;
  @Inject private YamlGitService yamlGitService;

  @Inject private ExecutorService executorService;
  @Inject private TimeLimiter timeLimiter;

  @SuppressFBWarnings("REC_CATCH_EXCEPTION")
  @Override
  public void runMigrations() {
    Map<Integer, Class<? extends Migration>> migrations =
        MigrationList.getMigrations().stream().collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    int maxVersion = migrations.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);

    Map<Integer, Class<? extends Migration>> backgroundMigrations =
        MigrationBackgroundList.getMigrations().stream().collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    int maxBackgroundVersion = backgroundMigrations.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);

    Map<Integer, Class<? extends SeedDataMigration>> seedDataMigrations =
        SeedDataMigrationList.getMigrations().stream().collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    int maxSeedDataVersion = seedDataMigrations.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);

    try (
        AcquiredLock lock = persistentLocker.waitToAcquireLock(Schema.class, SCHEMA_ID, ofMinutes(25), ofMinutes(27))) {
      if (lock == null) {
        throw new WingsException(ErrorCode.GENERAL_ERROR)
            .addParam("message", "The persistent lock was not acquired. That very unlikely, but yet it happened.");
      }

      logger.info("[Migration] - Initializing Global DB Entries");
      initializeGlobalDbEntriesIfNeeded();

      logger.info("[Migration] - Checking for new migrations");
      Schema schema = wingsPersistence.createQuery(Schema.class).get();

      if (schema == null) {
        schema = aSchema()
                     .withVersion(maxVersion)
                     .withBackgroundVersion(maxBackgroundVersion)
                     .withSeedDataVersion(0)
                     .build();
        wingsPersistence.save(schema);
      }

      int currentBackgroundVersion = schema.getBackgroundVersion();
      if (currentBackgroundVersion < maxBackgroundVersion) {
        executorService.submit(() -> {
          try (AcquiredLock ignore =
                   persistentLocker.acquireLock(Schema.class, "Background-" + SCHEMA_ID, ofMinutes(120 + 1))) {
            timeLimiter.<Boolean>callWithTimeout(() -> {
              logger.info("[Migration] - Updating schema background version from {} to {}", currentBackgroundVersion,
                  maxBackgroundVersion);

              for (int i = currentBackgroundVersion + 1; i <= maxBackgroundVersion; i++) {
                if (!backgroundMigrations.containsKey(i)) {
                  continue;
                }
                Class<? extends Migration> migration = backgroundMigrations.get(i);
                logger.info("[Migration] - Migrating to background version {}: {} ...", i, migration.getSimpleName());
                injector.getInstance(migration).migrate();

                final UpdateOperations<Schema> updateOperations = wingsPersistence.createUpdateOperations(Schema.class);
                updateOperations.set(Schema.BACKGROUND_VERSION_KEY, i);
                wingsPersistence.update(wingsPersistence.createQuery(Schema.class), updateOperations);
              }

              logger.info("[Migration] - Background migration complete");
              return true;
            }, 2, TimeUnit.HOURS, true);
          } catch (Exception ex) {
            logger.warn("background work", ex);
          }
        });
      } else if (currentBackgroundVersion > maxBackgroundVersion) {
        // If the current version is bigger than the max version we are downgrading. Restore to the previous version
        logger.info("[Migration] - Rolling back schema background version from {} to {}", currentBackgroundVersion,
            maxBackgroundVersion);
        final UpdateOperations<Schema> updateOperations = wingsPersistence.createUpdateOperations(Schema.class);
        updateOperations.set(Schema.BACKGROUND_VERSION_KEY, maxBackgroundVersion);
        wingsPersistence.update(wingsPersistence.createQuery(Schema.class), updateOperations);
      } else {
        logger.info("[Migration] - Schema background is up to date");
      }

      if (schema.getVersion() < maxVersion) {
        logger.info("[Migration] - Updating schema version from {} to {}", schema.getVersion(), maxVersion);
        for (int i = schema.getVersion() + 1; i <= maxVersion; i++) {
          if (migrations.containsKey(i)) {
            Class<? extends Migration> migration = migrations.get(i);
            logger.info("[Migration] - Migrating to version {}: {} ...", i, migration.getSimpleName());
            injector.getInstance(migration).migrate();

            final UpdateOperations<Schema> updateOperations = wingsPersistence.createUpdateOperations(Schema.class);
            updateOperations.set(Schema.VERSION_KEY, i);
            wingsPersistence.update(wingsPersistence.createQuery(Schema.class), updateOperations);
          }
        }

        logger.info("[Migration] - Migration complete");

        executorService.submit(() -> {
          logger.info("Running Git full sync on all the accounts");

          try (
              HIterator<Account> accounts = new HIterator<>(
                  wingsPersistence.createQuery(Account.class, excludeAuthority).project("accountName", true).fetch())) {
            while (accounts.hasNext()) {
              Account account = accounts.next();
              try {
                yamlGitService.fullSync(account.getUuid(), false);
              } catch (Exception ex) {
                logger.error("Git full sync failed for account: {}. Reason is: {}", account.getAccountName(),
                    Misc.getMessage(ex));
              }
            }
          }
          logger.info("Git full sync on all the accounts completed");
        });
      } else if (schema.getVersion() > maxVersion) {
        // If the current version is bigger than the max version we are downgrading. Restore to the previous version
        logger.info("[Migration] - Rolling back schema version from {} to {}", schema.getVersion(), maxVersion);
        final UpdateOperations<Schema> updateOperations = wingsPersistence.createUpdateOperations(Schema.class);
        updateOperations.set(Schema.VERSION_KEY, maxVersion);
        wingsPersistence.update(wingsPersistence.createQuery(Schema.class), updateOperations);
      } else {
        logger.info("[Migration] - Schema is up to date");
      }

      /*
       * We run the seed data migration only once, so even in case of a rollback, we do not reset the seedDataVersion to
       * the previous version This has been done on purpose to simplify the migrations, where the migrations do not have
       * any special logic to check if they have already been run or not.
       */
      if (schema.getSeedDataVersion() < maxSeedDataVersion) {
        logger.info("[SeedDataMigration] - Updating schema version from {} to {}", schema.getSeedDataVersion(),
            maxSeedDataVersion);
        for (int i = schema.getSeedDataVersion() + 1; i <= maxSeedDataVersion; i++) {
          if (seedDataMigrations.containsKey(i)) {
            Class<? extends SeedDataMigration> seedDataMigration = seedDataMigrations.get(i);
            logger.info("[SeedDataMigration] - Migrating to version {}: {} ...", i, seedDataMigration.getSimpleName());
            injector.getInstance(seedDataMigration).migrate();

            final UpdateOperations<Schema> updateOperations = wingsPersistence.createUpdateOperations(Schema.class);
            updateOperations.set(Schema.SEED_DATA_VERSION_KEY, i);
            wingsPersistence.update(wingsPersistence.createQuery(Schema.class), updateOperations);
          }
        }
      } else {
        logger.info("[SeedDataMigration] - Schema is up to date");
      }

    } catch (Exception e) {
      logger.error("[Migration] - Migration failed.", e);
    }
  }

  private void initializeGlobalDbEntriesIfNeeded() {
    try {
      Query<Account> globalAccountQuery = wingsPersistence.createQuery(Account.class).filter(ID_KEY, GLOBAL_ACCOUNT_ID);
      Account globalAccount = globalAccountQuery.get();
      if (globalAccount == null) {
        wingsPersistence.save(Account.Builder.anAccount()
                                  .withUuid(GLOBAL_ACCOUNT_ID)
                                  .withCompanyName("Global")
                                  .withAccountName("Global")
                                  .withDelegateConfiguration(DelegateConfiguration.builder()
                                                                 .watcherVersion("1.0.0-dev")
                                                                 .delegateVersions(asList("1.0.0-dev"))
                                                                 .build())
                                  .build());
      } else if (globalAccount.getDelegateConfiguration() == null) {
        UpdateOperations<Account> ops = wingsPersistence.createUpdateOperations(Account.class);
        setUnset(ops, "delegateConfiguration",
            DelegateConfiguration.builder().watcherVersion("1.0.0-dev").delegateVersions(asList("1.0.0-dev")).build());
        wingsPersistence.update(globalAccountQuery, ops);
      }
    } catch (Exception e) {
      logger.error("[Migration] - initializeGlobalDbEntriesIfNeeded failed.", e);
    }
  }
}
