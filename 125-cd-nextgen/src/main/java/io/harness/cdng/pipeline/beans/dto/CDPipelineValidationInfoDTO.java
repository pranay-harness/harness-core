package io.harness.cdng.pipeline.beans.dto;

import io.harness.walktree.visitor.ErrorResponseWrapper;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class CDPipelineValidationInfoDTO {
  String pipelineYaml;
  @ApiModelProperty(name = "isErrorResponse") boolean isErrorResponse;
  Map<String, ErrorResponseWrapper> uuidToErrorResponseMap;
}
