package software.wings.helpers.ext.openshift;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.cli.CliResponse;

import software.wings.beans.command.ExecutionLogCallback;

import java.util.List;
import javax.annotation.Nonnull;
import org.hibernate.validator.constraints.NotEmpty;

@TargetModule(Module._960_API_SERVICES)
public interface OpenShiftClient {
  @Nonnull
  CliResponse process(@NotEmpty String ocBinaryPath, @NotEmpty String templateFilePath, List<String> paramsFilePaths,
      @NotEmpty String manifestFilesDirectoryPath, ExecutionLogCallback executionLogCallback);
}
