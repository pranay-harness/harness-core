package io.harness.cdng.pipeline.beans;

import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class CDPipelineSetupParameters implements StepParameters {
  private NgPipeline ngPipeline;
  private Map<String, String> fieldToExecutionNodeIdMap;
  private String inputSetPipelineYaml;
}
