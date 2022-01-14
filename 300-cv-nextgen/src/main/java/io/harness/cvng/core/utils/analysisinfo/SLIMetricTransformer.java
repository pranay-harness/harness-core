package io.harness.cvng.core.utils.analysisinfo;

import io.harness.cvng.core.beans.HealthSourceMetricDefinition.SLIDTO;
import io.harness.cvng.core.entities.AnalysisInfo.SLI;

import java.util.Objects;

public class SLIMetricTransformer {
  // TODO This needs to be moved to the PrometheusHealthSourceSpecTransformer while refactoring DTO to entity transfer
  // of PrometheusCVConfig
  public static SLI transformDTOtoEntity(SLIDTO sliDto) {
    if (Objects.nonNull(sliDto)) {
      return SLI.builder().enabled(sliDto.getEnabled()).build();
    }
    return null;
  }
}
