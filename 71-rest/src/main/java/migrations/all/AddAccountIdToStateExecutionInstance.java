package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
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
import org.mongodb.morphia.Key;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.dl.WingsPersistence;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class AddAccountIdToStateExecutionInstance implements Migration {
  @Inject protected WingsPersistence wingsPersistence;
  private String debugLine = "STATEEXECUTIONINSTANCE MIGRATION: ";

  @Override
  public void migrate() {
    log.info(debugLine + "Migration of stateExecutionInstances started");
    Map<String, Set<String>> accountIdToAppIdMap = new HashMap<>();
    try (HIterator<Account> accounts = new HIterator<>(
             wingsPersistence.createQuery(Account.class, excludeAuthority).project(Account.ID_KEY, true).fetch())) {
      while (accounts.hasNext()) {
        final Account account = accounts.next();
        final String accountId = account.getUuid();

        List<Key<Application>> appIdKeyList =
            wingsPersistence.createQuery(Application.class).filter(ApplicationKeys.accountId, accountId).asKeyList();

        if (isNotEmpty(appIdKeyList)) {
          Set<String> appIdSet =
              appIdKeyList.stream().map(applicationKey -> (String) applicationKey.getId()).collect(Collectors.toSet());
          accountIdToAppIdMap.put(accountId, appIdSet);
        }
      }
    }
    for (Map.Entry<String, Set<String>> entry : accountIdToAppIdMap.entrySet()) {
      for (String appId : entry.getValue()) {
        bulkSetAccountId(entry.getKey(), "stateExecutionInstances", appId);
      }
    }
    log.info(debugLine + "Migration of stateExecutionInstances finished");
  }

  private void bulkSetAccountId(String accountId, String collectionName, String appId) {
    log.info(debugLine + "Migrating all stateExecutionInstances for account " + accountId);
    final DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, collectionName);

    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    BasicDBObject objectsToBeUpdated = new BasicDBObject("accountId", null).append("appId", appId);
    BasicDBObject projection = new BasicDBObject("_id", Boolean.TRUE);
    DBCursor dataRecords = collection.find(objectsToBeUpdated, projection).limit(1000);

    int updated = 0;
    int batched = 0;
    try {
      while (dataRecords.hasNext()) {
        DBObject record = dataRecords.next();

        String uuId = (String) record.get("_id");

        bulkWriteOperation.find(new BasicDBObject("_id", uuId))
            .updateOne(new BasicDBObject("$set", new BasicDBObject("accountId", accountId)));
        updated++;
        batched++;

        if (updated != 0 && updated % 1000 == 0) {
          bulkWriteOperation.execute();
          sleep(Duration.ofMillis(100));
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          dataRecords = collection.find(objectsToBeUpdated, projection).limit(1000);
          batched = 0;
          log.info(debugLine + "Number of records updated for {} is: {}", collectionName, updated);
        }
      }

      if (batched != 0) {
        bulkWriteOperation.execute();
        log.info(debugLine + "Number of records updated for {} is: {}", collectionName, updated);
      }
    } catch (Exception e) {
      log.error(debugLine + "Exception occurred while migrating account id field for {}", collectionName, e);
    } finally {
      dataRecords.close();
    }
  }
}
