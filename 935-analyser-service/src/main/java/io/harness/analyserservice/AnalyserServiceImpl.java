package io.harness.analyserservice;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.beans.alerts.AlertMetadata;
import io.harness.beans.alerts.AlertMetadata.AlertMetadataKeys;
import io.harness.event.OverviewResponse;
import io.harness.event.QueryAlertCategory;
import io.harness.event.QueryStats;
import io.harness.event.QueryStats.QueryStatsKeys;
import io.harness.repositories.QueryStatsRepository;
import io.harness.serviceinfo.ServiceInfo;
import io.harness.serviceinfo.ServiceInfoService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

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
    newQueryStats.removeAll(oldQueryStats);
    return newQueryStats;
  }

  @Override
  public List<OverviewResponse> getOverview() {
    List<ServiceInfo> serviceInfos = serviceInfoService.getAllServices();
    List<OverviewResponse> responses = new ArrayList<>();
    for (ServiceInfo serviceInfo : serviceInfos) {
      List<QueryStats> queryStats =
          queryStatsRepository.findByServiceIdAndVersion(serviceInfo.getServiceId(), serviceInfo.getLatestVersion());
      responses.add(
          OverviewResponse.builder()
              .serviceName(serviceInfo.getServiceId())
              .totalQueriesAnalysed(queryStats.size())
              .flaggedQueriesCount((int) queryStats.stream().filter(e -> checkNotEmpty(e.getAlerts())).count())
              .build());
    }
    return responses;
  }
  boolean checkNotEmpty(List<AlertMetadata> list) {
    return list != null && list.size() > 0;
  }
}
