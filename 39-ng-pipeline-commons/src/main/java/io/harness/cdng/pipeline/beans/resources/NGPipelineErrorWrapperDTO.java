package io.harness.cdng.pipeline.beans.resources;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
@ApiModel("NGPipelineErrorWrapper")
public class NGPipelineErrorWrapperDTO {
  String errorPipelineYaml;
  Map<String, NGPipelineErrorResponseDTO> uuidToErrorResponseMap;
}
