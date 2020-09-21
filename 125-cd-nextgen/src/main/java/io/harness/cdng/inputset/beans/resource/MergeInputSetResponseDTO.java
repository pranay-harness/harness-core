package io.harness.cdng.inputset.beans.resource;

import io.harness.walktree.visitor.ErrorResponseWrapper;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MergeInputSetResponseDTO {
  String pipelineYaml;
  @ApiModelProperty(name = "isErrorResponse") boolean isErrorResponse;
  Map<String, ErrorResponseWrapper> uuidToErrorResponseMap;
}
