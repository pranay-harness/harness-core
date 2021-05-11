package io.harness.exception;

import static io.harness.eraro.ErrorCode.EXPLANATION;
import static io.harness.eraro.ErrorCode.HINT;
import static io.harness.eraro.Level.INFO;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.EnumSet;

@OwnedBy(HarnessTeam.DX)
public class HintException extends WingsException {
  public static final String HINT_EMPTY_ACCESS_KEY = "Check if access key is empty";
  public static final String HINT_EMPTY_SECRET_KEY = "Check if secret key is empty";
  public static final String HINT_AWS_IAM_ROLE_CHECK = "Check IAM role on delegate ec2";
  public static final String HINT_AWS_CLIENT_UNKNOWN_ISSUE = "Check AWS client on delegate";
  public static final String HINT_ECR_IMAGE_NAME = "Check if given ECR image is available in specified region";
  public static final String HINT_AWS_ACCESS_DENIED = "Hint: please ensure AWS credentials are valid";
  public static final String HINT_UNEXPECTED_ERROR = "Please reach out to harness support team";

  public static final HintException MOVE_TO_THE_PARENT_OBJECT =
      new HintException("Navigate back to the parent object page and continue from there.");
  public static final HintException REFRESH_THE_PAGE = new HintException("Refresh the web page to update the data.");

  public HintException(String message) {
    super(message, null, HINT, INFO, null, null);
    super.excludeReportTarget(EXPLANATION, EnumSet.of(ReportTarget.LOG_SYSTEM));
    super.param("message", message);
  }

  public HintException(String message, Throwable cause) {
    super(message, cause, HINT, INFO, null, null);
    super.excludeReportTarget(EXPLANATION, EnumSet.of(ReportTarget.LOG_SYSTEM));
    super.param("message", message);
  }
}
