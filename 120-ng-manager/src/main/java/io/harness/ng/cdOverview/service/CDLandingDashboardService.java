package io.harness.ng.cdOverview.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.dashboards.EnvCount;
import io.harness.dashboards.ProjectsDashboardInfo;
import io.harness.dashboards.ServicesCount;
import io.harness.dashboards.ServicesDashboardInfo;
import io.harness.dashboards.SortBy;
import io.harness.ng.core.OrgProjectIdentifier;

import java.util.List;
import javax.validation.constraints.NotNull;

@OwnedBy(PIPELINE)
public interface CDLandingDashboardService {
  ServicesDashboardInfo getActiveServices(@NotNull String accountIdentifier,
      @NotNull List<OrgProjectIdentifier> orgProjectIdentifiers, long startInterval, long endInterval,
      @NotNull SortBy sortBy);

  ProjectsDashboardInfo getTopProjects(
      String accountIdentifier, List<OrgProjectIdentifier> orgProjectIdentifiers, long startInterval, long endInterval);

  ServicesCount getServicesCount(
      String accountIdentifier, List<OrgProjectIdentifier> orgProjectIdentifiers, long startInterval, long endInterval);

  EnvCount getEnvCount(
      String accountIdentifier, List<OrgProjectIdentifier> orgProjectIdentifiers, long startInterval, long endInterval);
}
