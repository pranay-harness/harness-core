package io.harness.ngtriggers.dtos;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel("NGPipelineError")
public class NGPipelineErrorDTO {
  String fieldName;
  String message;
  String identifierOfErrorSource;
}
