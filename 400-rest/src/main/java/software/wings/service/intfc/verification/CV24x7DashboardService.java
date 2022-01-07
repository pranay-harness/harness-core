/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
