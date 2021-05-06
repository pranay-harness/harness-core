package io.harness.ngpipeline.pipeline.beans.resources;

import io.harness.annotations.dev.ToBeDeleted;

import io.swagger.annotations.ApiModel;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel("NGPipelineErrorResponse")
@ToBeDeleted
@Deprecated
public class NGPipelineErrorResponseDTO {
  @Builder.Default List<NGPipelineErrorDTO> errors = new ArrayList<>();
}
