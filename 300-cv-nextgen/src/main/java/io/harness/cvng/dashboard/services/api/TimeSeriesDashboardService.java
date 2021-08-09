package io.harness.cvng.dashboard.services.api;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.dashboard.beans.TimeSeriesMetricDataDTO;
import io.harness.ng.beans.PageResponse;

public interface TimeSeriesDashboardService {
  // TODO: Change this to a request body. This is too many query params.
  PageResponse<TimeSeriesMetricDataDTO> getSortedMetricData(String accountId, String projectIdentifier,
      String orgIdentifier, String environmentIdentifier, String serviceIdentifier,
      CVMonitoringCategory monitoringCategory, Long startTimeMillis, Long endTimeMillis, Long analysisStartTimeMillis,
      boolean anomalous, int page, int size, String filter, DataSourceType dataSourceType);

  PageResponse<TimeSeriesMetricDataDTO> getActivityMetrics(String activityId, String accountId,
      String projectIdentifier, String orgIdentifier, String environmentIdentifier, String serviceIdentifier,
      Long startTimeMillis, Long endTimeMillis, boolean anomalousOnly, int page, int size);
}
