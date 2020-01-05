package software.wings.delegatetasks.pcf.pcftaskhandler;

import static io.harness.pcf.model.PcfConstants.PCF_ARTIFACT_DOWNLOAD_DIR_PATH;
import static io.harness.pcf.model.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX;
import static software.wings.beans.Log.LogColor.White;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.Log.LogWeight.Bold;
import static software.wings.beans.Log.color;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.FileData;
import io.harness.data.structure.CollectionUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.UnexpectedException;
import io.harness.filesystem.FileIo;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.PcfConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.PcfClient;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.PivotalClientApiException;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfRunPluginCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfRunPluginScriptRequestData;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.sm.states.pcf.PcfPluginState;
import software.wings.utils.Misc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
@Singleton
@Slf4j
public class PcfRunPluginCommandTaskHandler extends PcfCommandTaskHandler {
  @Inject PcfClient pcfClient;

  /**
   * Execute the pcf plugin command
   */
  @Override
  protected PcfCommandExecutionResponse executeTaskInternal(PcfCommandRequest pcfCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback) {
    if (!(pcfCommandRequest instanceof PcfRunPluginCommandRequest)) {
      throw new InvalidArgumentsException(Pair.of("pcfCommandRequest", "Must be instance of PcfPluginCommandRequest"));
    }

    PcfRunPluginCommandRequest pluginCommandRequest = (PcfRunPluginCommandRequest) pcfCommandRequest;

    executionLogCallback.saveExecutionLog(color("---------- Starting PCF Run Plugin Command Execution", White, Bold));
    PcfConfig pcfConfig = pcfCommandRequest.getPcfConfig();
    encryptionService.decrypt(pcfConfig, encryptedDataDetails);
    File workingDirectory = null;
    try {
      workingDirectory = createWorkingDirectory();
      final String workingDirCanonicalPath = dirCanonicalPath(workingDirectory);

      // save the files in the directory
      if (EmptyPredicate.isNotEmpty(pluginCommandRequest.getFileDataList())) {
        saveFilesInWorkingDirectory(pluginCommandRequest.getFileDataList(), workingDirCanonicalPath);
      }
      //  insert working directory in script path
      final String finalScriptString = prepareFinalScript(pluginCommandRequest.getRenderedScriptString(),
          workingDirCanonicalPath, StringUtils.defaultIfEmpty(pluginCommandRequest.getRepoRoot(), "/"));

      // log all the files being saved and files being resolved in the script

      executionLogCallback.saveExecutionLog("\n #  File paths identified in the Script :");
      pcfCommandTaskHelper.printFileNamesInExecutionLogs(
          pluginCommandRequest.getFilePathsInScript(), executionLogCallback);

      executionLogCallback.saveExecutionLog(
          "\n #  Files saved in working directory [" + workingDirCanonicalPath + "]:");
      pcfCommandTaskHelper.printFileNamesInExecutionLogs(
          CollectionUtils.emptyIfNull(pluginCommandRequest.getFileDataList())
              .stream()
              .map(FileData::getFilePath)
              .collect(Collectors.toList()),
          executionLogCallback);

      PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder()
                                              .orgName(pluginCommandRequest.getOrganization())
                                              .spaceName(pluginCommandRequest.getSpace())
                                              .userName(pcfConfig.getUsername())
                                              .password(String.valueOf(pcfConfig.getPassword()))
                                              .endpointUrl(pcfConfig.getEndpointUrl())
                                              .timeOutIntervalInMins(pluginCommandRequest.getTimeoutIntervalInMin())
                                              .build();

      final PcfRunPluginScriptRequestData pcfRunPluginScriptRequestData =
          PcfRunPluginScriptRequestData.builder()
              .workingDirectory(workingDirCanonicalPath)
              .finalScriptString(finalScriptString)
              .pcfRequestConfig(pcfRequestConfig)
              .pluginCommandRequest(pluginCommandRequest)
              .build();
      pcfClient.runPcfPluginScript(pcfRunPluginScriptRequestData, executionLogCallback);

      executionLogCallback.saveExecutionLog(
          "\n ----------  PCF Run Plugin Command completed successfully", INFO, CommandExecutionStatus.SUCCESS);
      return PcfCommandExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();

    } catch (PivotalClientApiException | IOException e) {
      return handleError(executionLogCallback, pluginCommandRequest, e);
    } catch (Exception e) {
      return handleError(executionLogCallback, pluginCommandRequest, e);
    } finally {
      removeTempFilesCreated(executionLogCallback, workingDirectory);
    }
  }

  private String dirCanonicalPath(File workingDirectory) throws IOException {
    String canonicalPath = workingDirectory.getCanonicalPath().trim();
    if (canonicalPath.endsWith("/")) {
      canonicalPath = canonicalPath.substring(0, canonicalPath.length() - 1);
    }
    return canonicalPath;
  }

  private String prepareFinalScript(String renderedScriptString, String workingDirCanonicalPathStr, String repoRoot) {
    // replace the path identifier with actual working directory path
    String finalScript =
        renderedScriptString.replaceAll(PcfPluginState.FILE_START_REPO_ROOT_REGEX, workingDirCanonicalPathStr);
    final String dirPathWithRepoRoot = workingDirCanonicalPathStr + ("/".equals(repoRoot) ? "" : repoRoot);
    finalScript = finalScript.replaceAll(PcfPluginState.FILE_START_SERVICE_MANIFEST_REGEX, dirPathWithRepoRoot);

    return finalScript;
  }

  @VisibleForTesting
  void saveFilesInWorkingDirectory(final List<FileData> fileDataList, final String workingDirectoryCanonicalPath) {
    if (EmptyPredicate.isNotEmpty(fileDataList)) {
      fileDataList.forEach(file -> {
        try {
          final Path filePath = Paths.get(workingDirectoryCanonicalPath, canonicalise(file.getFilePath()));
          // ensure directory exists
          FileIo.createDirectoryIfDoesNotExist(filePath.getParent());
          // ensure file is newly created
          Files.deleteIfExists(filePath);
          final Path createdFile = Files.createFile(filePath);
          final byte[] fileBytes = file.getFileBytes();
          if (fileBytes != null) {
            FileIo.writeFile(createdFile, fileBytes);
          }
        } catch (IOException e) {
          throw new UnexpectedException(
              "Error while writing file :" + file.getFilePath() + "in directory :" + workingDirectoryCanonicalPath);
        }
      });
    }
  }

  private String canonicalise(String filePath) {
    String result = filePath.trim();
    if (result.isEmpty() || result.charAt(0) != '/') {
      result = "/" + result;
    }
    return result;
  }

  @VisibleForTesting
  PcfCommandExecutionResponse handleError(
      ExecutionLogCallback executionLogCallback, PcfRunPluginCommandRequest pluginCommandRequest, Exception e) {
    logger.error(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX + "Exception in processing PCF Run Plugin Command task [{}]",
        pluginCommandRequest);
    logger.error("Exception is ", e);
    executionLogCallback.saveExecutionLog("\n\n ----------  PCF Run Plugin Command failed to complete successfully",
        ERROR, CommandExecutionStatus.FAILURE);
    Misc.logAllMessages(e, executionLogCallback);
    return PcfCommandExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
        .errorMessage(ExceptionUtils.getMessage(e))
        .build();
  }

  private void removeTempFilesCreated(ExecutionLogCallback executionLogCallback, File workingDirectory) {
    try {
      executionLogCallback.saveExecutionLog("# Deleting any temporary files created");
      if (workingDirectory != null) {
        FileIo.deleteDirectoryAndItsContentIfExists(workingDirectory.getAbsolutePath());
      }
    } catch (Exception e) {
      logger.warn("Failed to remove temp files created", e);
    }
  }

  private File createWorkingDirectory() throws IOException {
    // This value is set to CF_HOME env variable when process executor is created.
    String randomToken = UUIDGenerator.generateUuid();
    return generateWorkingDirectoryForScript(randomToken);
  }

  private File generateWorkingDirectoryForScript(String workingDirecotry) throws IOException {
    String workingDir = PCF_ARTIFACT_DOWNLOAD_DIR_PATH + "/" + workingDirecotry;
    FileIo.createDirectoryIfDoesNotExist(workingDir);
    return new File(workingDir);
  }
}
