package migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.TimeSeriesKeyTransactions;
import software.wings.service.impl.analysis.TimeSeriesKeyTransactions.TimeSeriesKeyTransactionsKeys;
import software.wings.verification.CVConfiguration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class AddAccountIdToTimeSeriesKeyTransaction implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, "timeSeriesKeyTransactions");

    logger.info("Starting migration from timeSeriesKeyTransactions");
    Map<String, String> configToAccountIdMap = new HashMap<>();
    try (HIterator<TimeSeriesKeyTransactions> timeSeriesKeyTransactionsHIterator =
             new HIterator<>(wingsPersistence.createQuery(TimeSeriesKeyTransactions.class, excludeAuthority)
                                 .filter("accountId", null)
                                 .fetch())) {
      try {
        while (timeSeriesKeyTransactionsHIterator.hasNext()) {
          TimeSeriesKeyTransactions keyTransactions = timeSeriesKeyTransactionsHIterator.next();
          String cvConfigId = keyTransactions.getCvConfigId();
          String uuId = keyTransactions.getUuid();

          if (!configToAccountIdMap.containsKey(cvConfigId)) {
            CVConfiguration cvConfiguration = wingsPersistence.get(CVConfiguration.class, cvConfigId);
            if (cvConfiguration != null) {
              configToAccountIdMap.put(cvConfigId, cvConfiguration.getAccountId());
            }
          }

          String accountId = configToAccountIdMap.get(cvConfigId);

          if (accountId == null) {
            collection.remove(new BasicDBObject("_id", uuId));
            logger.info("Deleted key transaction for cvConfigId: {}", cvConfigId);
          } else {
            collection.update(new BasicDBObject("_id", uuId),
                new BasicDBObject("$set", new BasicDBObject(TimeSeriesKeyTransactionsKeys.accountId, accountId)));
            logger.info("Updated account id for id: {}", uuId);
          }
          sleep(Duration.ofMillis(100));
        }
      } catch (Exception e) {
        logger.error("Exception while migrating timeSeriesKeyTransactions", e);
      }
    }
  }
}
