package io.harness.cvng.core.utils.analysisinfo;

import io.harness.cvng.core.beans.HealthSourceMetricDefinition;
import io.harness.cvng.core.entities.AnalysisInfo.LiveMonitoring;

import java.util.Objects;

public class LiveMonitoringTransformer {
  // TODO This needs to be moved to the PrometheusHealthSourceSpecTransformer while refactoring DTO to entity transfer
  // of PrometheusCVConfig
  public static LiveMonitoring transformDTOtoEntity(HealthSourceMetricDefinition.AnalysisDTO analysisDTO) {
    if (Objects.nonNull(analysisDTO) && Objects.nonNull(analysisDTO.getLiveMonitoring())) {
      return LiveMonitoring.builder().enabled(analysisDTO.getLiveMonitoring().getEnabled()).build();
    }
    return null;
  }
}
