package io.harness.migration.service.impl;

import io.harness.migration.MigrationDetails;
import io.harness.migration.MigrationProvider;
import io.harness.migration.NGMigration;
import io.harness.migration.beans.MigrationType;
import io.harness.migration.entities.NGSchema;
import io.harness.migration.entities.NGSchema.NGSchemaKeys;
import io.harness.migration.service.NGMigrationService;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Injector;
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
public class NGMigrationServiceImpl implements NGMigrationService {
  @Inject private MigrationProvider migrationProvider;
  @Inject private ExecutorService executorService;
  @Inject private TimeLimiter timeLimiter;
  @Inject private Injector injector;
  @Inject MongoTemplate mongoTemplate;
  //  @Inject private MigrationDetails migrationDetails;
  final String SCHEMA_PREFIX = "schema_";

  @Override
  public void runMigrations() {
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
      Map<String, Integer> migrationTypesWithVersion =
          migrationTypes.stream().collect(Collectors.toMap(Function.identity(), 0));
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

      Map<String, Integer> allSchemaMigrations = schema.getMigrationDetails();
      int currentVersion = allSchemaMigrations.getOrDefault(migrationDetail.getMigrationTypeName(), 0);

      boolean isBackground = migrationDetail.isBackground();
      if (isBackground) {
        migrateBackgroundMigrations(
            currentVersion, maxVersion, migrations, migrationDetail, collectionName, serviceName);
      } else {
        if (currentVersion < maxVersion) {
          executorService.submit(() -> {
            doMigration(currentVersion, maxVersion, migrations, migrationDetail.getMigrationTypeName(), collectionName,
                serviceName);
          });

        } else if (currentVersion > maxVersion) {
          // If the current version is bigger than the max version we are downgrading. Restore to the previous version
          log.info("[Migration] - {} : Rolling back {} version from {} to {}", serviceName,
              migrationDetail.getMigrationTypeName(), currentVersion, maxVersion);
          Update update = new Update().setOnInsert(
              NGSchemaKeys.migrationDetails + migrationDetail.getMigrationTypeName(), maxVersion);
          mongoTemplate.updateFirst(new Query(), update, collectionName);
        } else {
          log.info("[Migration] - {} : NGSchema {} is up to date", serviceName, migrationDetail.getMigrationTypeName());
        }
      }
    }
  }

  private void migrateBackgroundMigrations(int currentVersion, int maxVersion,
      Map<Integer, Class<? extends NGMigration>> migrations, MigrationDetails migrationDetail, String collectionName,
      String serviceName) {
    if (currentVersion < maxVersion) {
      executorService.submit(() -> {
        //        timeLimiter.callUninterruptiblyWithTimeout()
        timeLimiter.<Boolean>callWithTimeout(() -> {
          doMigration(currentVersion, maxVersion, migrations, migrationDetail.getMigrationTypeName(), collectionName,
              serviceName);
          return true;
        }, 2, TimeUnit.HOURS, true);
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
