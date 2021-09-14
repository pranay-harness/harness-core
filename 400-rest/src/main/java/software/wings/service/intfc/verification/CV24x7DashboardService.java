/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.verification;

import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.verification.HeatMap;

import java.util.List;
import java.util.Map;

public interface CV24x7DashboardService {
  List<HeatMap> getHeatMapForLogs(
      String accountId, String appId, String serviceId, long startTime, long endTime, boolean detailed);
  LogMLAnalysisSummary getAnalysisSummary(String cvConfigId, Long startTime, Long endTime, String appId);
  Map<String, Double> getMetricTags(String accountId, String appId, String cvConfigId, long startTime, long endTIme);
  long getCurrentAnalysisWindow(String cvConfigId);
}
