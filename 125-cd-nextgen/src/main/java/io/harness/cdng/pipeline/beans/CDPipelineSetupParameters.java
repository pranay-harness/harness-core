package io.harness.cdng.pipeline.beans;

import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("cdPipelineSetupParameters")
public class CDPipelineSetupParameters implements StepParameters {
  private NgPipeline ngPipeline;
  private Map<String, String> fieldToExecutionNodeIdMap;
  private String inputSetPipelineYaml;
}
