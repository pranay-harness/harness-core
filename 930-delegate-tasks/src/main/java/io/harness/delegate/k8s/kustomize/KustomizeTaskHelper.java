package io.harness.delegate.k8s.kustomize;

import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.exception.WingsException.USER;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.beans.FileData;
import io.harness.cli.CliResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.kustomize.KustomizeClient;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.hibernate.validator.constraints.NotEmpty;
import org.jetbrains.annotations.NotNull;

@Singleton
public class KustomizeTaskHelper {
  @Inject private KustomizeClient kustomizeClient;

  @Nonnull
  public List<FileData> build(@Nonnull String manifestFilesDirectory, @Nonnull String kustomizeBinaryPath,
      String pluginRootDir, String kustomizeDirPath, LogCallback executionLogCallback) {
    CliResponse cliResponse;
    try {
      if (isBlank(pluginRootDir)) {
        cliResponse =
            kustomizeClient.build(manifestFilesDirectory, kustomizeDirPath, kustomizeBinaryPath, executionLogCallback);

      } else {
        cliResponse = kustomizeClient.buildWithPlugins(
            manifestFilesDirectory, kustomizeDirPath, kustomizeBinaryPath, pluginRootDir, executionLogCallback);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InvalidRequestException("Kustomize build interrupted", e, WingsException.USER);
    } catch (TimeoutException e) {
      throw new InvalidRequestException("Kustomize build timed out", e, WingsException.USER);
    } catch (IOException e) {
      throw new InvalidRequestException("IO Failure occurred while running kustomize build", e, WingsException.USER);
    }

    if (cliResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      return Collections.singletonList(
          FileData.builder().fileName("manifest.yaml").fileContent(cliResponse.getOutput()).build());
    } else {
      throw new InvalidRequestException("Kustomize build failed. Msg: " + cliResponse.getOutput(), WingsException.USER);
    }
  }

  @NotNull
  public List<FileData> buildForApply(@Nonnull String kustomizeBinaryPath, String pluginRootDir,
      @Nonnull String manifestFilesDirectory, @NotEmpty List<String> filesToApply, LogCallback executionLogCallback) {
    if (hasNone(filesToApply)) {
      throw new InvalidRequestException("Apply files can't be empty", USER);
    }
    if (filesToApply.size() > 1) {
      throw new InvalidRequestException("Apply with Kustomize is supported for single file only", USER);
    }
    String kustomizeDirPath = filesToApply.get(0);
    return build(manifestFilesDirectory, kustomizeBinaryPath, pluginRootDir, kustomizeDirPath, executionLogCallback);
  }
}
