package io.harness.cdng.pipeline.beans;

import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.state.io.StepParameters;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CDPipelineSetupParameters implements StepParameters {
  private NgPipeline ngPipeline;
  private Map<String, String> fieldToExecutionNodeIdMap;
  private String inputSetPipelineYaml;
}
