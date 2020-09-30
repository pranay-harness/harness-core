package io.harness.cdng.inputset.beans.resource;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("MergeInputSetResponse")
public class MergeInputSetResponseDTO {
  String pipelineYaml;

  @ApiModelProperty(name = "isErrorResponse") boolean isErrorResponse;
  InputSetErrorWrapperDTO inputSetErrorWrapper;
}
