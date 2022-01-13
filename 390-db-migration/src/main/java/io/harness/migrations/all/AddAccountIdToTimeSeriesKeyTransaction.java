/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.TimeSeriesKeyTransactions;
import software.wings.service.impl.analysis.TimeSeriesKeyTransactions.TimeSeriesKeyTransactionsKeys;
import software.wings.verification.CVConfiguration;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddAccountIdToTimeSeriesKeyTransaction implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, "timeSeriesKeyTransactions");

    log.info("Starting migration from timeSeriesKeyTransactions");
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
            log.info("Deleted key transaction for cvConfigId: {}", cvConfigId);
          } else {
            collection.update(new BasicDBObject("_id", uuId),
                new BasicDBObject("$set", new BasicDBObject(TimeSeriesKeyTransactionsKeys.accountId, accountId)));
            log.info("Updated account id for id: {}", uuId);
          }
          sleep(Duration.ofMillis(100));
        }
      } catch (Exception e) {
        log.error("Exception while migrating timeSeriesKeyTransactions", e);
      }
    }
  }
}
