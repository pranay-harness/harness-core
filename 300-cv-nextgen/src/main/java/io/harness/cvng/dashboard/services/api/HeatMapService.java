package io.harness.cvng.dashboard.services.api;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.dashboard.beans.EnvServiceRiskDTO;
import io.harness.cvng.dashboard.beans.HeatMapDTO;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import javax.validation.constraints.NotNull;

public interface HeatMapService {
  void updateRiskScore(@NotNull String accountId, @NotNull String projectIdentifier, @NotNull String serviceIdentifier,
      @NotNull String envIdentifier, @NotNull CVMonitoringCategory category, @NotNull Instant timeStamp,
      double riskScore);

  Map<CVMonitoringCategory, SortedSet<HeatMapDTO>> getHeatMap(@NotNull String accountId,
      @NotNull String projectIdentifier, String serviceIdentifier, String envIdentifier, @NotNull Instant startTime,
      @NotNull Instant endTime);

  Map<CVMonitoringCategory, Integer> getCategoryRiskScores(@NotNull String accountId, @NotNull String orgIdentifier,
      @NotNull String projectIdentifier, String serviceIdentifier, String envIdentifier);

  List<EnvServiceRiskDTO> getEnvServiceRiskScores(
      @NotNull String accountId, @NotNull String orgIdentifier, @NotNull String projectIdentifier);
}
