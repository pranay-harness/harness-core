package io.harness.cvng.dashboard.services.api;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.beans.monitoredService.HistoricalTrend;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.dashboard.beans.CategoryRisksDTO;
import io.harness.cvng.dashboard.beans.EnvServiceRiskDTO;
import io.harness.cvng.dashboard.beans.HeatMapDTO;
import io.harness.cvng.dashboard.beans.RiskSummaryPopoverDTO;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.tuple.Pair;

public interface HeatMapService {
  void updateRiskScore(@NotNull String accountId, @NotNull String orgIdentifier, @NotNull String projectIdentifier,
      @NotNull String serviceIdentifier, @NotNull String envIdentifier, @NotNull CVConfig cvConfig,
      @NotNull CVMonitoringCategory category, @NotNull Instant timeStamp, double riskScore);

  Map<CVMonitoringCategory, SortedSet<HeatMapDTO>> getHeatMap(@NotNull String accountId, @NotNull String orgIdentifier,
      @NotNull String projectIdentifier, String serviceIdentifier, String envIdentifier, @NotNull Instant startTime,
      @NotNull Instant endTime);

  CategoryRisksDTO getCategoryRiskScores(@NotNull String accountId, @NotNull String orgIdentifier,
      @NotNull String projectIdentifier, String serviceIdentifier, String envIdentifier);

  List<EnvServiceRiskDTO> getEnvServiceRiskScores(
      @NotNull String accountId, @NotNull String orgIdentifier, @NotNull String projectIdentifier);

  RiskSummaryPopoverDTO getRiskSummaryPopover(String accountId, String orgIdentifier, String projectIdentifier,
      Instant endTime, String serviceIdentifier, CVMonitoringCategory category);

  List<HistoricalTrend> getHistoricalTrend(String accountId, String orgIdentifier, String projectIdentifier,
      List<Pair<String, String>> serviceIdentifiers, int hours);
}
