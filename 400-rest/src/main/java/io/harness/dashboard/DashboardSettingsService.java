/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.dashboard;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;

import javax.validation.constraints.NotNull;

public interface DashboardSettingsService {
  DashboardSettings createDashboardSettings(@NotNull String accountId, @NotNull DashboardSettings dashboardSettings);

  DashboardSettings updateDashboardSettings(@NotNull String accountId, @NotNull DashboardSettings dashboardSettings);

  DashboardSettings get(@NotNull String accountId, @NotNull String id);

  boolean doesPermissionsMatch(@NotNull DashboardSettings newDashboard, @NotNull DashboardSettings existingDashboard);

  boolean deleteDashboardSettings(@NotNull String accountId, @NotNull String id);

  PageResponse<DashboardSettings> getDashboardSettingSummary(
      @NotNull String accountId, @NotNull PageRequest pageRequest);
}
