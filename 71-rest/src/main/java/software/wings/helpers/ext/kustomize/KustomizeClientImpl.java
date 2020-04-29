package software.wings.helpers.ext.kustomize;

import static software.wings.helpers.ext.kustomize.KustomizeConstants.KUSTOMIZE_BINARY_PATH;
import static software.wings.helpers.ext.kustomize.KustomizeConstants.KUSTOMIZE_BUILD_COMMAND;
import static software.wings.helpers.ext.kustomize.KustomizeConstants.KUSTOMIZE_BUILD_COMMAND_WITH_PLUGINS;
import static software.wings.helpers.ext.kustomize.KustomizeConstants.KUSTOMIZE_COMMAND_TIMEOUT;
import static software.wings.helpers.ext.kustomize.KustomizeConstants.KUSTOMIZE_DIR_PATH;
import static software.wings.helpers.ext.kustomize.KustomizeConstants.XDG_CONFIG_HOME;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.cli.CliHelper;
import software.wings.helpers.ext.cli.CliResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;

@Slf4j
@Singleton
public class KustomizeClientImpl implements KustomizeClient {
  @Inject CliHelper cliHelper;

  @Override
  public CliResponse build(@Nonnull String manifestFilesDirectory, @Nonnull String kustomizeDirPath,
      @Nonnull String kustomizeBinaryPath, @Nonnull ExecutionLogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException {
    String kustomizeBuildCommand = KUSTOMIZE_BUILD_COMMAND.replace(KUSTOMIZE_BINARY_PATH, kustomizeBinaryPath)
                                       .replace(KUSTOMIZE_DIR_PATH, kustomizeDirPath);
    return cliHelper.executeCliCommand(kustomizeBuildCommand, KUSTOMIZE_COMMAND_TIMEOUT, Collections.emptyMap(),
        manifestFilesDirectory, executionLogCallback);
  }

  @Override
  public CliResponse buildWithPlugins(@Nonnull String manifestFilesDirectory, @Nonnull String kustomizeDirPath,
      @Nonnull String kustomizeBinaryPath, @Nonnull String pluginPath,
      @Nonnull ExecutionLogCallback executionLogCallback) throws InterruptedException, TimeoutException, IOException {
    String kustomizeBuildCommand =
        KUSTOMIZE_BUILD_COMMAND_WITH_PLUGINS.replace(KUSTOMIZE_BINARY_PATH, kustomizeBinaryPath)
            .replace(KUSTOMIZE_DIR_PATH, kustomizeDirPath)
            .replace(XDG_CONFIG_HOME, pluginPath);
    return cliHelper.executeCliCommand(kustomizeBuildCommand, KUSTOMIZE_COMMAND_TIMEOUT, Collections.emptyMap(),
        manifestFilesDirectory, executionLogCallback);
  }
}
