package io.harness.beans;

import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.state.io.StepParameters;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("ciPipelineSetupParameters")
public class CIPipelineSetupParameters implements StepParameters {
  private NgPipeline ngPipeline;
  private CIExecutionArgs ciExecutionArgs;
  private Map<String, String> fieldToExecutionNodeIdMap;
}
