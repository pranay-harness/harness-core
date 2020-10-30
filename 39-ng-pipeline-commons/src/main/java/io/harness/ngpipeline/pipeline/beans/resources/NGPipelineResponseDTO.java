package io.harness.ngpipeline.pipeline.beans.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("NGPipelineResponse")
public class NGPipelineResponseDTO {
  NgPipeline ngPipeline;
  List<String> executionsPlaceHolder;
  private String yamlPipeline;
  @JsonIgnore Long version;
}
