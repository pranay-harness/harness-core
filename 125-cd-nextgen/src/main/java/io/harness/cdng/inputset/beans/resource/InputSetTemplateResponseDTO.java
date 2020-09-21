package io.harness.cdng.inputset.beans.resource;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InputSetTemplateResponseDTO {
  String inputSetTemplateYaml;
}
