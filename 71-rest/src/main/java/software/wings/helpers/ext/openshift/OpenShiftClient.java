package software.wings.helpers.ext.openshift;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.cli.CliResponse;

import java.util.List;
import javax.annotation.Nonnull;

public interface OpenShiftClient {
  @Nonnull
  CliResponse process(@NotEmpty String ocBinaryPath, @NotEmpty String templateFilePath, List<String> paramsFilePaths,
      @NotEmpty String manifestFilesDirectoryPath, ExecutionLogCallback executionLogCallback);
}
