package io.harness.ng.core.template.exception;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ngexception.NGTemplateException;
import io.harness.ng.core.template.TemplateInputsErrorResponseDTO;

import java.util.EnumSet;
import lombok.Getter;

@OwnedBy(HarnessTeam.CDC)
@Getter
public class NGTemplateResolveException extends NGTemplateException {
  private final TemplateInputsErrorResponseDTO errorResponseDTO;

  public NGTemplateResolveException(
      String message, EnumSet<ReportTarget> reportTarget, TemplateInputsErrorResponseDTO errorResponseDTO) {
    super(message, null, reportTarget);
    this.errorResponseDTO = errorResponseDTO;
    this.errorResponseDTO.setMessage(message);
  }
}
