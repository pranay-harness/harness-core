package software.wings.helpers.ext.kustomize;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.hibernate.validator.constraints.NotEmpty;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.command.ExecutionLogCallback;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;

@Singleton
public class KustomizeTaskHelper {
  @Inject private KustomizeClient kustomizeClient;

  @Nonnull
  public List<ManifestFile> build(@Nonnull String manifestFilesDirectory, @Nonnull String kustomizeBinaryPath,
      @Nonnull KustomizeConfig kustomizeConfig, ExecutionLogCallback executionLogCallback) {
    CliResponse cliResponse;
    try {
      if (isBlank(kustomizeConfig.getPluginRootDir())) {
        cliResponse = kustomizeClient.build(
            manifestFilesDirectory, kustomizeConfig.getKustomizeDirPath(), kustomizeBinaryPath, executionLogCallback);

      } else {
        cliResponse = kustomizeClient.buildWithPlugins(manifestFilesDirectory, kustomizeConfig.getKustomizeDirPath(),
            kustomizeBinaryPath, kustomizeConfig.getPluginRootDir(), executionLogCallback);
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
          ManifestFile.builder().fileName("manifest.yaml").fileContent(cliResponse.getOutput()).build());
    } else {
      throw new InvalidRequestException("Kustomize build failed. Msg: " + cliResponse.getOutput(), WingsException.USER);
    }
  }

  @NotNull
  public List<ManifestFile> buildForApply(@Nonnull String kustomizeBinaryPath, @Nonnull KustomizeConfig kustomizeConfig,
      @Nonnull String manifestFilesDirectory, @NotEmpty List<String> filesToApply,
      ExecutionLogCallback executionLogCallback) {
    if (isEmpty(filesToApply)) {
      throw new InvalidRequestException("Apply files can't be empty", USER);
    }
    if (filesToApply.size() > 1) {
      throw new InvalidRequestException("Apply with Kustomize is supported for single file only", USER);
    }
    KustomizeConfig kustomizeConfigForApply = KustomizeConfig.builder()
                                                  .kustomizeDirPath(filesToApply.get(0))
                                                  .pluginRootDir(kustomizeConfig.getPluginRootDir())
                                                  .build();
    return build(manifestFilesDirectory, kustomizeBinaryPath, kustomizeConfigForApply, executionLogCallback);
  }
}
