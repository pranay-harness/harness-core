package io.harness.cdng.pipeline.beans;

import io.harness.cdng.pipeline.CDPipeline;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CDPipelineSetupParameters implements StepParameters {
  private CDPipeline cdPipeline;
}
