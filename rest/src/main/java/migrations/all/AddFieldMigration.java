package migrations.all;

import static software.wings.beans.Base.ID_KEY;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import migrations.Migration;
import org.slf4j.Logger;
import software.wings.dl.WingsPersistence;

/**
 * Created by rsingh on 3/26/18.
 */
public abstract class AddFieldMigration implements Migration {
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    DBCollection collection = wingsPersistence.getCollection(getCollectionName());
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    DBCursor dataRecords = wingsPersistence.getCollection(getCollectionName()).find();

    getLogger().info("will go through " + dataRecords.size() + " records");

    int updated = 0;
    int batched = 0;
    while (dataRecords.hasNext()) {
      DBObject next = dataRecords.next();

      String uuId = (String) next.get("_id");
      bulkWriteOperation.find(wingsPersistence.createQuery(getCollectionClass()).filter(ID_KEY, uuId).getQueryObject())
          .updateOne(new BasicDBObject("$set", new BasicDBObject(getFieldName(), getFieldValue())));
      updated++;
      batched++;

      if (updated != 0 && updated % 1000 == 0) {
        bulkWriteOperation.execute();
        bulkWriteOperation = collection.initializeUnorderedBulkOperation();
        batched = 0;
        getLogger().info("updated: " + updated);
      }
    }

    if (batched != 0) {
      bulkWriteOperation.execute();
      getLogger().info("updated: " + updated);
    }

    getLogger().info("Complete. Updated " + updated + " records.");
  }
  protected abstract Logger getLogger();

  protected abstract String getCollectionName();

  protected abstract Class getCollectionClass();

  protected abstract String getFieldName();

  protected abstract Object getFieldValue();
}
