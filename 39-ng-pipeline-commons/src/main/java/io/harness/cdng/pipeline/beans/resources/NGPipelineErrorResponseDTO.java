package io.harness.cdng.pipeline.beans.resources;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
@ApiModel("NGPipelineErrorResponse")
public class NGPipelineErrorResponseDTO {
  @Builder.Default List<NGPipelineErrorDTO> errors = new ArrayList<>();
}
