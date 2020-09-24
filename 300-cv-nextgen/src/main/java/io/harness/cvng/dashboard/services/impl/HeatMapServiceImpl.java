package io.harness.cvng.dashboard.services.impl;

import static io.harness.cvng.core.utils.DateTimeUtils.roundDownTo5MinBoundary;
import static io.harness.cvng.dashboard.entities.HeatMap.HeatMapResolution.getHeatMapResolution;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.DBCollectionUpdateOptions;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.dashboard.beans.EnvServiceRiskDTO;
import io.harness.cvng.dashboard.beans.EnvServiceRiskDTO.ServiceRisk;
import io.harness.cvng.dashboard.beans.EnvToServicesDTO;
import io.harness.cvng.dashboard.beans.HeatMapDTO;
import io.harness.cvng.dashboard.entities.HeatMap;
import io.harness.cvng.dashboard.entities.HeatMap.HeatMapKeys;
import io.harness.cvng.dashboard.entities.HeatMap.HeatMapResolution;
import io.harness.cvng.dashboard.entities.HeatMap.HeatMapRisk;
import io.harness.cvng.dashboard.entities.HeatMap.HeatMapRisk.HeatMapRiskKeys;
import io.harness.cvng.dashboard.services.api.HeatMapService;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.UpdateOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

public class HeatMapServiceImpl implements HeatMapService {
  private static final int RISK_TIME_BUFFER_MINS = 15;
  @Inject private HPersistence hPersistence;
  @Inject private CVConfigService cvConfigService;
  @Inject private Clock clock;

  @Override
  public void updateRiskScore(String accountId, String projectIdentifier, String serviceIdentifier,
      String envIdentifier, CVMonitoringCategory category, Instant timeStamp, double riskScore) {
    // update for service/env
    updateRiskScore(category, accountId, projectIdentifier, serviceIdentifier, envIdentifier, timeStamp, riskScore);

    // update for env
    updateRiskScore(category, accountId, projectIdentifier, null, envIdentifier, timeStamp, riskScore);

    // update for project
    updateRiskScore(category, accountId, projectIdentifier, null, null, timeStamp, riskScore);
  }

  private void updateRiskScore(CVMonitoringCategory category, String accountId, String projectIdentifier,
      String serviceIdentifier, String envIdentifier, Instant timeStamp, double riskScore) {
    UpdateOptions options = new UpdateOptions();
    options.upsert(true);
    for (HeatMapResolution heatMapResolution : HeatMapResolution.values()) {
      Instant bucketStartTime = getBoundaryOfResolution(timeStamp, heatMapResolution.getBucketSize());
      Instant bucketEndTime = bucketStartTime.plusMillis(heatMapResolution.getBucketSize().toMillis() - 1);
      Instant heatMapStartTime = getBoundaryOfResolution(timeStamp, heatMapResolution.getResolution());
      Instant heatMapEndTime = heatMapStartTime.plusMillis(heatMapResolution.getResolution().toMillis() - 1);

      Query<HeatMap> heatMapQuery = hPersistence.createQuery(HeatMap.class)
                                        .filter(HeatMapKeys.projectIdentifier, projectIdentifier)
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
                  .setOnInsert(HeatMapKeys.accountId, accountId)
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
  public Map<CVMonitoringCategory, SortedSet<HeatMapDTO>> getHeatMap(String accountId, String projectIdentifier,
      String serviceIdentifier, String envIdentifier, Instant startTime, Instant endTime) {
    Map<CVMonitoringCategory, SortedSet<HeatMapDTO>> heatMaps = new HashMap<>();
    HeatMapResolution heatMapResolution = getHeatMapResolution(startTime, endTime);
    Instant startTimeBoundary = getBoundaryOfResolution(startTime, heatMapResolution.getResolution());
    Instant endTimeBoundary = getBoundaryOfResolution(endTime, heatMapResolution.getResolution());

    Set<CVMonitoringCategory> cvMonitoringCategories =
        cvConfigService.getAvailableCategories(accountId, projectIdentifier);
    for (CVMonitoringCategory category : cvMonitoringCategories) {
      Map<Instant, HeatMapDTO> heatMapsFromDB = getHeatMapsFromDB(
          projectIdentifier, serviceIdentifier, envIdentifier, category, startTime, endTime, heatMapResolution);

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

  @Override
  public List<EnvServiceRiskDTO> getEnvServiceRiskScores(
      String accountId, String orgIdentifier, String projectIdentifier) {
    List<EnvToServicesDTO> envToServicesDTOS =
        cvConfigService.getEnvToServicesList(accountId, orgIdentifier, projectIdentifier);
    Map<String, Set<String>> envToServicesMap = new HashMap<>();
    envToServicesDTOS.forEach(envToServicesDTO -> {
      if (envToServicesDTO.getEnvironment().getType().equals(EnvironmentType.Production)) {
        Set<String> services =
            envToServicesDTO.getServices().stream().map(ServiceResponseDTO::getIdentifier).collect(Collectors.toSet());
        envToServicesMap.put(envToServicesDTO.getEnvironment().getIdentifier(), services);
      }
    });
    List<EnvServiceRiskDTO> envServiceRiskDTOList = new ArrayList<>();
    envToServicesMap.forEach((envIdentifier, serviceSet) -> {
      Set<ServiceRisk> serviceRisks = new HashSet<>();
      serviceSet.forEach(service -> {
        Map<CVMonitoringCategory, Integer> categoryRisk =
            getCategoryRiskScoresForSpecificServiceEnv(accountId, projectIdentifier, service, envIdentifier);

        if (isNotEmpty(categoryRisk)) {
          Integer risk = Collections.max(categoryRisk.values());
          serviceRisks.add(ServiceRisk.builder().serviceIdentifier(service).risk(risk).build());
        }
      });
      if (isNotEmpty(serviceRisks)) {
        envServiceRiskDTOList.add(EnvServiceRiskDTO.builder()
                                      .envIdentifier(envIdentifier)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .serviceRisks(serviceRisks)
                                      .build());
      }
    });
    return envServiceRiskDTOList;
  }

  @Override
  public Map<CVMonitoringCategory, Integer> getCategoryRiskScores(@NotNull String accountId,
      @NotNull String orgIdentifier, @NotNull String projectIdentifier, String serviceIdentifier,
      String envIdentifier) {
    if (isNotEmpty(serviceIdentifier) && isEmpty(envIdentifier)) {
      throw new UnsupportedOperationException("Illeagal state in getCategoryRiskScores. EnvIdentifier is null but"
          + "serviceIdentifier is not null");
    }

    if (isEmpty(envIdentifier) && isEmpty(serviceIdentifier)) {
      serviceIdentifier = null;
      envIdentifier = null;
    } else if (isEmpty(serviceIdentifier)) {
      serviceIdentifier = null;
    }
    return getCategoryRiskScoresForSpecificServiceEnv(accountId, projectIdentifier, serviceIdentifier, envIdentifier);
  }

  private Map<CVMonitoringCategory, Integer> getCategoryRiskScoresForSpecificServiceEnv(
      @NotNull String accountId, @NotNull String projectIdentifier, String serviceIdentifier, String envIdentifier) {
    HeatMapResolution heatMapResolution = HeatMapResolution.FIVE_MIN;
    Map<CVMonitoringCategory, Integer> categoryScoreMap = new HashMap<>();
    Set<CVMonitoringCategory> cvMonitoringCategories =
        cvConfigService.getAvailableCategories(accountId, projectIdentifier);

    cvMonitoringCategories.forEach(category -> {
      Query<HeatMap> heatMapQuery = hPersistence.createQuery(HeatMap.class, excludeAuthority)
                                        .filter(HeatMapKeys.projectIdentifier, projectIdentifier)
                                        .filter(HeatMapKeys.category, category)
                                        .filter(HeatMapKeys.heatMapResolution, heatMapResolution)
                                        .filter(HeatMapKeys.envIdentifier, envIdentifier)
                                        .filter(HeatMapKeys.serviceIdentifier, serviceIdentifier)
                                        .order(Sort.descending(HeatMapKeys.heatMapBucketEndTime));

      HeatMap latestHeatMap = heatMapQuery.get();
      Instant roundedDownTime = roundDownTo5MinBoundary(clock.instant());
      if (latestHeatMap != null) {
        SortedSet<HeatMapRisk> risks = new TreeSet<>(latestHeatMap.getHeatMapRisks());
        if (risks.last().getEndTime().isAfter(roundedDownTime.minus(RISK_TIME_BUFFER_MINS, ChronoUnit.MINUTES))) {
          Double risk = risks.last().getRiskScore() * 100;
          categoryScoreMap.put(category, risk.intValue());
        }
      }
    });
    Arrays.asList(CVMonitoringCategory.values()).forEach(category -> {
      if (!categoryScoreMap.containsKey(category)) {
        categoryScoreMap.put(category, -1);
      }
    });
    return categoryScoreMap;
  }

  private Map<Instant, HeatMapDTO> getHeatMapsFromDB(String projectIdentifier, String serviceIdentifier,
      String envIdentifier, CVMonitoringCategory category, Instant startTime, Instant endTime,
      HeatMapResolution heatMapResolution) {
    Instant startTimeBucketBoundary = getBoundaryOfResolution(startTime, heatMapResolution.getBucketSize());
    Instant endTimeBucketBoundary = getBoundaryOfResolution(endTime, heatMapResolution.getBucketSize());
    Map<Instant, HeatMapDTO> heatMapDTOS = new HashMap<>();
    try (HIterator<HeatMap> heatMapRecords =
             new HIterator<>(hPersistence.createQuery(HeatMap.class, excludeAuthority)
                                 .filter(HeatMapKeys.projectIdentifier, projectIdentifier)
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
