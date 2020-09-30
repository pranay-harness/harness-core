package io.harness.cdng.pipeline.beans.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.walktree.visitor.response.VisitorErrorResponseWrapper;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("NGPipelineValidationInfo")
public class CDPipelineValidationInfoDTO {
  String pipelineYaml;
  @ApiModelProperty(name = "isErrorResponse") boolean isErrorResponse;
  Map<String, VisitorErrorResponseWrapper> uuidToErrorResponseMap;
}
