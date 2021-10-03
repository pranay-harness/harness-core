package io.harness.ng.core.template;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDC)
public class TemplateInputsErrorDTO {
  String fieldName;
  String message;
  String identifierOfErrorSource;
}
