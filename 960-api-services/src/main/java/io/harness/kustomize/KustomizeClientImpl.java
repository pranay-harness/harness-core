/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.kustomize;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.kustomize.KustomizeConstants.KUSTOMIZE_BINARY_PATH;
import static io.harness.kustomize.KustomizeConstants.KUSTOMIZE_BUILD_COMMAND;
import static io.harness.kustomize.KustomizeConstants.KUSTOMIZE_BUILD_COMMAND_WITH_PLUGINS;
import static io.harness.kustomize.KustomizeConstants.KUSTOMIZE_COMMAND_TIMEOUT;
import static io.harness.kustomize.KustomizeConstants.KUSTOMIZE_DIR_PATH;
import static io.harness.kustomize.KustomizeConstants.XDG_CONFIG_HOME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cli.CliHelper;
import io.harness.cli.CliResponse;
import io.harness.logging.LogCallback;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDP)
@Slf4j
@Singleton
public class KustomizeClientImpl implements KustomizeClient {
  @Inject CliHelper cliHelper;

  @Override
  public CliResponse build(@Nonnull String manifestFilesDirectory, @Nonnull String kustomizeDirPath,
      @Nonnull String kustomizeBinaryPath, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException {
    String kustomizeBuildCommand = KUSTOMIZE_BUILD_COMMAND.replace(KUSTOMIZE_BINARY_PATH, kustomizeBinaryPath)
                                       .replace(KUSTOMIZE_DIR_PATH, kustomizeDirPath);
    return cliHelper.executeCliCommand(kustomizeBuildCommand, KUSTOMIZE_COMMAND_TIMEOUT, Collections.emptyMap(),
        manifestFilesDirectory, executionLogCallback);
  }

  @Override
  public CliResponse buildWithPlugins(@Nonnull String manifestFilesDirectory, @Nonnull String kustomizeDirPath,
      @Nonnull String kustomizeBinaryPath, @Nonnull String pluginPath, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException {
    String kustomizeBuildCommand =
        KUSTOMIZE_BUILD_COMMAND_WITH_PLUGINS.replace(KUSTOMIZE_BINARY_PATH, kustomizeBinaryPath)
            .replace(KUSTOMIZE_DIR_PATH, kustomizeDirPath)
            .replace(XDG_CONFIG_HOME, pluginPath);
    return cliHelper.executeCliCommand(kustomizeBuildCommand, KUSTOMIZE_COMMAND_TIMEOUT, Collections.emptyMap(),
        manifestFilesDirectory, executionLogCallback);
  }
}
