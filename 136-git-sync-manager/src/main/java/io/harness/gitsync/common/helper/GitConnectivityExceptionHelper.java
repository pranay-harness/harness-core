package io.harness.gitsync.common.helper;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.DelegateServiceDriverException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.ScmException;
import io.harness.exception.WingsException;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PL)
public class GitConnectivityExceptionHelper {
  public static final String CONNECTIVITY_ERROR = "Unable to connect to Git provider due to error: ";
  public static final String ERROR_MSG_MSVC_DOWN = "Something went wrong, Please contact Harness Support.";

  public String getErrorMessage(Exception ex) {
    if (ex instanceof DelegateServiceDriverException) {
      return CONNECTIVITY_ERROR + ExceptionUtils.getMessage(ex);
    } else if (ex instanceof WingsException) {
      if (ExceptionUtils.cause(ScmException.class, ex) != null) {
        return CONNECTIVITY_ERROR + ExceptionUtils.getMessage(ex);
      } else {
        return "Error: " + ExceptionUtils.getMessage(ex);
      }
    } else {
      return null;
    }
  }
}
