package io.harness.cdng.inputset.beans.resource;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
@ApiModel("InputSetErrorResponse")
public class InputSetErrorResponseDTO {
  @Builder.Default List<InputSetErrorDTO> errors = new ArrayList<>();
}
