package io.harness.beans;

import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class CIPipelineSetupParameters implements StepParameters {
  private CIPipeline ciPipeline;
  private Map<String, String> fieldToExecutionNodeIdMap;
}
