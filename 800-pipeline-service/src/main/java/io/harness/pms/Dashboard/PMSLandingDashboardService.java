package io.harness.pms.Dashboard;

import io.harness.ng.core.OrgProjectIdentifier;
import io.harness.pms.dashboards.PipelinesCount;

import java.util.List;

public interface PMSLandingDashboardService {
  PipelinesCount getPipelinesCount(
      String accountIdentifier, List<OrgProjectIdentifier> orgProjectIdentifiers, long startInterval, long endInterval);
}
