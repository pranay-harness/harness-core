package software.wings.delegatetasks.validation.terraform;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.provision.TerraformConstants.VAR_FILE_FORMAT;

import io.harness.exception.ExceptionUtils;
import io.harness.provision.TfVarScriptRepositorySource;
import io.harness.provision.TfVarSource;
import io.harness.provision.TfVarSource.TfVarSourceType;

import software.wings.api.terraform.TfVarGitSource;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.inject.Singleton;
import java.nio.file.Paths;
import java.util.List;
import javax.validation.constraints.NotNull;
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

  /**
   * Currently using tfVarDirectory only for Git TfVarFiles sources
   * @param userDir user home directory
   * @param tfVarSource input tf-var files source
   * @param workingDir working directory of tf-var script repository
   * @param tfVarDirectory tf-var directory. (this is different from tf-var script repository and used only for Git
   *     sources for now.
   * @return -var-file output to be used in tf command
   */
  @VisibleForTesting
  public static String fetchAllTfVarFilesArgument(
      String userDir, @NotNull TfVarSource tfVarSource, String workingDir, String tfVarDirectory) {
    StringBuilder builder = new StringBuilder();
    final List<String> filePathList;
    final String directory;

    if (tfVarSource.getTfVarSourceType() == TfVarSourceType.SCRIPT_REPOSITORY) {
      TfVarScriptRepositorySource source = (TfVarScriptRepositorySource) tfVarSource;
      filePathList = source.getTfVarFilePaths();
      directory = workingDir;
    } else if (tfVarSource.getTfVarSourceType() == TfVarSourceType.GIT) {
      TfVarGitSource source = (TfVarGitSource) tfVarSource;
      filePathList = source.getGitFileConfig().getFilePathList();
      directory = tfVarDirectory;
    } else {
      filePathList = null;
      directory = null;
    }

    if (!isEmpty(filePathList)) {
      filePathList.forEach(file -> {
        String pathForFile = Paths.get(userDir, directory, file).toString();
        builder.append(String.format(VAR_FILE_FORMAT, pathForFile));
      });
    }

    return builder.toString();
  }
}
