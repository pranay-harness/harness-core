package io.harness.migration.service.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.migration.entities.NGSchema.NG_SCHEMA_ID;

import static software.wings.beans.Schema.SCHEMA_ID;

import static java.time.Duration.ofMinutes;

import io.harness.Microservice;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.GeneralException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.migration.MigrationDetails;
import io.harness.migration.MigrationProvider;
import io.harness.migration.NGMigration;
import io.harness.migration.beans.MigrationType;
import io.harness.migration.beans.NGMigrationConfiguration;
import io.harness.migration.entities.NGSchema;
import io.harness.migration.entities.NGSchema.NGSchemaKeys;
import io.harness.migration.service.NGMigrationService;

import software.wings.beans.Schema;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Slf4j
@Singleton
@OwnedBy(DX)
public class NGMigrationServiceImpl implements NGMigrationService {
  @Inject private PersistentLocker persistentLocker;
  @Inject private ExecutorService executorService;
  @Inject private TimeLimiter timeLimiter;
  @Inject private Injector injector;
  @Inject MongoTemplate mongoTemplate;
  final String SCHEMA_PREFIX = "schema_";

  @Override
  public void runMigrations(NGMigrationConfiguration configuration) {
    List<MigrationProvider> migrationProviderList = configuration.getMigrationProviderList();
    Microservice microservice = configuration.getMicroservice();

    try (AcquiredLock lock = persistentLocker.waitToAcquireLock(
             NGSchema.class, NG_SCHEMA_ID + microservice, ofMinutes(25), ofMinutes(27))) {
      if (lock == null) {
        throw new GeneralException("The persistent lock was not acquired. That very unlikely, but yet it happened.");
      }

      for (MigrationProvider migrationProvider : migrationProviderList) {
        String serviceName = migrationProvider.getServiceName();
        String collectionName = SCHEMA_PREFIX + serviceName;
        List<? extends MigrationDetails> migrationDetailsList = migrationProvider.getMigrationDetailsList();

        log.info("[Migration] - Checking for new migrations");
        NGSchema schema = mongoTemplate.findOne(new Query(), NGSchema.class, collectionName);
        List<MigrationType> migrationTypes = migrationProvider.getMigrationDetailsList()
                                                 .stream()
                                                 .map(MigrationDetails::getMigrationTypeName)
                                                 .collect(Collectors.toList());

        if (schema == null) {
          Map<MigrationType, Integer> migrationTypesWithVersion =
              migrationTypes.stream().collect(Collectors.toMap(Function.identity(), e -> 0));
          schema = NGSchema.builder()
                       .name(migrationProvider.getServiceName())
                       .migrationDetails(migrationTypesWithVersion)
                       .build();
          mongoTemplate.save(schema, collectionName);
        }

        for (MigrationDetails migrationDetail : migrationDetailsList) {
          List<Pair<Integer, Class<? extends NGMigration>>> migrationsList = migrationDetail.getMigrations();
          Map<Integer, Class<? extends NGMigration>> migrations =
              migrationsList.stream().collect(Collectors.toMap(Pair::getKey, Pair::getValue));

          int maxVersion = migrations.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);

          Map<MigrationType, Integer> allSchemaMigrations = schema.getMigrationDetails();
          int currentVersion = allSchemaMigrations.getOrDefault(migrationDetail.getMigrationTypeName(), 0);

          boolean isBackground = migrationDetail.isBackground();
          if (isBackground) {
            runBackgroundMigrations(
                currentVersion, maxVersion, migrations, migrationDetail, collectionName, serviceName, microservice);
          } else {
            runForegroundMigrations(
                serviceName, collectionName, migrationDetail, migrations, maxVersion, currentVersion);
          }
        }
      }
    } catch (Exception e) {
      log.error("[Migration] - {} : Migration failed.", microservice, e);
    }
  }

  private void runForegroundMigrations(String serviceName, String collectionName, MigrationDetails migrationDetail,
      Map<Integer, Class<? extends NGMigration>> migrations, int maxVersion, int currentVersion) {
    if (currentVersion < maxVersion) {
      doMigration(
          currentVersion, maxVersion, migrations, migrationDetail.getMigrationTypeName(), collectionName, serviceName);

    } else if (currentVersion > maxVersion) {
      // If the current version is bigger than the max version we are downgrading. Restore to the previous version
      log.info("[Migration] - {} : Rolling back {} version from {} to {}", serviceName,
          migrationDetail.getMigrationTypeName(), currentVersion, maxVersion);
      Update update =
          new Update().setOnInsert(NGSchemaKeys.migrationDetails + migrationDetail.getMigrationTypeName(), maxVersion);
      mongoTemplate.updateFirst(new Query(), update, collectionName);
    } else {
      log.info("[Migration] - {} : NGSchema {} is up to date", serviceName, migrationDetail.getMigrationTypeName());
    }
  }

  private void runBackgroundMigrations(int currentVersion, int maxVersion,
      Map<Integer, Class<? extends NGMigration>> migrations, MigrationDetails migrationDetail, String collectionName,
      String serviceName, Microservice microservice) {
    if (currentVersion < maxVersion) {
      executorService.submit(() -> {
        migrationDetail.getMigrationTypeName();
        try (AcquiredLock ignore = persistentLocker.acquireLock(Schema.class,
                 "Background-" + SCHEMA_ID + microservice + migrationDetail.getMigrationTypeName(),
                 ofMinutes(120 + 1))) {
          timeLimiter.<Boolean>callWithTimeout(() -> {
            doMigration(currentVersion, maxVersion, migrations, migrationDetail.getMigrationTypeName(), collectionName,
                serviceName);
            return true;
          }, 2, TimeUnit.HOURS, true);
        } catch (Exception ex) {
          log.warn("Migration work", ex);
        }
      });
    } else if (currentVersion > maxVersion) {
      // If the current version is bigger than the max version we are downgrading. Restore to the previous version
      log.info("[Migration] - {} : Rolling back {} version from {} to {}", serviceName,
          migrationDetail.getMigrationTypeName(), currentVersion, maxVersion);
      Update update =
          new Update().setOnInsert(NGSchemaKeys.migrationDetails + migrationDetail.getMigrationTypeName(), maxVersion);
      mongoTemplate.updateFirst(new Query(), update, collectionName);
    } else {
      log.info("[Migration] - {} : NGSchema {} is up to date", serviceName, migrationDetail.getMigrationTypeName());
    }
  }

  private void doMigration(int currentVersion, int maxVersion, Map<Integer, Class<? extends NGMigration>> migrations,
      MigrationType migrationTypeName, String collectionName, String serviceName) {
    log.info("[Migration] - {} : Updating {} version from {} to {}", serviceName, migrationTypeName, currentVersion,
        maxVersion);

    for (int i = currentVersion + 1; i <= maxVersion; i++) {
      if (!migrations.containsKey(i)) {
        continue;
      }
      Class<? extends NGMigration> migration = migrations.get(i);
      log.info("[Migration] - {} : Migrating to version {} ...", serviceName, i);
      try {
        injector.getInstance(migration).migrate();
      } catch (Exception ex) {
        log.error("[Migration] - {} : Error while running migration {}", serviceName, migration.getSimpleName(), ex);
        break;
      }

      Update update = new Update().setOnInsert(NGSchemaKeys.migrationDetails + migrationTypeName, i);
      mongoTemplate.updateFirst(new Query(), update, collectionName);
    }

    log.info("[Migration] - {} : {} complete", serviceName, migrationTypeName);
  }
}
