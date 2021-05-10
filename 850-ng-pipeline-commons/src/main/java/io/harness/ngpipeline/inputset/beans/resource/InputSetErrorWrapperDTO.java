package io.harness.ngpipeline.inputset.beans.resource;

import io.harness.annotations.dev.ToBeDeleted;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("InputSetErrorWrapper")
@ToBeDeleted
@Deprecated
public class InputSetErrorWrapperDTO {
  String errorPipelineYaml;
  Map<String, InputSetErrorResponseDTO> uuidToErrorResponseMap;
}
