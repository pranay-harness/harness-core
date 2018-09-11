package software.wings.exception;

import static io.harness.eraro.ErrorCode.INVALID_TOKEN;

import java.util.EnumSet;

public class UnauthorizedException extends WingsException {
  public UnauthorizedException(String message, EnumSet<ReportTarget> reportTarget) {
    super(INVALID_TOKEN, message, reportTarget);
  }
}
