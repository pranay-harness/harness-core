package io.harness.cvng.dashboard.services.api;

import io.harness.cvng.core.beans.CVMonitoringCategory;
import io.harness.cvng.core.dashboard.beans.HeatMapDTO;

import java.time.Instant;
import java.util.Map;
import java.util.SortedSet;
import javax.validation.constraints.NotNull;

public interface HeatMapService {
  void updateRiskScore(@NotNull String accountId, @NotNull String serviceIdentifier, @NotNull String envIdentifier,
      @NotNull CVMonitoringCategory category, @NotNull Instant timeStamp, double riskScore);

  Map<CVMonitoringCategory, SortedSet<HeatMapDTO>> getHeatMap(@NotNull String accountId,
      @NotNull String serviceIdentifier, @NotNull String envIdentifier, @NotNull Instant startTime,
      @NotNull Instant endTime);
}
