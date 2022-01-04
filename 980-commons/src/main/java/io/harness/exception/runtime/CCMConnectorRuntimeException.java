package io.harness.exception.runtime;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import java.util.EnumSet;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@OwnedBy(CE)
@EqualsAndHashCode(callSuper = true)
public class CCMConnectorRuntimeException extends RuntimeException {
  private String message;
  private ErrorCode code = ErrorCode.INVALID_REQUEST;
  private EnumSet<WingsException.ReportTarget> reportTargets;

  public CCMConnectorRuntimeException(String message) {
    this.message = message;
  }

  public CCMConnectorRuntimeException(String message, ErrorCode code) {
    this.message = message;
    this.code = code;
  }

  public CCMConnectorRuntimeException(
      String message, ErrorCode code, EnumSet<WingsException.ReportTarget> reportTargets) {
    this.message = message;
    this.code = code;
    this.reportTargets = reportTargets;
  }
}
