package io.harness.cdng.pipeline.beans;

import io.harness.cdng.pipeline.CDPipeline;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class CDPipelineSetupParameters implements StepParameters {
  private CDPipeline cdPipeline;
  private Map<String, String> fieldToExecutionNodeIdMap;
}
