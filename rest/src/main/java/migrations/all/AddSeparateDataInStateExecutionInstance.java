package migrations.all;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import migrations.Migration;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.MorphiaIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;

@SuppressWarnings("deprecation")
public class AddSeparateDataInStateExecutionInstance implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(AddSeparateDataInStateExecutionInstance.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    Mapper mapper = ((org.mongodb.morphia.DatastoreImpl) wingsPersistence.getDatastore()).getMapper();
    final DBCollection collection = wingsPersistence.getCollection("stateExecutionInstances");
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    final MorphiaIterator<StateExecutionInstance, StateExecutionInstance> stateExecutionInstances =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .field(StateExecutionInstance.STATE_EXECUTION_DATA_KEY)
            .doesNotExist()
            .field(StateExecutionInstance.STATE_EXECUTION_MAP_KEY)
            .exists()
            .project(StateExecutionInstance.DISPLAY_NAME_KEY, true)
            .project(StateExecutionInstance.STATE_EXECUTION_MAP_KEY, true)
            .fetch();

    int i = 1;
    try (DBCursor ignored = stateExecutionInstances.getCursor()) {
      while (stateExecutionInstances.hasNext()) {
        final StateExecutionInstance stateExecutionInstance = stateExecutionInstances.next();

        final StateExecutionData stateExecutionData =
            stateExecutionInstance.getStateExecutionMap().get(stateExecutionInstance.getDisplayName());
        if (stateExecutionData == null) {
          logger.info("StateExecutionInstance: {} skipped", stateExecutionInstance.getUuid());
          continue;
        }

        if (i % 1000 == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          logger.info("StateExecutionInstance: {} updated", i);
        }
        ++i;

        bulkWriteOperation
            .find(wingsPersistence.createQuery(StateExecutionInstance.class)
                      .filter(StateExecutionInstance.ID_KEY, stateExecutionInstance.getUuid())
                      .getQueryObject())
            .updateOne(new BasicDBObject("$set",
                new BasicDBObject(
                    StateExecutionInstance.STATE_EXECUTION_DATA_KEY, mapper.toDBObject(stateExecutionData))));
      }
    }
    if (i % 1000 != 1) {
      bulkWriteOperation.execute();
      logger.info("StateExecutionInstance: {} updated", i);
    }
  }
}
