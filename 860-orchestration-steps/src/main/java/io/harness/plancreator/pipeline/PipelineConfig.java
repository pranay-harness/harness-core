package io.harness.plancreator.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.YamlDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PIPELINE)
@Value
@Builder
@TypeAlias("pipelineConfig")
public class PipelineConfig implements YamlDTO {
  @JsonProperty("pipeline") PipelineInfoConfig pipelineInfoConfig;
}
