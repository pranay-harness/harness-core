package io.harness.cvng.core.dashboard.services.impl;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.DBCollectionUpdateOptions;
import io.harness.cvng.core.beans.CVMonitoringCategory;
import io.harness.cvng.core.dashboard.entities.HeatMap;
import io.harness.cvng.core.dashboard.entities.HeatMap.HeatMapKeys;
import io.harness.cvng.core.dashboard.entities.HeatMap.HeatMapRisk;
import io.harness.cvng.core.dashboard.entities.HeatMap.HeatMapRisk.HeatMapRiskKeys;
import io.harness.cvng.core.dashboard.services.api.HeatMapService;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.UpdateOptions;
import org.mongodb.morphia.query.Query;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HeatMapServiceImpl implements HeatMapService {
  @Inject private HPersistence hPersistence;

  @Override
  public void updateRiskScore(String accountId, String serviceIdentifier, String envIdentifier,
      CVMonitoringCategory category, long timeStamp, double riskScore) {
    long bucketBoundary = timeStamp - Math.floorMod(timeStamp, TimeUnit.HOURS.toMillis(4));
    UpdateOptions options = new UpdateOptions();
    options.upsert(true);

    Query<HeatMap> heatMapQuery = hPersistence.createQuery(HeatMap.class)
                                      .filter(HeatMapKeys.serviceIdentifier, serviceIdentifier)
                                      .filter(HeatMapKeys.envIdentifier, envIdentifier)
                                      .filter(HeatMapKeys.category, category)
                                      .filter(HeatMapKeys.heatMapBucketStartTime, Instant.ofEpochMilli(bucketBoundary));
    // first create the heatmap record if it doesn't exists
    hPersistence.getDatastore(HeatMap.class)
        .update(heatMapQuery,
            hPersistence.createUpdateOperations(HeatMap.class)
                .set(HeatMapKeys.accountId, accountId)
                .push(HeatMapKeys.heatMapRisks,
                    Lists.newArrayList(
                        HeatMapRisk.builder().riskScore(riskScore).timeStamp(Instant.ofEpochMilli(timeStamp)).build())),
            options);

    DBCollectionUpdateOptions arrayFilterOptions = new DBCollectionUpdateOptions();
    arrayFilterOptions.upsert(true);
    arrayFilterOptions.multi(false);
    Map<String, Object> filterMap = new HashMap<>();
    filterMap.put("elem." + HeatMapRiskKeys.timeStamp, Instant.ofEpochMilli(timeStamp));
    filterMap.put("elem." + HeatMapRiskKeys.riskScore, new BasicDBObject("$lt", riskScore));
    arrayFilterOptions.arrayFilters(Lists.newArrayList(new BasicDBObject(filterMap)));
    hPersistence.getCollection(HeatMap.class)
        .update(heatMapQuery.getQueryObject(),
            new BasicDBObject("$set",
                new BasicDBObject(HeatMapKeys.heatMapRisks + ".$[elem]." + HeatMapRiskKeys.riskScore, riskScore)),
            arrayFilterOptions);
  }
}
