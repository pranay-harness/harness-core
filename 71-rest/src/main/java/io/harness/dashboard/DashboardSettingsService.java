package io.harness.dashboard;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;

import javax.validation.constraints.NotNull;

public interface DashboardSettingsService {
  DashboardSettings createDashboardSettings(@NotNull String accountId, @NotNull DashboardSettings dashboardSettings);

  DashboardSettings updateDashboardSettings(@NotNull String accountId, @NotNull DashboardSettings dashboardSettings);

  DashboardSettings get(@NotNull String accountId, @NotNull String id);

  boolean deleteDashboardSettings(@NotNull String accountId, @NotNull String id);

  PageResponse<DashboardSettings> getDashboardSettingSummary(
      @NotNull String accountId, @NotNull PageRequest pageRequest);
}
