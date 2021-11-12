package io.harness.cvng.core.utils.analysisinfo;

import io.harness.cvng.core.beans.HealthSourceMetricDefinition.AnalysisDTO;
import io.harness.cvng.core.entities.AnalysisInfo.DeploymentVerification;

import java.util.Objects;

public class DevelopmentVerificationTransformer {
  // TODO This needs to be moved to the PrometheusHealthSourceSpecTransformer while refactoring DTO to entity transfer
  // of PrometheusCVConfig
  public static DeploymentVerification transformDTOtoEntity(AnalysisDTO analysisDTO) {
    if (Objects.nonNull(analysisDTO) && Objects.nonNull(analysisDTO.getDeploymentVerification())) {
      return DeploymentVerification.builder().enabled(analysisDTO.getDeploymentVerification().getEnabled()).build();
    }
    return null;
  }
}
