package io.harness.cdng.inputset.beans.resource;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel("InputSetError")
public class InputSetErrorDTO {
  String fieldName;
  String message;
  String identifierOfErrorSource;
}
