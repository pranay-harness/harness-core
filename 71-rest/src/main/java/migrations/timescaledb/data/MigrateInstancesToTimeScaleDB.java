package migrations.timescaledb.data;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.ReadPreference;
import io.harness.persistence.HIterator;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import migrations.TimeScaleDBDataMigration;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.instance.InstanceTimeScaleProcessor;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * This will migrate the instance data to timescale db
 */
@Slf4j
@Singleton
public class MigrateInstancesToTimeScaleDB implements TimeScaleDBDataMigration {
  @Inject TimeScaleDBService timeScaleDBService;
  @Inject InstanceTimeScaleProcessor instanceTimeScaleProcessor;

  @Inject WingsPersistence wingsPersistence;

  @Override
  public boolean migrate() {
    if (!timeScaleDBService.isValid()) {
      logger.info("TimeScaleDB not found, not migrating data to TimeScaleDB");
      return false;
    }

    logger.info("Starting instance data migration to timescale db");

    FindOptions findOptions = new FindOptions();
    findOptions.readPreference(ReadPreference.secondaryPreferred());

    try (HIterator<Instance> iterator = new HIterator<>(wingsPersistence.createQuery(Instance.class, excludeAuthority)
                                                            .order(Sort.descending(InstanceKeys.createdAt))
                                                            .fetch(findOptions))) {
      while (iterator.hasNext()) {
        Instance instance = iterator.next();
        try (Connection connection = timeScaleDBService.getDBConnection()) {
          boolean exists = instanceTimeScaleProcessor.checkIfInstanceExists(connection, instance.getUuid());
          if (exists) {
            instanceTimeScaleProcessor.updateInstance(connection, instance);
          } else {
            instanceTimeScaleProcessor.createInstance(connection, instance);
          }
        }
      }
      logger.info("Completed migrating instance data to timescale db");
      return true;
    } catch (SQLException e) {
      logger.warn("Failed to complete instance data migration", e);
      return false;
    }
  }
}
