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

/**
 * This will migrate the instance data to timescale db
 * @author rktummala
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
    int count = 0;
    FindOptions findOptions = new FindOptions();
    findOptions.readPreference(ReadPreference.secondaryPreferred());

    try (HIterator<Instance> iterator = new HIterator<>(wingsPersistence.createQuery(Instance.class, excludeAuthority)
                                                            .order(Sort.descending(InstanceKeys.createdAt))
                                                            .fetch(findOptions))) {
      while (iterator.hasNext()) {
        Instance instance = iterator.next();
        count++;
        boolean exists = instanceTimeScaleProcessor.checkIfInstanceExists(instance.getUuid());
        if (exists) {
          instanceTimeScaleProcessor.updateInstance(instance);
        } else {
          instanceTimeScaleProcessor.createInstance(instance);
        }
        logger.info("Migrated instances {}, current instance processed {}", count, instance.getUuid());
      }
      logger.info("Completed migrating instance data to timescale db");
      return true;
    } catch (Exception e) {
      logger.warn("Failed to complete instance data migration", e);
      return false;
    }
  }
}
