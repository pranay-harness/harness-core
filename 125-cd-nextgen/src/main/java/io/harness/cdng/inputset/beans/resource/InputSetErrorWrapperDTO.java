package io.harness.cdng.inputset.beans.resource;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
@ApiModel("InputSetErrorWrapper")
public class InputSetErrorWrapperDTO {
  String errorPipelineYaml;
  Map<String, InputSetErrorResponseDTO> uuidToErrorResponseMap;
}
