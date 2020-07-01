package io.harness.cvng.core.dashboard.services.api;

import io.harness.cvng.core.beans.CVMonitoringCategory;

import javax.validation.constraints.NotNull;

public interface HeatMapService {
  void updateRiskScore(@NotNull String accountId, @NotNull String serviceIdentifier, @NotNull String envIdentifier,
      @NotNull CVMonitoringCategory category, long timeStamp, double riskScore);
}
