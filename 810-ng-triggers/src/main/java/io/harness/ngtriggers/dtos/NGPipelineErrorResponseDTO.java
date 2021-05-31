package io.harness.ngtriggers.dtos;

import io.swagger.annotations.ApiModel;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel("NGPipelineErrorResponse")
public class NGPipelineErrorResponseDTO {
  @Builder.Default List<NGPipelineErrorDTO> errors = new ArrayList<>();
}
