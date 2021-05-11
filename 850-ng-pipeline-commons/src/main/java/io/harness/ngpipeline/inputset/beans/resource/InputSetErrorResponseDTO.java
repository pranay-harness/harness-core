package io.harness.ngpipeline.inputset.beans.resource;

import io.harness.annotations.dev.ToBeDeleted;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("InputSetErrorResponse")
@ToBeDeleted
@Deprecated
public class InputSetErrorResponseDTO {
  @Builder.Default List<InputSetErrorDTO> errors = new ArrayList<>();
}
