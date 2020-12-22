package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.StackdriverSampleDataDTO;
import io.harness.cvng.core.beans.stackdriver.StackdriverDashboardDTO;
import io.harness.cvng.core.beans.stackdriver.StackdriverDashboardDetail;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;

import java.util.List;

public interface StackdriverService {
  PageResponse<StackdriverDashboardDTO> listDashboards(String accountId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, int pageSize, int offset, String filter, String tracingId);
  List<StackdriverDashboardDetail> getDashboardDetails(String accountId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, String path, String tracingId);
  ResponseDTO<StackdriverSampleDataDTO> getSampleData(String accountId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, Object metricDefinitionDTO, String tracingId);
}
