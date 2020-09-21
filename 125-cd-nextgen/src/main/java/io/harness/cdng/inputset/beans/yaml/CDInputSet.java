package io.harness.cdng.inputset.beans.yaml;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.harness.cdng.inputset.deserialiser.CDInputSetDeserializer;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.yaml.core.intfc.InputSet;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonDeserialize(using = CDInputSetDeserializer.class)
public class CDInputSet implements InputSet {
  String identifier;
  String name;
  String description;
  CDPipeline pipeline;

  // Add tags
}
