package software.wings.delegatetasks.validation.terraform;

import com.google.common.base.Throwables;
import com.google.inject.Singleton;

import io.harness.exception.ExceptionUtils;
import lombok.experimental.UtilityClass;
import org.eclipse.jgit.api.errors.JGitInternalException;

@Singleton
@UtilityClass
public class TerraformTaskUtils {
  public static String getGitExceptionMessageIfExists(Throwable t) {
    if (t instanceof JGitInternalException) {
      return Throwables.getRootCause(t).getMessage();
    }
    return ExceptionUtils.getMessage(t);
  }
}
