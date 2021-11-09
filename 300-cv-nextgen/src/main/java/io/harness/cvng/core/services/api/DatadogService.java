package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.TimeSeriesSampleDTO;
import io.harness.cvng.core.beans.datadog.DatadogDashboardDTO;
import io.harness.cvng.core.beans.datadog.DatadogDashboardDetail;
import io.harness.ng.beans.PageResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public interface DatadogService {

    PageResponse<DatadogDashboardDTO> getAllDashboards(String accountId, String connectorIdentifier,
                                                       String orgIdentifier, String projectIdentifier,
                                                       int pageSize, int offset, String filter, String tracingId);

    List<DatadogDashboardDetail> getDashboardDetails(String dashboardId, String accountId, String connectorIdentifier, String orgIdentifier,
                                                     String projectIdentifier, String tracingId);

    List<String> getMetricTagsList(String metricName, String accountId, String connectorIdentifier, String orgIdentifier,
                                   String projectIdentifier, String tracingId);

    List<String> getActiveMetrics(String accountId, String connectorIdentifier, String orgIdentifier,
                                  String projectIdentifier, String tracingId);

    List<TimeSeriesSampleDTO> getTimeSeriesPoints(String accountId, String connectorIdentifier, String orgIdentifier,
                                                  String projectIdentifier, String query, String tracingId);

    List<LinkedHashMap> getSampleLogData(String accountId, String connectorIdentifier, String orgIdentifier,
                                         String projectIdentifier, String query, String tracingId);
}
