/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.analyserservice;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.alerts.AlertMetadata;
import io.harness.beans.alerts.AlertMetadata.AlertMetadataKeys;
import io.harness.event.QueryAlertCategory;
import io.harness.event.QueryStats;
import io.harness.event.QueryStats.QueryStatsKeys;
import io.harness.repositories.QueryStatsRepository;
import io.harness.serviceinfo.ServiceInfoService;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.PIPELINE)
public class AnalyserServiceImpl implements AnalyserService {
  @Inject QueryStatsRepository queryStatsRepository;
  @Inject AnalyserServiceConfiguration analyserServiceConfiguration;
  @Inject ServiceInfoService serviceInfoService;
  @Inject MongoTemplate mongoTemplate;

  @Override
  public List<QueryStats> getQueryStats(String service, String version) {
    return queryStatsRepository.findByServiceIdAndVersion(service, version);
  }

  @Override
  public Map<String, Integer> getAlertMap(String service, QueryAlertCategory alertCategory) {
    List<String> versions = serviceInfoService.getAllVersions(service);
    Map<String, Integer> response = new HashMap<>();
    for (String version : versions) {
      response.put(version, getQueryStats(service, version, alertCategory).size());
    }
    return response;
  }

  @Override
  public List<QueryStats> getMostExpensiveQueries(String service, String version) {
    return queryStatsRepository
        .findByServiceIdAndVersionAndExecutionTimeMillisGreaterThanOrderByExecutionTimeMillisDesc(
            service, version, analyserServiceConfiguration.getExecutionTimeLimitMillis());
  }

  @Override
  public List<QueryStats> getQueryStats(
      @NonNull String service, @NonNull String version, @NonNull QueryAlertCategory alertCategory) {
    Query query =
        query(where(QueryStatsKeys.serviceId).is(service))
            .addCriteria(where(QueryStatsKeys.version).is(version))
            .addCriteria(where(QueryStatsKeys.alerts + "." + AlertMetadataKeys.alertCategory).is(alertCategory));
    return mongoTemplate.find(query, QueryStats.class);
  }

  @Override
  public List<QueryStats> getDisjointQueries(String service, String oldVersion, String newVersion) {
    List<QueryStats> oldQueryStats = queryStatsRepository.findByServiceIdAndVersion(service, oldVersion);
    List<QueryStats> newQueryStats = queryStatsRepository.findByServiceIdAndVersion(service, newVersion);

    Set<String> oldHashes = oldQueryStats.stream().map(s -> s.getHash()).collect(Collectors.toSet());
    return newQueryStats.stream().filter(n -> !oldHashes.contains(n.getHash())).collect(Collectors.toList());
  }

  boolean checkNotEmpty(List<AlertMetadata> list) {
    return list != null && list.size() > 0;
  }
}
