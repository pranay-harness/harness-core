package io.harness.cvng.dashboard.services.api;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.beans.params.TimeRangeParams;
import io.harness.cvng.dashboard.beans.TimeSeriesMetricDataDTO;
import io.harness.ng.beans.PageResponse;

import java.util.List;

public interface TimeSeriesDashboardService {
  // TODO: Change this to a request body. This is too many query params. Also change the order of parameters to be
  // consistent with other methods.
  @Deprecated
  PageResponse<TimeSeriesMetricDataDTO> getSortedMetricData(String accountId, String projectIdentifier,
      String orgIdentifier, String environmentIdentifier, String serviceIdentifier,
      CVMonitoringCategory monitoringCategory, Long startTimeMillis, Long endTimeMillis, Long analysisStartTimeMillis,
      boolean anomalous, int page, int size, String filter, DataSourceType dataSourceType);

  PageResponse<TimeSeriesMetricDataDTO> getActivityMetrics(String activityId, String accountId,
      String projectIdentifier, String orgIdentifier, String environmentIdentifier, String serviceIdentifier,
      Long startTimeMillis, Long endTimeMillis, boolean anomalousOnly, int page, int size);

  PageResponse<TimeSeriesMetricDataDTO> getTimeSeriesMetricData(ServiceEnvironmentParams serviceEnvironmentParams,
      TimeRangeParams timeRangeParams, boolean anomalous, List<String> healthSourceIdentifiersFilter, String filter,
      PageParams pageParams);
}
