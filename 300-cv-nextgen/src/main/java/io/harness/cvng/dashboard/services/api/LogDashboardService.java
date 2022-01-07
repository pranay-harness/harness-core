/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.dashboard.services.api;

import io.harness.cvng.analysis.beans.LiveMonitoringLogAnalysisClusterDTO;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.beans.params.TimeRangeParams;
import io.harness.cvng.core.beans.params.filterParams.LiveMonitoringLogAnalysisFilter;
import io.harness.cvng.dashboard.beans.AnalyzedLogDataDTO;
import io.harness.cvng.dashboard.beans.LogDataByTag;
import io.harness.ng.beans.PageResponse;

import java.time.Instant;
import java.util.List;
import java.util.SortedSet;

public interface LogDashboardService {
  @Deprecated
  PageResponse<AnalyzedLogDataDTO> getAnomalousLogs(String accountId, String projectIdentifier, String orgIdentifier,
      String serviceIdentifier, String environmentIdentifer, CVMonitoringCategory category, long startTimeMillis,
      long endTimeMillis, int page, int size);

  @Deprecated
  PageResponse<AnalyzedLogDataDTO> getAllLogs(String accountId, String projectIdentifier, String orgIdentifier,
      String serviceIdentifier, String environmentIdentifer, CVMonitoringCategory category, long startTimeMillis,
      long endTimeMillis, int page, int size);

  @Deprecated
  SortedSet<LogDataByTag> getLogCountByTag(String accountId, String projectIdentifier, String orgIdentifier,
      String serviceIdentifier, String environmentIdentifer, CVMonitoringCategory category, long startTimeMillis,
      long endTimeMillis);

  @Deprecated
  SortedSet<LogDataByTag> getLogCountByTagForActivity(String accountId, String projectIdentifier, String orgIdentifier,
      String activityId, Instant startTimeMillis, Instant endTimeMillis);

  @Deprecated
  PageResponse<AnalyzedLogDataDTO> getActivityLogs(String activityId, String accountId, String projectIdentifier,
      String orgIdentifier, String environmentIdentifier, String serviceIdentifier, Long startTimeMillis,
      Long endTimeMillis, boolean anomalousOnly, int page, int size);

  PageResponse<AnalyzedLogDataDTO> getAllLogsData(ServiceEnvironmentParams serviceEnvironmentParams,
      TimeRangeParams timeRangeParams, LiveMonitoringLogAnalysisFilter liveMonitoringLogAnalysisFilter,
      PageParams pageParams);

  List<LiveMonitoringLogAnalysisClusterDTO> getLogAnalysisClusters(ServiceEnvironmentParams serviceEnvironmentParams,
      TimeRangeParams timeRangeParams, LiveMonitoringLogAnalysisFilter liveMonitoringLogAnalysisFilter);
}
