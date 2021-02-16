package software.wings.helpers.ext.kustomize;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.cli.CliResponse;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;

@TargetModule(Module._960_API_SERVICES)
public interface KustomizeClient {
  @Nonnull
  CliResponse build(@Nonnull String manifestFilesDirectory, @Nonnull String kustomizeDirPath,
      @Nonnull String kustomizeBinaryPath, @Nonnull ExecutionLogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse buildWithPlugins(@Nonnull String manifestFilesDirectory, @Nonnull String kustomizeDirPath,
      @Nonnull String kustomizeBinaryPath, @Nonnull String pluginPath, @Nonnull ExecutionLogCallback callback)
      throws InterruptedException, TimeoutException, IOException;
}
