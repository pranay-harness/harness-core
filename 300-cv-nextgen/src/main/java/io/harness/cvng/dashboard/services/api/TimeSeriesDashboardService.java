package io.harness.cvng.dashboard.services.api;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.dashboard.beans.TimeSeriesMetricDataDTO;
import io.harness.ng.beans.PageResponse;

public interface TimeSeriesDashboardService {
  PageResponse<TimeSeriesMetricDataDTO> getSortedMetricData(String accountId, String projectIdentifier,
      String orgIdentifier, String environmentIdentifier, String serviceIdentifier,
      CVMonitoringCategory monitoringCategory, Long startTimeMillis, Long endTimeMillis, int page, int size);
  PageResponse<TimeSeriesMetricDataDTO> getSortedAnomalousMetricData(String accountId, String projectIdentifier,
      String orgIdentifier, String environmentIdentifier, String serviceIdentifier,
      CVMonitoringCategory monitoringCategory, Long startTimeMillis, Long endTimeMillis, int page, int size);
}
