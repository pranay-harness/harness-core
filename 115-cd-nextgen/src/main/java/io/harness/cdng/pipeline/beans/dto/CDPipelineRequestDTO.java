package io.harness.cdng.pipeline.beans.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.cdng.pipeline.CDPipeline;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CDPipelineRequestDTO {
  @JsonIgnore CDPipeline cdPipeline;
}
