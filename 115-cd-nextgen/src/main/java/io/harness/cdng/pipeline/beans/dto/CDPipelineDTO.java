package io.harness.cdng.pipeline.beans.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CDPipelineDTO {
  // Kept this dto class in case we need to support JSON as input
  private String name;
  private String description;
  @Singular private List<StageElementWrapper> stages;
  private String identifier;
  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore private String yamlPipeline;
}
