package io.harness.cdng.pipeline.beans.dto;

import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CDPipelineRequestDTO {
  @JsonIgnore NgPipeline ngPipeline;
}
