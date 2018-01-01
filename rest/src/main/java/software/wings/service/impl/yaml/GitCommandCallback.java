package software.wings.service.impl.yaml;

import com.google.inject.Inject;

import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.beans.GitCommit;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitCommand.GitCommandType;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.beans.yaml.GitCommandResult;
import software.wings.beans.yaml.GitCommitAndPushResult;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitFileChange;
import software.wings.exception.YamlProcessingException;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.sync.YamlService;
import software.wings.waitnotify.NotifyCallback;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlChangeSet.Status;

import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 10/27/17.
 */
public class GitCommandCallback implements NotifyCallback {
  private String accountId;
  private String changeSetId;
  private String yamlGitConfigId;

  public GitCommandCallback() {}

  public GitCommandCallback(String accountId, String changeSetId, String yamlGitConfigId) {
    this.accountId = accountId;
    this.changeSetId = changeSetId;
    this.yamlGitConfigId = yamlGitConfigId;
  }

  @Transient private final transient Logger logger = LoggerFactory.getLogger(getClass());

  @Transient @Inject private transient YamlChangeSetService yamlChangeSetService;
  @Transient @Inject private transient YamlService yamlService;

  @Transient @Inject private transient YamlGitService yamlGitService;

  @Override
  public void notify(Map<String, NotifyResponseData> response) {
    logger.info("Git command response [{}]", response);
    NotifyResponseData notifyResponseData = response.values().iterator().next();
    if (notifyResponseData instanceof GitCommandExecutionResponse) {
      GitCommandExecutionResponse gitCommandExecutionResponse = (GitCommandExecutionResponse) notifyResponseData;
      GitCommandResult gitCommandResult = gitCommandExecutionResponse.getGitCommandResult();

      if (gitCommandExecutionResponse.getGitCommandStatus().equals(GitCommandStatus.FAILURE)) {
        if (changeSetId != null) {
          logger.error("Git Command failed [{}] for changeSetId [{}]", gitCommandExecutionResponse.getErrorMessage(),
              changeSetId);
          yamlChangeSetService.updateStatus(accountId, changeSetId, Status.FAILED);
        }
        return;
      }

      logger.info("Git command [type: {}] request completed with status [{}]", gitCommandResult.getGitCommandType(),
          gitCommandExecutionResponse.getGitCommandStatus());

      if (gitCommandResult.getGitCommandType().equals(GitCommandType.COMMIT_AND_PUSH)) {
        GitCommitAndPushResult gitCommitAndPushResult = (GitCommitAndPushResult) gitCommandResult;
        YamlChangeSet yamlChangeSet = yamlChangeSetService.get(accountId, changeSetId);
        if (yamlChangeSet != null) {
          yamlChangeSetService.updateStatus(accountId, changeSetId, Status.COMPLETED);
          if (gitCommitAndPushResult.getGitCommitResult().getCommitId() != null) {
            yamlGitService.saveCommit(GitCommit.builder()
                                          .accountId(accountId)
                                          .yamlChangeSet(yamlChangeSet)
                                          .yamlGitConfigId(yamlGitConfigId)
                                          .status(GitCommit.Status.COMPLETED)
                                          .commitId(gitCommitAndPushResult.getGitCommitResult().getCommitId())
                                          .gitCommandResult(gitCommitAndPushResult)
                                          .build());
          }
          yamlGitService.removeGitSyncErrors(accountId, yamlChangeSet.getGitFileChanges());
        }
      } else if (gitCommandResult.getGitCommandType().equals(GitCommandType.DIFF)) {
        GitDiffResult gitDiffResult = (GitDiffResult) gitCommandResult;
        List<GitFileChange> gitFileChangeList = gitDiffResult.getGitFileChanges();
        try {
          List<ChangeContext> fileChangeContexts = yamlService.processChangeSet(gitFileChangeList);
          logger.info("Processed ChangeSet: [{}]", fileChangeContexts);
          yamlGitService.saveCommit(GitCommit.builder()
                                        .accountId(accountId)
                                        .yamlChangeSet(YamlChangeSet.builder()
                                                           .accountId(accountId)
                                                           .appId(Base.GLOBAL_APP_ID)
                                                           .gitToHarness(true)
                                                           .status(Status.COMPLETED)
                                                           .gitFileChanges(gitDiffResult.getGitFileChanges())
                                                           .build())
                                        .yamlGitConfigId(yamlGitConfigId)
                                        .status(GitCommit.Status.COMPLETED)
                                        .commitId(gitDiffResult.getCommitId())
                                        .gitCommandResult(gitDiffResult)
                                        .build());
          yamlGitService.removeGitSyncErrors(accountId, gitFileChangeList);
        } catch (YamlProcessingException ex) {
          logger.error("Processing changeSet failed", ex);
          yamlGitService.processFailedOrUnprocessedChanges(gitFileChangeList, ex.getChange(), ex.getMessage());
        }
      } else {
        logger.error("Unexpected commandType result: [{}] for changeSetId [{}]",
            gitCommandExecutionResponse.getErrorMessage(), changeSetId);
        yamlChangeSetService.updateStatus(accountId, changeSetId, Status.FAILED);
      }
    } else {
      logger.error("Unexpected notify response data: [{}]", notifyResponseData);
      yamlChangeSetService.updateStatus(accountId, changeSetId, Status.FAILED);
    }
  }

  @Override
  public void notifyError(Map<String, NotifyResponseData> response) {
    logger.error("Git request failed [{}]", response);
    yamlChangeSetService.updateStatus(accountId, changeSetId, Status.FAILED);
  }
}
