package io.harness.app.intfc;

import io.harness.app.beans.dto.StepResourceMetricDTO;

import java.util.List;

public interface CIStepMetricsService {
  /**
   * Gets list of step metrics for each step in given pipeline
   * @param accountIdentifier account identifier
   * @param orgIdentifier organisation identifier
   * @param projectIdentifier project identifier
   * @param pipelineIdentifier pipeline identifier
   * @return list of step metrics
   */
  List<StepResourceMetricDTO> getCIStepMetrics(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineIdentifier);
}
