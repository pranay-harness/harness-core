package io.harness.ngtriggers.dtos;

import io.swagger.annotations.ApiModel;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel("NGPipelineErrorWrapper")
public class NGPipelineErrorWrapperDTO {
  String errorPipelineYaml;
  Map<String, NGPipelineErrorResponseDTO> uuidToErrorResponseMap;
}
