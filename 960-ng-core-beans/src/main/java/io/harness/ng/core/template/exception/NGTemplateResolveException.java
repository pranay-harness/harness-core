package io.harness.ng.core.template.exception;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ngexception.NGTemplateException;
import io.harness.exception.ngexception.beans.templateservice.TemplateInputsErrorMetadataDTO;

import java.util.EnumSet;
import lombok.Getter;

@OwnedBy(HarnessTeam.CDC)
@Getter
public class NGTemplateResolveException extends NGTemplateException {
  private final TemplateInputsErrorMetadataDTO errorResponseDTO;

  public NGTemplateResolveException(
      String message, EnumSet<ReportTarget> reportTarget, TemplateInputsErrorMetadataDTO errorResponseDTO) {
    super(message, reportTarget, errorResponseDTO);
    this.errorResponseDTO = errorResponseDTO;
  }
}
