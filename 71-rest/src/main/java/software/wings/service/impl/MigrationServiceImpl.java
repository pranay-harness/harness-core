package software.wings.service.impl;

import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.time.Duration.ofMinutes;
import static java.util.Arrays.asList;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.ID_KEY;
import static software.wings.beans.Schema.SCHEMA_ID;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import migrations.MigrationBackgroundList;
import migrations.MigrationList;
import migrations.SeedDataMigration;
import migrations.SeedDataMigrationList;
import migrations.TimeScaleDBDataMigration;
import migrations.TimeScaleDBMigration;
import migrations.TimescaleDBDataMigrationList;
import migrations.TimescaleDBMigrationList;
import org.apache.commons.lang3.tuple.Pair;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Account;
import software.wings.beans.Schema;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.MigrationService;
import software.wings.service.intfc.yaml.YamlGitService;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class MigrationServiceImpl implements MigrationService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private PersistentLocker persistentLocker;
  @Inject private Injector injector;
  @Inject private YamlGitService yamlGitService;

  @Inject private ExecutorService executorService;
  @Inject private TimeLimiter timeLimiter;

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

    Map<Integer, Class<? extends TimeScaleDBMigration>> timescaleDBMigrations =
        TimescaleDBMigrationList.getMigrations().stream().collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    int maxTimeScaleDBMigration = timescaleDBMigrations.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);

    Map<Integer, Class<? extends TimeScaleDBDataMigration>> backgroundTimeScaleDBDataMigrations =
        TimescaleDBDataMigrationList.getMigrations().stream().collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    int maxTimeScaleDBDataMigration =
        backgroundTimeScaleDBDataMigrations.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);

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
        schema = Schema.builder()
                     .version(maxVersion)
                     .backgroundVersion(maxBackgroundVersion)
                     .seedDataVersion(0)
                     .timescaleDbVersion(0)
                     .timescaleDBDataVersion(0)
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
                try {
                  injector.getInstance(migration).migrate();
                } catch (Exception ex) {
                  logger.error("Error while running migration {}", migration.getSimpleName(), ex);
                  break;
                }

                final UpdateOperations<Schema> updateOperations = wingsPersistence.createUpdateOperations(Schema.class);
                updateOperations.set(Schema.BACKGROUND_VERSION, i);
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
        updateOperations.set(Schema.BACKGROUND_VERSION, maxBackgroundVersion);
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
            updateOperations.set(Schema.VERSION, i);
            wingsPersistence.update(wingsPersistence.createQuery(Schema.class), updateOperations);
          }
        }

        logger.info("[Migration] - Migration complete");

        executorService.submit(() -> {
          logger.info("Running Git full sync on all the accounts");

          try (
              HIterator<Account> accounts = new HIterator<>(
                  wingsPersistence.createQuery(Account.class, excludeAuthority).project("accountName", true).fetch())) {
            for (Account account : accounts) {
              try {
                yamlGitService.fullSyncForEntireAccount(account.getUuid());
              } catch (Exception ex) {
                logger.error("Git full sync failed for account: {}. Reason is: {}", account.getAccountName(),
                    ExceptionUtils.getMessage(ex));
              }
            }
          }
          logger.info("Git full sync on all the accounts completed");
        });
      } else if (schema.getVersion() > maxVersion) {
        // If the current version is bigger than the max version we are downgrading. Restore to the previous version
        logger.info("[Migration] - Rolling back schema version from {} to {}", schema.getVersion(), maxVersion);
        final UpdateOperations<Schema> updateOperations = wingsPersistence.createUpdateOperations(Schema.class);
        updateOperations.set(Schema.VERSION, maxVersion);
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
            updateOperations.set(Schema.SEED_DATA_VERSION, i);
            wingsPersistence.update(wingsPersistence.createQuery(Schema.class), updateOperations);
          }
        }
      } else {
        logger.info("[SeedDataMigration] - Schema is up to date");
      }

      if (schema.getTimescaleDbVersion() < maxTimeScaleDBMigration) {
        logger.info("[TimescaleDBMigration] - Forward migrating schema version from {} to {}",
            schema.getTimescaleDbVersion(), maxTimeScaleDBMigration, maxSeedDataVersion);
        for (int i = schema.getTimescaleDbVersion() + 1; i <= maxTimeScaleDBMigration; i++) {
          if (timescaleDBMigrations.containsKey(i)) {
            Class<? extends TimeScaleDBMigration> timescaleDBMigration = timescaleDBMigrations.get(i);
            logger.info("[TimescaleDBMigration] - Forward Migrating to version {}: {} ...", i,
                timescaleDBMigration.getSimpleName());
            boolean result = injector.getInstance(timescaleDBMigration).migrate();
            if (!result) {
              logger.info("Forward TimescaleDBMigration did not run successfully");
            } else {
              final UpdateOperations<Schema> updateOperations = wingsPersistence.createUpdateOperations(Schema.class);
              updateOperations.set(Schema.TIMESCALEDB_VERSION, i);
              wingsPersistence.update(wingsPersistence.createQuery(Schema.class), updateOperations);
            }
          }
        }
      } else {
        logger.info(
            "No TimescaleDB migration required, schema version is valid : [{}]", schema.getTimescaleDbVersion());
      }

      int currentTimeScaleDBDataMigration = schema.getTimescaleDBDataVersion();
      if (currentTimeScaleDBDataMigration < maxTimeScaleDBDataMigration) {
        executorService.submit(() -> {
          try (AcquiredLock ignore =
                   persistentLocker.acquireLock(Schema.class, "Background-" + SCHEMA_ID, ofMinutes(120 + 1))) {
            timeLimiter.<Boolean>callWithTimeout(() -> {
              logger.info("[TimeScaleDBDataMigration] - Updating schema background version from {} to {}",
                  currentTimeScaleDBDataMigration, maxTimeScaleDBDataMigration);

              boolean successfulMigration = true;

              for (int i = currentTimeScaleDBDataMigration + 1; i <= maxTimeScaleDBDataMigration; i++) {
                if (!backgroundTimeScaleDBDataMigrations.containsKey(i)) {
                  continue;
                }
                Class<? extends TimeScaleDBDataMigration> migration = backgroundTimeScaleDBDataMigrations.get(i);
                logger.info("[TimeScaleDBDataMigration] - Migrating to background version {}: {} ...", i,
                    migration.getSimpleName());
                try {
                  successfulMigration = successfulMigration && injector.getInstance(migration).migrate();
                } catch (Exception ex) {
                  logger.error("Error while running timeScaleDBDataMigration {}", migration.getSimpleName(), ex);
                  break;
                }
                if (successfulMigration) {
                  final UpdateOperations<Schema> updateOperations =
                      wingsPersistence.createUpdateOperations(Schema.class);
                  updateOperations.set(Schema.TIMESCALEDB_DATA_VERSION, i);
                  wingsPersistence.update(wingsPersistence.createQuery(Schema.class), updateOperations);
                } else {
                  break;
                }
              }
              if (successfulMigration) {
                logger.info("TimeScaleDB migration was successfully completed");
              } else {
                logger.info("TimeScaleDB migration was not successful ");
              }
              return true;
            }, 2, TimeUnit.HOURS, true);
          } catch (Exception ex) {
            logger.warn("background work", ex);
          }
        });
      } else {
        logger.info("No TimescaleDB Data migration required, schema version is valid : [{}]",
            schema.getTimescaleDBDataVersion());
      }

    } catch (RuntimeException e) {
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
