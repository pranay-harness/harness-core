package io.harness.analyserservice;

import io.harness.event.OverviewResponse;
import io.harness.event.QueryAlertCategory;
import io.harness.event.QueryStats;

import java.util.List;
import java.util.Map;

public interface AnalyserService {
  List<QueryStats> getQueryStats(String service, String version);
  Map<String, Integer> getAlertMap(String service, QueryAlertCategory alertCategory);
  List<QueryStats> getMostExpensiveQueries(String service, String version);
  List<QueryStats> getQueryStats(String service, String version, QueryAlertCategory alertCategory);
  List<QueryStats> getDisjointQueries(String service, String oldVersion, String newVersion);
  List<OverviewResponse> getOverview();
  List<QueryStats> getNewQueriesInLatestVersion(String serviceName);
}
