package io.harness.cdng.pipeline.beans.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.cdng.pipeline.CDPipeline;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CDPipelineResponseDTO {
  CDPipeline cdPipeline;
  List<String> executionsPlaceHolder;
  private String yamlPipeline;
}
