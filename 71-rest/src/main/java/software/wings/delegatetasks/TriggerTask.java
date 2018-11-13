package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.GitRepositoryType;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.trigger.TriggerCommand.TriggerCommandType;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.helpers.ext.trigger.request.TriggerDeploymentNeededRequest;
import software.wings.helpers.ext.trigger.request.TriggerRequest;
import software.wings.helpers.ext.trigger.response.TriggerDeploymentNeededResponse;
import software.wings.helpers.ext.trigger.response.TriggerResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.Misc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TriggerTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(TriggerTask.class);

  @Inject private EncryptionService encryptionService;
  @Inject private GitService gitService;

  public TriggerTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public TriggerResponse run(Object[] parameters) {
    TriggerRequest triggerRequest = (TriggerRequest) (parameters[0]);
    TriggerCommandType triggerCommandType = triggerRequest.getTriggerCommandType();
    TriggerResponse triggerResponse;

    logger.info(format(
        "Executing trigger task for account %s and type is %s", triggerRequest.getAccountId(), triggerCommandType));

    try {
      switch (triggerCommandType) {
        case DEPLOYMENT_NEEDED_CHECK:
          TriggerDeploymentNeededRequest triggerDeploymentNeededRequest =
              (TriggerDeploymentNeededRequest) triggerRequest;
          triggerResponse = isDeploymentNeeded(triggerDeploymentNeededRequest);
          break;

        default:
          throw new InvalidRequestException("Operation not supported");
      }

      return triggerResponse;
    } catch (Exception ex) {
      logger.error(format("Exception in processing trigger task for account %s, triggerCommandType %s",
                       triggerRequest.getAccountId(), triggerCommandType),
          ex);
      return new TriggerResponse(CommandExecutionStatus.FAILURE, Misc.getMessage(ex));
    }
  }

  private TriggerDeploymentNeededResponse isDeploymentNeeded(TriggerDeploymentNeededRequest triggerRequest) {
    GitConfig gitConfig = triggerRequest.getGitConfig();
    String gitConnectorId = triggerRequest.getGitConnectorId();
    String currentCommitId = triggerRequest.getCurrentCommitId();
    String oldCommitId = triggerRequest.getOldCommitId();
    List<String> filePaths = triggerRequest.getFilePaths();
    List<EncryptedDataDetail> encryptionDetails = triggerRequest.getEncryptionDetails();

    logger.info(format("Checking if deployment needed - gitConnectorId %s, currentCommitId %s, oldCommitId %s",
        gitConnectorId, currentCommitId, oldCommitId));

    try {
      encryptionService.decrypt(gitConfig, encryptionDetails);

      gitConfig.setGitRepoType(GitRepositoryType.TRIGGER);
      GitFetchFilesResult gitFetchFilesResult =
          gitService.fetchFilesBetweenCommits(gitConfig, currentCommitId, oldCommitId, gitConnectorId);

      boolean deploymentNeeded = isDeploymentNeeded(filePaths, gitFetchFilesResult.getFiles());
      return TriggerDeploymentNeededResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
          .deploymentNeeded(deploymentNeeded)
          .build();
    } catch (Exception ex) {
      logger.error("Exception in checking if deployment needed " + Misc.getMessage(ex));
      throw ex;
    }
  }

  private boolean isDeploymentNeeded(List<String> filePaths, List<GitFile> changedFiles) {
    Set<String> changedFilesSet = new HashSet<>();

    if (isNotEmpty(changedFiles)) {
      for (GitFile gitFile : changedFiles) {
        changedFilesSet.add(gitFile.getFilePath());
      }

      for (String path : filePaths) {
        if (changedFilesSet.contains(path)) {
          return true;
        }
      }
    }

    return false;
  }
}
