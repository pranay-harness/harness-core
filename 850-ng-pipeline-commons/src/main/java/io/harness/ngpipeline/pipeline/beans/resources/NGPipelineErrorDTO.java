package io.harness.ngpipeline.pipeline.beans.resources;

import io.harness.annotations.dev.ToBeDeleted;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel("NGPipelineError")
@ToBeDeleted
@Deprecated
public class NGPipelineErrorDTO {
  String fieldName;
  String message;
  String identifierOfErrorSource;
}
