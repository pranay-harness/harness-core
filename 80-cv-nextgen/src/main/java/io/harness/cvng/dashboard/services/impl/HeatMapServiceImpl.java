package io.harness.cvng.dashboard.services.impl;

import static io.harness.cvng.dashboard.entities.HeatMap.HeatMapResolution.getHeatMapResolution;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.DBCollectionUpdateOptions;
import io.harness.cvng.core.beans.CVMonitoringCategory;
import io.harness.cvng.core.dashboard.beans.HeatMapDTO;
import io.harness.cvng.dashboard.entities.HeatMap;
import io.harness.cvng.dashboard.entities.HeatMap.HeatMapKeys;
import io.harness.cvng.dashboard.entities.HeatMap.HeatMapResolution;
import io.harness.cvng.dashboard.entities.HeatMap.HeatMapRisk;
import io.harness.cvng.dashboard.entities.HeatMap.HeatMapRisk.HeatMapRiskKeys;
import io.harness.cvng.dashboard.services.api.HeatMapService;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.UpdateOptions;
import org.mongodb.morphia.query.Query;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class HeatMapServiceImpl implements HeatMapService {
  @Inject private HPersistence hPersistence;

  @Override
  public void updateRiskScore(String accountId, String serviceIdentifier, String envIdentifier,
      CVMonitoringCategory category, Instant timeStamp, double riskScore) {
    UpdateOptions options = new UpdateOptions();
    options.upsert(true);
    for (HeatMapResolution heatMapResolution : HeatMapResolution.values()) {
      Instant bucketStartTime = getBoundaryOfResolution(timeStamp, heatMapResolution.getBucketSize());
      Instant bucketEndTime = bucketStartTime.plusMillis(heatMapResolution.getBucketSize().toMillis() - 1);
      Instant heatMapStartTime = getBoundaryOfResolution(timeStamp, heatMapResolution.getResolution());
      Instant heatMapEndTime = heatMapStartTime.plusMillis(heatMapResolution.getResolution().toMillis() - 1);

      Query<HeatMap> heatMapQuery = hPersistence.createQuery(HeatMap.class)
                                        .filter(HeatMapKeys.serviceIdentifier, serviceIdentifier)
                                        .filter(HeatMapKeys.envIdentifier, envIdentifier)
                                        .filter(HeatMapKeys.category, category)
                                        .filter(HeatMapKeys.heatMapResolution, heatMapResolution)
                                        .filter(HeatMapKeys.heatMapBucketStartTime, bucketStartTime)
                                        .filter(HeatMapKeys.heatMapBucketEndTime, bucketEndTime);

      // first create the heatmap record if it doesn't exists
      hPersistence.getDatastore(HeatMap.class)
          .update(heatMapQuery,
              hPersistence.createUpdateOperations(HeatMap.class)
                  .set(HeatMapKeys.accountId, accountId)
                  .addToSet(HeatMapKeys.heatMapRisks,
                      HeatMapRisk.builder()
                          .riskScore(riskScore)
                          .startTime(heatMapStartTime)
                          .endTime(heatMapEndTime)
                          .build()),
              options);

      DBCollectionUpdateOptions arrayFilterOptions = new DBCollectionUpdateOptions();
      arrayFilterOptions.upsert(true);
      arrayFilterOptions.multi(false);
      Map<String, Object> filterMap = new HashMap<>();
      filterMap.put("elem." + HeatMapRiskKeys.startTime, heatMapStartTime);
      filterMap.put("elem." + HeatMapRiskKeys.endTime, heatMapEndTime);
      filterMap.put("elem." + HeatMapRiskKeys.riskScore, new BasicDBObject("$lt", riskScore));
      arrayFilterOptions.arrayFilters(Lists.newArrayList(new BasicDBObject(filterMap)));
      hPersistence.getCollection(HeatMap.class)
          .update(heatMapQuery.getQueryObject(),
              new BasicDBObject("$set",
                  new BasicDBObject(HeatMapKeys.heatMapRisks + ".$[elem]." + HeatMapRiskKeys.riskScore, riskScore)),
              arrayFilterOptions);
    }
  }

  @Override
  public Map<CVMonitoringCategory, SortedSet<HeatMapDTO>> getHeatMap(
      String accountId, String serviceIdentifier, String envIdentifier, Instant startTime, Instant endTime) {
    Map<CVMonitoringCategory, SortedSet<HeatMapDTO>> heatMaps = new HashMap<>();
    HeatMapResolution heatMapResolution = getHeatMapResolution(startTime, endTime);
    Instant startTimeBoundary = getBoundaryOfResolution(startTime, heatMapResolution.getResolution());
    Instant endTimeBoundary = getBoundaryOfResolution(endTime, heatMapResolution.getResolution());

    for (CVMonitoringCategory category : CVMonitoringCategory.values()) {
      Map<Instant, HeatMapDTO> heatMapsFromDB =
          getHeatMapsFromDB(serviceIdentifier, envIdentifier, category, startTime, endTime, heatMapResolution);

      SortedSet<HeatMapDTO> heatMapDTOS = new TreeSet<>();
      for (long timeStampMs = startTimeBoundary.toEpochMilli(); timeStampMs <= endTimeBoundary.toEpochMilli();
           timeStampMs += heatMapResolution.getResolution().toMillis()) {
        if (heatMapsFromDB.containsKey(Instant.ofEpochMilli(timeStampMs))) {
          heatMapDTOS.add(heatMapsFromDB.get(Instant.ofEpochMilli(timeStampMs)));
          continue;
        }

        heatMapDTOS.add(HeatMapDTO.builder()
                            .startTime(timeStampMs)
                            .endTime(timeStampMs + heatMapResolution.getResolution().toMillis() - 1)
                            .build());
      }
      heatMaps.put(category, heatMapDTOS);
    }
    return heatMaps;
  }

  private Map<Instant, HeatMapDTO> getHeatMapsFromDB(String serviceIdentifier, String envIdentifier,
      CVMonitoringCategory category, Instant startTime, Instant endTime, HeatMapResolution heatMapResolution) {
    Instant startTimeBucketBoundary = getBoundaryOfResolution(startTime, heatMapResolution.getBucketSize());
    Instant endTimeBucketBoundary = getBoundaryOfResolution(endTime, heatMapResolution.getBucketSize());
    Map<Instant, HeatMapDTO> heatMapDTOS = new HashMap<>();
    try (HIterator<HeatMap> heatMapRecords =
             new HIterator<>(hPersistence.createQuery(HeatMap.class, excludeAuthority)
                                 .filter(HeatMapKeys.serviceIdentifier, serviceIdentifier)
                                 .filter(HeatMapKeys.envIdentifier, envIdentifier)
                                 .filter(HeatMapKeys.category, category)
                                 .filter(HeatMapKeys.heatMapResolution, heatMapResolution)
                                 .field(HeatMapKeys.heatMapBucketStartTime)
                                 .greaterThanOrEq(startTimeBucketBoundary)
                                 .field(HeatMapKeys.heatMapBucketStartTime)
                                 .lessThanOrEq(endTimeBucketBoundary)
                                 .fetch())) {
      while (heatMapRecords.hasNext()) {
        HeatMap heatMap = heatMapRecords.next();
        heatMap.getHeatMapRisks()
            .stream()
            .filter(heatMapRisk
                -> heatMapRisk.getStartTime().compareTo(startTime) >= 0
                    && heatMapRisk.getStartTime().compareTo(endTime) <= 0)
            .forEach(heatMapRisk
                -> heatMapDTOS.put(heatMapRisk.getStartTime(),
                    HeatMapDTO.builder()
                        .startTime(heatMapRisk.getStartTime().toEpochMilli())
                        .endTime(heatMapRisk.getEndTime().toEpochMilli())
                        .riskScore(heatMapRisk.getRiskScore())
                        .build()));
      }
    }
    return heatMapDTOS;
  }

  private Instant getBoundaryOfResolution(Instant input, Duration resolution) {
    long timeStamp = input.toEpochMilli();
    return Instant.ofEpochMilli(timeStamp - (timeStamp % resolution.toMillis()));
  }
}
