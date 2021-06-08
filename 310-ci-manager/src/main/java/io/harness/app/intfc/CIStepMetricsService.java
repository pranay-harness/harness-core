package io.harness.app.intfc;

import io.harness.app.beans.dto.StepResourceMetricDTO;

import java.util.List;

public interface CIStepMetricsService {
  List<StepResourceMetricDTO> getCIStepMetrics(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineIdentifier);
}
