package io.harness.delegate.task.aws;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.provision.AWSConstants.AWS_SAM_WORKING_DIRECTORY;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.AwsSamClient;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.exception.AwsSamCommandExecutionException;
import io.harness.exception.ExceptionUtils;
import io.harness.git.model.GitBaseRequest;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.PlanJsonLogOutputStream;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
public class AwsSamDeployTaskHandlerNG extends AwsSamAbstractTaskHandler {
  @Inject AwsBaseHelper awsBaseHelper;
  @Inject AwsSamClient awsSamClient;

  @Override
  public AwsSamTaskNGResponse executeTaskInternal(AwsSamTaskParameters taskParameters, String delegateId, String taskId,
      LogCallback logCallback) throws IOException, AwsSamCommandExecutionException {
    GitStoreDelegateConfig confileFileGitStore = taskParameters.getConfigFile().getGitStoreDelegateConfig();
    String awsSamProjectDirectoryPath = confileFileGitStore.getPaths().get(0);

    if (isNotEmpty(confileFileGitStore.getBranch())) {
      logCallback.saveExecutionLog("Branch: " + confileFileGitStore.getBranch(), INFO, CommandExecutionStatus.RUNNING);
    }

    logCallback.saveExecutionLog(
        "Normalized Path: " + awsSamProjectDirectoryPath, INFO, CommandExecutionStatus.RUNNING);

    if (isNotEmpty(confileFileGitStore.getCommitId())) {
      logCallback.saveExecutionLog(
          format("%nInheriting git state at commit id: [%s]", confileFileGitStore.getCommitId()), INFO,
          CommandExecutionStatus.RUNNING);
    }
    GitBaseRequest gitBaseRequestForConfigFile = awsBaseHelper.getGitBaseRequestForConfigFile(
        taskParameters.getAccountId(), confileFileGitStore, (GitConfigDTO) confileFileGitStore.getGitConfigDTO());

    String baseDir = AWS_SAM_WORKING_DIRECTORY + taskParameters.getEntityId();

    String awsSamAppDirectory = awsBaseHelper.fetchAwsSamAppDirectory(gitBaseRequestForConfigFile,
        taskParameters.getAccountId(), "", "", confileFileGitStore, logCallback, awsSamProjectDirectoryPath, baseDir);

    try (PlanJsonLogOutputStream planJsonLogOutputStream = new PlanJsonLogOutputStream()) {
      String samBuildCommand = format("sam build");
      Map<String, String> envVariables = new HashMap<>();
      awsSamClient.runCommand(samBuildCommand, 120000l, envVariables, awsSamAppDirectory, logCallback);

      return AwsSamTaskNGResponse.builder()
          .outputs("")
          .commitIdForConfigFilesMap(new HashMap<>())
          .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
          .build();
    } catch (AwsSamCommandExecutionException awsSamCommandExecutionException) {
      log.warn("Failed to execute Aws Sam Deploy Step", awsSamCommandExecutionException);
      logCallback.saveExecutionLog("Failed", ERROR, CommandExecutionStatus.FAILURE);
      return AwsSamTaskNGResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(awsSamCommandExecutionException))
          .build();
    } catch (Exception exception) {
      log.warn("Exception Occurred", exception);
      logCallback.saveExecutionLog("Failed", ERROR, CommandExecutionStatus.FAILURE);
      return AwsSamTaskNGResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(exception))
          .build();
    }
  }
}
