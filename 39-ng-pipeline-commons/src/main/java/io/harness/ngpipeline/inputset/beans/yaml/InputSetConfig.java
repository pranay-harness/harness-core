package io.harness.ngpipeline.inputset.beans.yaml;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.harness.ngpipeline.inputset.beans.yaml.serializer.InputSetConfigSerializer;
import io.harness.ngpipeline.inputset.deserialiser.InputSetDeserializer;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.yaml.core.intfc.BaseInputSetConfig;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
@Builder
@JsonSerialize(using = InputSetConfigSerializer.class)
@JsonDeserialize(using = InputSetDeserializer.class)
public class InputSetConfig implements BaseInputSetConfig {
  @NotNull String identifier;
  String name;
  String description;
  @NotNull NgPipeline pipeline;

  Map<String, String> tags;
}
