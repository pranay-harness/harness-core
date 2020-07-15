package io.harness.cvng.dashboard.services.api;

import io.harness.cvng.core.beans.CVMonitoringCategory;
import io.harness.cvng.dashboard.beans.AnomalyDTO;
import io.harness.cvng.dashboard.entities.Anomaly.AnomalousMetric;

import java.time.Instant;
import java.util.List;
import javax.validation.constraints.NotNull;

public interface AnomalyService {
  void openAnomaly(@NotNull String accountId, @NotNull String cvConfigId, @NotNull Instant anomalyTimestamp,
      @NotNull List<AnomalousMetric> anomalousMetrics);

  void closeAnomaly(@NotNull String accountId, @NotNull String cvConfigId, @NotNull Instant anomalyTimeStamp);

  List<AnomalyDTO> getAnomalies(@NotNull String accountId, @NotNull String serviceIdentifier,
      @NotNull String envIdentifier, @NotNull CVMonitoringCategory category, @NotNull Instant startTime,
      @NotNull Instant endTime);
}
