package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.stackdriver.StackdriverDashboardDTO;
import io.harness.cvng.core.beans.stackdriver.StackdriverDashboardDetail;
import io.harness.ng.beans.PageResponse;

import java.util.List;

public interface StackdriverService {
  PageResponse<StackdriverDashboardDTO> listDashboards(String accountId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, int pageSize, int offset, String filter);
  List<StackdriverDashboardDetail> getDashboardDetails(
      String accountId, String connectorIdentifier, String orgIdentifier, String projectIdentifier, String path);
}
