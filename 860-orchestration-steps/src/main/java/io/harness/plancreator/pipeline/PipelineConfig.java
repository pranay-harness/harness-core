package io.harness.plancreator.pipeline;

import io.harness.EntityType;
import io.harness.yamlSchema.YamlSchemaRoot;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("pipelineConfig")
@YamlSchemaRoot(EntityType.PIPELINES)
public class PipelineConfig {
  @JsonProperty("pipeline") PipelineInfoConfig pipelineInfoConfig;
}
