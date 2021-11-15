package io.harness.ng.core.template;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDC)
@Schema(name = "TemplateInputsError", description = "This contains details of the Template Inputs Error")
public class TemplateInputsErrorDTO {
  String fieldName;
  String message;
  String identifierOfErrorSource;
}
