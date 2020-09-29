package io.harness.beans;

import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class CIPipelineSetupParameters implements StepParameters {
  private CDPipeline ciPipeline;
  private CIExecutionArgs ciExecutionArgs;
  private Map<String, String> fieldToExecutionNodeIdMap;
}
