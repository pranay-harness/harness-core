package migrations.all;

import static software.wings.beans.Base.ID_KEY;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import migrations.Migration;
import org.mongodb.morphia.query.MorphiaIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.sm.StateExecutionInstance;

public class StateExecutionInstanceDisplayName implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(StateExecutionInstanceDisplayName.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    MorphiaIterator<StateExecutionInstance, StateExecutionInstance> iterator =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .field("displayName")
            .doesNotExist()
            .project("uuid", true)
            .project("stateName", true)
            .fetch();

    final DBCollection collection = wingsPersistence.getCollection("stateExecutionInstances");
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    int i = 1;
    try (DBCursor cursor = iterator.getCursor()) {
      while (iterator.hasNext()) {
        StateExecutionInstance stateExecutionInstance = iterator.next();
        if (stateExecutionInstance.getDisplayName() != null || stateExecutionInstance.getStateName() == null) {
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
                      .filter(ID_KEY, stateExecutionInstance.getUuid())
                      .getQueryObject())
            .updateOne(
                new BasicDBObject("$set", new BasicDBObject("displayName", stateExecutionInstance.getStateName())));
      }
    }
    if (i % 1000 != 1) {
      bulkWriteOperation.execute();
    }
  }
}
