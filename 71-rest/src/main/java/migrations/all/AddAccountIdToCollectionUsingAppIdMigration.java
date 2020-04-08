package migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class AddAccountIdToCollectionUsingAppIdMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;

  @Override
  public void migrate() {
    logger.info("Adding accountId to {}", getCollectionName());
    List<String> accountIds = new ArrayList<>();
    try (HIterator<Account> accounts =
             new HIterator<>(wingsPersistence.createQuery(Account.class, excludeAuthority).fetch())) {
      while (accounts.hasNext()) {
        accountIds.add(accounts.next().getUuid());
      }
    }

    for (String accountId : accountIds) {
      List<String> appIds = appService.getAppIdsByAccountId(accountId);
      for (String appId : appIds) {
        try {
          updateAccountIdForAppId(accountId, appId);
        } catch (Exception e) {
          logger.error("Exception occurred while updating account id for appID: " + appId);
        }
      }
    }
    logger.info("Adding accountIds to {} completed for all applications", getCollectionName());
  }

  private void updateAccountIdForAppId(String accountId, String appId) {
    final DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, getCollectionName());
    logger.info("Adding accountId to {} for application {}", getCollectionName(), appId);

    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    BasicDBObject objectsToBeUpdated = new BasicDBObject("appId", appId).append(getFieldName(), null);

    DBCursor dataRecords = collection.find(objectsToBeUpdated);

    logger.info("Number of records to be updated: " + dataRecords.size());

    int updated = 0;
    int batched = 0;
    while (dataRecords.hasNext()) {
      DBObject record = dataRecords.next();

      String uuId = (String) record.get("_id");
      bulkWriteOperation.find(new BasicDBObject("_id", uuId))
          .updateOne(new BasicDBObject("$set", new BasicDBObject(getFieldName(), accountId)));
      updated++;
      batched++;

      if (updated != 0 && updated % 1000 == 0) {
        bulkWriteOperation.execute();
        sleep(Duration.ofMillis(200));
        bulkWriteOperation = collection.initializeUnorderedBulkOperation();
        batched = 0;
        logger.info("updated: " + updated);
      }

      try {
        dataRecords.hasNext();
      } catch (IllegalStateException e) {
        dataRecords = collection.find(objectsToBeUpdated);
      }
    }

    if (batched != 0) {
      bulkWriteOperation.execute();
      logger.info("updated: " + updated);
    }
    logger.info("updated {} records for application {} ", updated, appId);
  }

  protected abstract String getCollectionName();

  protected abstract String getFieldName();
}
