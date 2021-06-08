package io.harness.analyserservice;

import io.harness.beans.alerts.AlertMetadata;
import io.harness.event.OverviewResponse;
import io.harness.event.QueryAlertCategory;
import io.harness.event.QueryStats;
import io.harness.repositories.QueryStatsRepository;
import io.harness.repositories.QueryStatsRepositoryCustomImpl;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AnalyserServiceImpl implements AnalyserService {
  @Inject QueryStatsRepository queryStatsRepository;
  @Inject QueryStatsRepositoryCustomImpl queryStatsRepositoryCustom;
  @Inject AnalyserServiceConfiguration analyserServiceConfiguration;

  @Override
  public List<QueryStats> getQueryStats(String service, String version) {
    return filterByServiceAndVersion(service, version);
  }

  @Override
  public List<QueryStats> getMostExpensiveQueries(String service, String version) {
    List<QueryStats> queries = filterByServiceAndVersion(service, version);
    return queries.stream()
        .filter(e
            -> e.getExplainResult().getExecutionStats().getExecutionTimeMillis()
                > analyserServiceConfiguration.getExecutionTimeLimitMillis())
        .sorted(this::compareQueryExecutionTimes)
        .collect(Collectors.toList());
  }

  // TODO: needs to be changed
  @Override
  public List<QueryStats> getQueryStats(String service, String version, QueryAlertCategory alertCategory) {
    List<QueryStats> queryStats = filterByServiceAndVersion(service, version);
    if (alertCategory == null) {
      return queryStats;
    }
    switch (alertCategory) {
      case SORT_STAGE:
        return queryStats.stream()
            .filter(
                e -> e.getAlerts().stream().anyMatch(o -> o.getAlertCategory().equals(QueryAlertCategory.SORT_STAGE)))
            .collect(Collectors.toList());
      case COLLSCAN:
        return queryStats.stream()
            .filter(e -> e.getAlerts().stream().anyMatch(o -> o.getAlertCategory().equals(QueryAlertCategory.COLLSCAN)))
            .collect(Collectors.toList());
      case SLOW_QUERY:
        return queryStats.stream()
            .filter(
                e -> e.getAlerts().stream().anyMatch(o -> o.getAlertCategory().equals(QueryAlertCategory.SLOW_QUERY)))
            .collect(Collectors.toList());
      case MANY_ENTRIES_EXAMINED:
        return queryStats.stream()
            .filter(e
                -> e.getAlerts().stream().anyMatch(
                    o -> o.getAlertCategory().equals(QueryAlertCategory.MANY_ENTRIES_EXAMINED)))
            .collect(Collectors.toList());
    }
    return queryStats;
  }

  @Override
  public List<QueryStats> getDisjointQueries(String service, String oldVersion, String newVersion) {
    List<QueryStats> queryStats = getQueryByService(service);
    List<QueryStats> oldQueryStats =
        queryStats.stream().filter(e -> e.getVersion().equals(oldVersion)).collect(Collectors.toList());
    List<QueryStats> newQueryStats =
        queryStats.stream().filter(e -> e.getVersion().equals(newVersion)).collect(Collectors.toList());
    newQueryStats.removeAll(oldQueryStats);
    return newQueryStats;
  }

  @Override
  public List<OverviewResponse> getOverview() {
    List<String> serviceNames = queryStatsRepositoryCustom.findAllServices();
    List<OverviewResponse> responses = new ArrayList<>();
    for (String serviceName : serviceNames) {
      // TODO: take latest version value
      List<QueryStats> queryStats = filterByServiceAndVersion(serviceName, "latestVersion");
      responses.add(
          OverviewResponse.builder()
              .serviceName(serviceName)
              .totalQueriesAnalysed(queryStats.size())
              .flaggedQueriesCount((int) queryStats.stream().filter(e -> checkNotEmpty(e.getAlerts())).count())
              .build());
    }
    return responses;
  }

  public List<QueryStats> getQueryByService(String serviceName) {
    return ((List<QueryStats>) queryStatsRepository.findAll())
        .stream()
        .filter(e -> e.getServiceName().equals(serviceName))
        .collect(Collectors.toList());
  }

  List<QueryStats> filterByServiceAndVersion(String serviceName, String version) {
    return ((List<QueryStats>) queryStatsRepository.findAll())
        .stream()
        .filter(e -> e.getServiceName().equals(serviceName) && e.getVersion().equals(version))
        .collect(Collectors.toList());
  }

  int compareQueryExecutionTimes(QueryStats a, QueryStats b) {
    return (int) (a.getExecutionTimeMillis() - b.getExecutionTimeMillis());
  }
  boolean checkNotEmpty(List<AlertMetadata> list) {
    return list != null && list.size() > 0;
  }
}
