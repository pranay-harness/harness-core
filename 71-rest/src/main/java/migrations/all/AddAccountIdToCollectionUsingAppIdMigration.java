package migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static io.harness.threading.Morpheus.sleep;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Application;
import software.wings.dl.WingsPersistence;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class AddAccountIdToCollectionUsingAppIdMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    logger.info("Adding accountId to {}", getCollectionName());
    Map<String, String> appIdToAccountIdMap = new HashMap<>();
    updateAccountIdForAppId(appIdToAccountIdMap);
    logger.info("Adding accountIds to {} completed for all applications", getCollectionName());
  }

  private String getAccountIdForAppId(Map<String, String> appIdToAccountIdMap, String appId) {
    if (!appIdToAccountIdMap.containsKey(appId)) {
      logger.info("Fetching account for app id {} and collection {}", appId, getCollectionName());
      Application application = wingsPersistence.get(Application.class, appId);
      if (application == null) {
        appIdToAccountIdMap.put(appId, "dummy_account_id");
      } else {
        appIdToAccountIdMap.put(appId, application.getAccountId());
      }
      logger.info("Set account id: {} for app id {} and collection {}", appIdToAccountIdMap.get(appId), appId,
          getCollectionName());
    }

    return appIdToAccountIdMap.get(appId);
  }

  private void updateAccountIdForAppId(Map<String, String> appIdToAccountIdMap) {
    final DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, getCollectionName());

    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    BasicDBObject objectsToBeUpdated = new BasicDBObject(getFieldName(), null);
    BasicDBObject projection = new BasicDBObject("_id", true).append("appId", true);
    DBCursor dataRecords = collection.find(objectsToBeUpdated, projection).limit(1000);

    int updated = 0;
    int batched = 0;
    try {
      while (dataRecords.hasNext()) {
        DBObject record = dataRecords.next();

        String uuId = (String) record.get("_id");
        String appId = (String) record.get("appId");
        String accountId = getAccountIdForAppId(appIdToAccountIdMap, appId);

        bulkWriteOperation.find(new BasicDBObject("_id", uuId))
            .updateOne(new BasicDBObject("$set", new BasicDBObject(getFieldName(), accountId)));
        updated++;
        batched++;

        if (updated != 0 && updated % 1000 == 0) {
          bulkWriteOperation.execute();
          sleep(Duration.ofMillis(200));
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          dataRecords = collection.find(objectsToBeUpdated, projection).limit(1000);
          batched = 0;
          logger.info("Number of records updated for {} is: {}", getCollectionName(), updated);
        }
      }

      if (batched != 0) {
        bulkWriteOperation.execute();
        logger.info("Number of records updated for {} is: {}", getCollectionName(), updated);
      }
    } catch (Exception e) {
      logger.error("Exception occurred while migrating account id field for {}", getCollectionName(), e);
    } finally {
      dataRecords.close();
    }
  }

  protected abstract String getCollectionName();

  protected abstract String getFieldName();
}
