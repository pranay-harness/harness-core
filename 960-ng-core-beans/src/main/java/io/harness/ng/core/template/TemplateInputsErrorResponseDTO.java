package io.harness.ng.core.template;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.ng.core.Status;
import io.harness.ng.core.dto.ErrorDTO;

import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.CDC)
public class TemplateInputsErrorResponseDTO extends ErrorDTO {
  String errorYaml;
  Map<String, TemplateInputsErrorDTO> errorMap;

  public TemplateInputsErrorResponseDTO(Status status, ErrorCode code, String message, String detailedMessage,
      String errorYaml, Map<String, TemplateInputsErrorDTO> errorMap) {
    super(status, code, message, detailedMessage);
    this.errorMap = errorMap;
    this.errorYaml = errorYaml;
  }
}
