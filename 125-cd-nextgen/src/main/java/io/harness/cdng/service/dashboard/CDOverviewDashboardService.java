package io.harness.cdng.service.dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.Deployment.HealthDeploymentDashboard;

@OwnedBy(HarnessTeam.CDC)
public interface CDOverviewDashboardService {
  HealthDeploymentDashboard getHealthDeploymentDashboard(String accountId, String orgId, String projectId,
      String startInterval, String endInterval, String previousStartInterval);
}
