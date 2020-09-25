package io.harness.cdng.inputset.beans.resource;

import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("InputSetTemplateResponse")
public class InputSetTemplateResponseDTO {
  String inputSetTemplateYaml;
}
