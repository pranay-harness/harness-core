package io.harness.cvng.migration.impl;

import static io.harness.cvng.migration.beans.CVNGSchema.CVNGMigrationStatus.PENDING;
import static io.harness.cvng.migration.beans.CVNGSchema.SCHEMA_ID;

import io.harness.cvng.migration.CNVGMigration;
import io.harness.cvng.migration.CVNGBackgroundMigrationList;
import io.harness.cvng.migration.beans.CVNGSchema;
import io.harness.cvng.migration.service.CVNGMigrationService;
import io.harness.persistence.HPersistence;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Singleton
@Slf4j
public class CVNGMigrationServiceImpl implements CVNGMigrationService {
  @Inject private HPersistence hPersistence;
  @Inject private Injector injector;
  @Inject private ExecutorService executorService;
  @Inject private TimeLimiter timeLimiter;

  @Override
  public void runMigrations() {
    Map<Integer, Class<? extends CNVGMigration>> backgroundMigrations =
        CVNGBackgroundMigrationList.getMigrations().stream().collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    int maxBackgroundVersion = backgroundMigrations.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);

    log.info("[Migration] - Initializing Global DB Entries");
    initializeGlobalDbEntriesIfNeeded();

    log.info("[Migration] - Checking for new backgroundMigrations");
    CVNGSchema cvngSchema = hPersistence.createQuery(CVNGSchema.class).get();

    if (cvngSchema.getVersion() < maxBackgroundVersion) {
      executorService.submit(() -> {
        try {
          timeLimiter.<Boolean>callWithTimeout(() -> {
            log.info(
                "[Migration] - Updating schema version from {} to {}", cvngSchema.getVersion(), maxBackgroundVersion);
            for (int i = cvngSchema.getVersion() + 1; i <= maxBackgroundVersion; i++) {
              if (backgroundMigrations.containsKey(i)) {
                Class<? extends CNVGMigration> migration = backgroundMigrations.get(i);
                log.info("[Migration] - Migrating to version {}: {} ...", i, migration.getSimpleName());
                try {
                  injector.getInstance(migration).migrate();
                } catch (Exception ex) {
                  log.error("Error while running migration {}", migration.getSimpleName(), ex);
                  break;
                }
                final UpdateOperations<CVNGSchema> updateOperations =
                    hPersistence.createUpdateOperations(CVNGSchema.class);
                updateOperations.set(CVNGSchema.VERSION, i);
                hPersistence.update(hPersistence.createQuery(CVNGSchema.class), updateOperations);
              }
            }
            log.info("[Migration] - Migration complete");
            return true;
          }, 2, TimeUnit.HOURS, true);
        } catch (Exception ex) {
          log.warn("background work", ex);
        }
      });
    }
  }

  private void initializeGlobalDbEntriesIfNeeded() {
    try {
      Query<CVNGSchema> globalSchemaQuery = hPersistence.createQuery(CVNGSchema.class).filter("_id", SCHEMA_ID);
      CVNGSchema globalSchema = globalSchemaQuery.get();
      if (globalSchema == null) {
        hPersistence.save(CVNGSchema.builder().uuid(SCHEMA_ID).version(0).cvngMigrationStatus(PENDING).build());
      }
    } catch (Exception e) {
      log.error("[Migration] - initializeGlobalDbEntriesIfNeeded failed.", e);
    }
  }
}
