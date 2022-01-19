/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.dashboard.services.api;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.beans.monitoredService.DurationDTO;
import io.harness.cvng.core.beans.monitoredService.HistoricalTrend;
import io.harness.cvng.core.beans.monitoredService.RiskData;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.utils.ServiceEnvKey;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;

public interface HeatMapService {
  void updateRiskScore(@NotNull String accountId, @NotNull String orgIdentifier, @NotNull String projectIdentifier,
      @NotNull String serviceIdentifier, @NotNull String envIdentifier, @NotNull CVConfig cvConfig,
      @NotNull CVMonitoringCategory category, @NotNull Instant timeStamp, double riskScore, long anomalousMetricsCount,
      long anomalousLogsCount);

  Map<ServiceEnvKey, RiskData> getLatestHealthScore(@NonNull ProjectParams projectParams,
      @NonNull List<String> serviceIdentifiers, @NonNull List<String> envIdentifiers);

  List<HistoricalTrend> getHistoricalTrend(String accountId, String orgIdentifier, String projectIdentifier,
      List<Pair<String, String>> serviceEnvIdentifiers, int hours);

  List<RiskData> getLatestRiskScoreForAllServicesList(String accountId, String orgIdentifier, String projectIdentifier,
      List<Pair<String, String>> serviceEnvIdentifiers);

  Map<ServiceEnvKey, RiskData> getLatestRiskScoreByServiceMap(
      ProjectParams projectParams, List<Pair<String, String>> serviceEnvIdentifiers);

  HistoricalTrend getOverAllHealthScore(ProjectParams projectParams, String serviceIdentifier,
      String environmentIdentifier, DurationDTO duration, Instant endTime);
}
