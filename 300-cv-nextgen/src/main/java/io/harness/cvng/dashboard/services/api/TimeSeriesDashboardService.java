package io.harness.cvng.dashboard.services.api;

import io.harness.beans.NGPageResponse;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.dashboard.beans.TimeSeriesMetricDataDTO;

public interface TimeSeriesDashboardService {
  NGPageResponse<TimeSeriesMetricDataDTO> getSortedMetricData(String accountId, String projectIdentifier,
      String orgIdentifier, String environmentIdentifier, String serviceIdentifier,
      CVMonitoringCategory monitoringCategory, Long startTimeMillis, Long endTimeMillis, int page, int size);
  NGPageResponse<TimeSeriesMetricDataDTO> getSortedAnomalousMetricData(String accountId, String projectIdentifier,
      String orgIdentifier, String environmentIdentifier, String serviceIdentifier,
      CVMonitoringCategory monitoringCategory, Long startTimeMillis, Long endTimeMillis, int page, int size);
}
