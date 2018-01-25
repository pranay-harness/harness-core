package software.wings.service.impl;

import static software.wings.beans.Schema.SchemaBuilder.aSchema;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import migrations.BaseMigration;
import migrations.Migration;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Schema;
import software.wings.dl.WingsPersistence;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;
import software.wings.service.intfc.MigrationService;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MigrationServiceImpl implements MigrationService {
  private static final Logger logger = LoggerFactory.getLogger(MigrationServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private PersistentLocker persistentLocker;

  /**
   * Add your migrations to the end of the list with the next sequence number. After it has been in production for a few
   * releases it can be deleted, but keep at least one item in this list with the latest sequence number. You can use
   * BaseMigration.class if there are no migrations left.
   */
  public static final List<Pair<Integer, Class<? extends Migration>>> migrationList =
      new ImmutableList.Builder<Pair<Integer, Class<? extends Migration>>>()
          .add(Pair.of(100, BaseMigration.class))
          .build();

  @Override
  public void runMigrations() {
    Map<Integer, Class<? extends Migration>> migrations =
        migrationList.stream().collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    int maxVersion = migrations.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);

    try (AcquiredLock lock = persistentLocker.acquireLock(Schema.class.getName(), Duration.ofMinutes(30))) {
      logger.info("[Migration] - Checking for new migrations");
      Schema schema = wingsPersistence.createQuery(Schema.class).get();

      if (schema == null) {
        schema = aSchema().withVersion(maxVersion).build();
        wingsPersistence.save(schema);
      }

      if (schema.getVersion() < maxVersion) {
        logger.info("[Migration] - Updating schema version from {} to {}", schema.getVersion(), maxVersion);
        for (int i = schema.getVersion() + 1; i <= maxVersion; i++) {
          logger.info("[Migration] - Migrating to version {}...", i);
          migrations.get(i).newInstance().migrate();
        }
        logger.info("[Migration] - Migration complete");

        schema.setVersion(maxVersion);
        wingsPersistence.save(schema);
      } else {
        logger.info("[Migration] - Schema is up to date");
      }
    } catch (Exception e) {
      logger.error("[Migration] - Migration failed.", e);
    }
  }
}
