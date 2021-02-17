package io.harness.plancreator.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("pipelineConfig")
public class PipelineConfig {
  @JsonProperty("pipeline") PipelineInfoConfig pipelineInfoConfig;
}
