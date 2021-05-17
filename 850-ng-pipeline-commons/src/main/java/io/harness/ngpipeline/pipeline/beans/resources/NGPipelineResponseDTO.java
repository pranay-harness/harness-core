package io.harness.ngpipeline.pipeline.beans.resources;

import io.harness.annotations.dev.ToBeDeleted;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("NGPipelineResponse")
@ToBeDeleted
@Deprecated
public class NGPipelineResponseDTO {
  NgPipeline ngPipeline;
  List<String> executionsPlaceHolder;
  private String yamlPipeline;
  @JsonIgnore Long version;
}
