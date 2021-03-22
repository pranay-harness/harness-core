package io.harness.exception;

import static io.harness.eraro.ErrorCode.EXPLANATION;
import static io.harness.eraro.Level.INFO;

import java.util.EnumSet;

public class ExplanationException extends WingsException {
  public static final String EXPLANATION_EMPTY_ACCESS_KEY = "Access key cannot be empty";
  public static String EXPLANATION_EMPTY_SECRET_KEY = "Secret key cannot be empty";
  public static String EXPLANATION_AWS_AM_ROLE_CHECK =
      "IAM role on delegate ec2 doesn't exist or doesn't have required permissions to perform the activity";
  public static String EXPLANATION_AWS_CLIENT_UNKNOWN_ISSUE = "Seems to encounter unknown AWS client issue";

  public ExplanationException(String message, Throwable cause) {
    super(null, cause, EXPLANATION, INFO, USER_SRE, null);
    super.excludeReportTarget(EXPLANATION, EnumSet.of(ReportTarget.LOG_SYSTEM));
    super.param("message", message);
  }
}
