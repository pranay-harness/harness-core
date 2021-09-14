/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.dashboard.services.api;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.beans.monitoredService.DurationDTO;
import io.harness.cvng.core.beans.monitoredService.HistoricalTrend;
import io.harness.cvng.core.beans.monitoredService.RiskData;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.dashboard.beans.CategoryRisksDTO;
import io.harness.cvng.dashboard.beans.EnvServiceRiskDTO;
import io.harness.cvng.dashboard.beans.HeatMapDTO;
import io.harness.cvng.dashboard.beans.RiskSummaryPopoverDTO;
import io.harness.cvng.dashboard.entities.HeatMap;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;

public interface HeatMapService {
  void updateRiskScore(@NotNull String accountId, @NotNull String orgIdentifier, @NotNull String projectIdentifier,
      @NotNull String serviceIdentifier, @NotNull String envIdentifier, @NotNull CVConfig cvConfig,
      @NotNull CVMonitoringCategory category, @NotNull Instant timeStamp, double riskScore, long anomalousMetricsCount,
      long anomalousLogsCount);

  Map<CVMonitoringCategory, SortedSet<HeatMapDTO>> getHeatMap(@NotNull String accountId, @NotNull String orgIdentifier,
      @NotNull String projectIdentifier, String serviceIdentifier, String envIdentifier, @NotNull Instant startTime,
      @NotNull Instant endTime);

  CategoryRisksDTO getCategoryRiskScores(@NotNull String accountId, @NotNull String orgIdentifier,
      @NotNull String projectIdentifier, String serviceIdentifier, String envIdentifier);

  List<EnvServiceRiskDTO> getEnvServiceRiskScores(
      @NotNull String accountId, @NotNull String orgIdentifier, @NotNull String projectIdentifier);

  RiskSummaryPopoverDTO getRiskSummaryPopover(String accountId, String orgIdentifier, String projectIdentifier,
      Instant endTime, String serviceIdentifier, CVMonitoringCategory category);

  List<HeatMap> getLatestHeatMaps(
      @NonNull ProjectParams projectParams, @Nullable String serviceIdentifier, @Nullable String envIdentifier);

  List<HistoricalTrend> getHistoricalTrend(String accountId, String orgIdentifier, String projectIdentifier,
      List<Pair<String, String>> serviceEnvIdentifiers, int hours);

  List<RiskData> getLatestRiskScore(String accountId, String orgIdentifier, String projectIdentifier,
      List<Pair<String, String>> serviceEnvIdentifiers, Duration bufferTime);

  HistoricalTrend getOverAllHealthScore(ProjectParams projectParams, String serviceIdentifier,
      String environmentIdentifier, DurationDTO duration, Instant endTime);
}
