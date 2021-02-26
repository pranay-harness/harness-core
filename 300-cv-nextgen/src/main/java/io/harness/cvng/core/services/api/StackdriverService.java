package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.TimeSeriesSampleDTO;
import io.harness.cvng.core.beans.stackdriver.StackdriverDashboardDTO;
import io.harness.cvng.core.beans.stackdriver.StackdriverDashboardDetail;
import io.harness.ng.beans.PageResponse;

import java.util.List;
import java.util.Set;

public interface StackdriverService extends MonitoringSourceImportStatusCreator, DataSourceConnectivityChecker {
  PageResponse<StackdriverDashboardDTO> listDashboards(String accountId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, int pageSize, int offset, String filter, String tracingId);
  List<StackdriverDashboardDetail> getDashboardDetails(String accountId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, String path, String tracingId);
  Set<TimeSeriesSampleDTO> getSampleData(String accountId, String connectorIdentifier, String orgIdentifier,
      String projectIdentifier, Object metricDefinitionDTO, String tracingId);
}
