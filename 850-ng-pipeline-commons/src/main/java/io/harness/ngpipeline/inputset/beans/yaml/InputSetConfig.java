package io.harness.ngpipeline.inputset.beans.yaml;

import io.harness.annotations.dev.ToBeDeleted;
import io.harness.ngpipeline.inputset.beans.yaml.serializer.InputSetConfigSerializer;
import io.harness.ngpipeline.inputset.deserialiser.InputSetDeserializer;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.yaml.core.intfc.BaseInputSetConfig;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonSerialize(using = InputSetConfigSerializer.class)
@JsonDeserialize(using = InputSetDeserializer.class)
@ToBeDeleted
@Deprecated
public class InputSetConfig implements BaseInputSetConfig {
  @NotNull String identifier;
  String name;
  String description;
  @NotNull NgPipeline pipeline;

  Map<String, String> tags;
}
