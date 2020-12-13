package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.stackdriver.StackdriverDashboardDTO;
import io.harness.ng.beans.PageResponse;

public interface StackdriverService {
  PageResponse<StackdriverDashboardDTO> listDashboards(String accountId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, int pageSize, int offset, String filter);
}
