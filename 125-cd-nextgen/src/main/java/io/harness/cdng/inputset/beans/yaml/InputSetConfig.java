package io.harness.cdng.inputset.beans.yaml;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.harness.cdng.inputset.deserialiser.InputSetDeserializer;
import io.harness.cdng.pipeline.NgPipeline;
import io.harness.yaml.core.intfc.BaseInputSetConfig;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonDeserialize(using = InputSetDeserializer.class)
public class InputSetConfig implements BaseInputSetConfig {
  @NotNull String identifier;
  String name;
  String description;
  @NotNull NgPipeline pipeline;

  // Add tags
}
