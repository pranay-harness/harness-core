package io.harness.openshift;

import io.harness.cli.CliResponse;
import io.harness.logging.LogCallback;

import java.util.List;
import javax.annotation.Nonnull;
import org.hibernate.validator.constraints.NotEmpty;

public interface OpenShiftClient {
  @Nonnull
  CliResponse process(@NotEmpty String ocBinaryPath, @NotEmpty String templateFilePath, List<String> paramsFilePaths,
      @NotEmpty String manifestFilesDirectoryPath, LogCallback executionLogCallback);
}
