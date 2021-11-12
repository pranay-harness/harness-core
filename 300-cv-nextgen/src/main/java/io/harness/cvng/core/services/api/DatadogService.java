package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.TimeSeriesSampleDTO;
import io.harness.cvng.core.beans.datadog.DatadogDashboardDTO;
import io.harness.cvng.core.beans.datadog.DatadogDashboardDetail;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.ng.beans.PageResponse;

import java.util.LinkedHashMap;
import java.util.List;

public interface DatadogService {
  PageResponse<DatadogDashboardDTO> getAllDashboards(ProjectParams projectParams, String connectorIdentifier,
      int pageSize, int offset, String filter, String tracingId);

  List<DatadogDashboardDetail> getDashboardDetails(
      ProjectParams projectParams, String connectorIdentifier, String dashboardId, String tracingId);

  List<String> getMetricTagsList(
      ProjectParams projectParams, String connectorIdentifier, String metricName, String tracingId);

  List<String> getActiveMetrics(ProjectParams projectParams, String connectorIdentifier, String tracingId);

  List<TimeSeriesSampleDTO> getTimeSeriesPoints(
      ProjectParams projectParams, String connectorIdentifier, String query, String tracingId);

  List<LinkedHashMap> getSampleLogData(
      ProjectParams projectParams, String connectorIdentifier, String query, String tracingId);

  List<String> getLogIndexes(ProjectParams projectParams, String connectorIdentifier, String tracingId);
}
