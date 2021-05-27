package io.harness.pms.inputset;

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
public class InputSetErrorWrapperDTOPMS {
  String errorPipelineYaml;
  Map<String, InputSetErrorResponseDTOPMS> uuidToErrorResponseMap;
}
