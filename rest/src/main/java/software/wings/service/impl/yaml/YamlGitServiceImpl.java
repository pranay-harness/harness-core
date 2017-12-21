package software.wings.service.impl.yaml;

import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.ErrorCode;
import software.wings.beans.GitCommit;
import software.wings.beans.GitConfig;
import software.wings.beans.TaskType;
import software.wings.beans.yaml.GitCommand.GitCommandType;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.beans.yaml.GitCommitRequest;
import software.wings.beans.yaml.GitDiffRequest;
import software.wings.beans.yaml.GitFileChange;
import software.wings.common.UUIDGenerator;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.utils.CryptoUtil;
import software.wings.waitnotify.WaitNotifyEngine;
import software.wings.yaml.directory.FolderNode;
import software.wings.yaml.gitSync.GitSyncWebhook;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlChangeSet.Status;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.validation.executable.ValidateOnExecution;

/**
 * The type Yaml git sync service.
 */
@ValidateOnExecution
public class YamlGitServiceImpl implements YamlGitService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * The constant SETUP_ENTITY_ID.
   */
  public static final String SETUP_ENTITY_ID = "setup";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private YamlDirectoryService yamlDirectoryService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private SecretManager secretManager;
  @Inject private ExecutorService executorService;
  @Inject private DelegateService delegateService;

  /**
   * Gets the yaml git sync info by entityId
   *
   * @return the rest response
   */
  @Override
  public YamlGitConfig get(String accountId, String entityId) {
    return wingsPersistence.createQuery(YamlGitConfig.class).field("accountId").equal(accountId).get();
  }

  /**
   * Creates a new yaml git sync info by object type and entitytId (uuid)
   *
   * @param ygs the yamlGitSync info
   * @return the rest response
   */
  @Override
  public YamlGitConfig save(YamlGitConfig ygs) {
    //
    GitConfig gitConfig = ygs.getGitConfig();
    gitConfig.setDecrypted(true);
    validateGit(gitConfig);
    gitConfig.setDecrypted(false);
    YamlGitConfig yamlGitSync = wingsPersistence.saveAndGet(YamlGitConfig.class, ygs);
    executorService.submit(() -> fullSync(ygs.getAccountId()));
    return yamlGitSync;
  }

  /**
   * Updates the yaml git sync info by object type and entitytId (uuid)
   *
   * @param ygs the yamlGitSync info
   * @return the rest response
   */
  @Override
  public YamlGitConfig update(YamlGitConfig ygs) {
    GitConfig gitConfig = ygs.getGitConfig();
    gitConfig.setDecrypted(true);
    validateGit(gitConfig);
    gitConfig.setDecrypted(false);
    YamlGitConfig yamlGitSync = wingsPersistence.saveAndGet(YamlGitConfig.class, ygs);
    executorService.submit(() -> fullSync(ygs.getAccountId()));
    return yamlGitSync;
  }

  private void validateGit(GitConfig gitConfig) {
    /*
    1. Invalid repoUrl
    2. Invalid credentials
    3. No write access
    4. Branch doesn't exist
     */
    try {
      GitCommandExecutionResponse gitCommandExecutionResponse =
          delegateService.executeTask(aDelegateTask()
                                          .withTaskType(TaskType.GIT_COMMAND)
                                          .withAccountId(gitConfig.getAccountId())
                                          .withAppId(GLOBAL_APP_ID)
                                          .withAsync(false)
                                          .withTimeout(TimeUnit.SECONDS.toMillis(60))
                                          .withParameters(new Object[] {GitCommandType.VALIDATE, gitConfig,
                                              secretManager.getEncryptionDetails(gitConfig, GLOBAL_APP_ID, null)})
                                          .build());
      logger.info("GitConfigValidation [{}]", gitCommandExecutionResponse);
      if (gitCommandExecutionResponse.getGitCommandStatus().equals(GitCommandStatus.FAILURE)) {
        throw new WingsException(ErrorCode.INVALID_REQUEST, "message", gitCommandExecutionResponse.getErrorMessage());
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void fullSync(String accountId) {
    YamlGitConfig yamlGitConfig = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (yamlGitConfig != null) {
      try {
        FolderNode top = yamlDirectoryService.getDirectory(accountId, SETUP_ENTITY_ID);
        List<GitFileChange> gitFileChanges = new ArrayList<>();
        gitFileChanges = yamlDirectoryService.traverseDirectory(gitFileChanges, accountId, top, "");

        YamlChangeSet yamlChangeSet =
            YamlChangeSet.builder().accountId(accountId).status(Status.QUEUED).gitFileChanges(gitFileChanges).build();
        yamlChangeSet.setAppId(GLOBAL_APP_ID);

        yamlChangeSetService.save(yamlChangeSet);
      } catch (Exception ex) {
        logger.error("Failed to push directory: ", ex);
      }
    }
  }

  @Override
  public List<GitFileChange> performFullSyncDryRun(String accountId) {
    try {
      logger.info("Performing full sync dry-run for account:" + accountId);
      FolderNode top = yamlDirectoryService.getDirectory(accountId, SETUP_ENTITY_ID);
      List<GitFileChange> gitFileChanges = new ArrayList<>();
      yamlDirectoryService.traverseDirectory(gitFileChanges, accountId, top, "");
      logger.info("Performed full sync dry-run for account:" + accountId);
      return gitFileChanges;
    } catch (Exception ex) {
      logger.error("Failed to perform full sync dry run for account: " + accountId, ex);
    }
    return new ArrayList<>();
  }

  @Override
  public boolean handleChangeSet(YamlChangeSet yamlChangeSet) {
    YamlGitConfig yamlGitConfig = get(yamlChangeSet.getAccountId(), yamlChangeSet.getAccountId());
    List<GitFileChange> gitFileChanges = yamlChangeSet.getGitFileChanges();

    if (yamlGitConfig == null) {
      throw new WingsException(ErrorCode.YAML_GIT_SYNC_ERROR);
    }

    logger.info("Change set [{}] files", yamlChangeSet.getUuid());

    String waitId = UUIDGenerator.getUuid();
    DelegateTask delegateTask =
        aDelegateTask()
            .withTaskType(TaskType.GIT_COMMAND)
            .withAccountId(yamlChangeSet.getAccountId())
            .withAppId(GLOBAL_APP_ID)
            .withWaitId(waitId)
            .withParameters(new Object[] {GitCommandType.COMMIT_AND_PUSH, yamlGitConfig.getGitConfig(),
                secretManager.getEncryptionDetails(yamlGitConfig.getGitConfig(), GLOBAL_APP_ID, null),
                GitCommitRequest.builder().gitFileChanges(gitFileChanges).build()})
            .build();

    waitNotifyEngine.waitForAll(
        new GitCommandCallback(yamlChangeSet.getAccountId(), yamlChangeSet.getUuid(), yamlGitConfig.getUuid()), waitId);
    delegateService.queueTask(delegateTask);
    return true;
  }

  @Override
  public void processWebhookPost(String accountId, String webhookToken, YamlWebHookPayload yamlWebHookPayload) {
    try {
      YamlGitConfig yamlGitConfig = wingsPersistence.createQuery(YamlGitConfig.class)
                                        .field("accountId")
                                        .equal(accountId)
                                        .field("webhookToken")
                                        .equal(webhookToken)
                                        .get();
      if (yamlGitConfig == null) {
        logger.error("Invalid git webhook request [{}]", webhookToken);
        return;
      }

      String headCommit = yamlWebHookPayload.getHeadCommit().getId();

      if (!isCommitAlreadyProcessed(accountId, headCommit)) {
        GitCommit gitCommit = wingsPersistence.createQuery(GitCommit.class)
                                  .field("accountId")
                                  .equal(accountId)
                                  .field("yamlGitConfigId")
                                  .equal(yamlGitConfig.getUuid())
                                  .field("status")
                                  .equal(Status.COMPLETED)
                                  .order("-lastUpdatedAt")
                                  .get();

        String processedCommit = gitCommit == null ? null : gitCommit.getCommitId();

        String waitId = UUIDGenerator.getUuid();
        DelegateTask delegateTask =
            aDelegateTask()
                .withTaskType(TaskType.GIT_COMMAND)
                .withAccountId(accountId)
                .withAppId(GLOBAL_APP_ID)
                .withWaitId(waitId)
                .withParameters(new Object[] {GitCommandType.DIFF, yamlGitConfig.getGitConfig(),
                    secretManager.getEncryptionDetails(yamlGitConfig.getGitConfig(), GLOBAL_APP_ID, null),
                    GitDiffRequest.builder().lastProcessedCommitId(processedCommit).build()})
                .build();

        waitNotifyEngine.waitForAll(new GitCommandCallback(accountId, null, yamlGitConfig.getUuid()), waitId);
        delegateService.queueTask(delegateTask);
      }
    } catch (Exception ex) {
      logger.error("Error while processing git webhook post", ex);
    }
  }

  private boolean isCommitAlreadyProcessed(String accountId, String headCommit) {
    GitCommit gitCommit = wingsPersistence.createQuery(GitCommit.class)
                              .field("accountId")
                              .equal(accountId)
                              .field("commitId")
                              .equal(headCommit)
                              .field("status")
                              .equal(Status.COMPLETED)
                              .get();
    if (gitCommit != null) {
      logger.info("Commit [id:{}] already processed [status:{}] on [date:{}] mode:[{}]", gitCommit.getCommitId(),
          gitCommit.getStatus(), gitCommit.getLastUpdatedAt(), gitCommit.getYamlChangeSet().isGitToHarness());
      return true;
    }
    return false;
  }

  public GitSyncWebhook getWebhook(String entityId, String accountId) {
    GitSyncWebhook gsw = wingsPersistence.createQuery(GitSyncWebhook.class)
                             .field("entityId")
                             .equal(entityId)
                             .field("accountId")
                             .equal(accountId)
                             .get();

    if (gsw != null) {
      return gsw;
    } else {
      // create a new GitSyncWebhook, save to Mongo and return it
      String newWebhookToken = CryptoUtil.secureRandAlphaNumString(40);
      gsw = GitSyncWebhook.builder().accountId(accountId).entityId(entityId).webhookToken(newWebhookToken).build();
      return wingsPersistence.saveAndGet(GitSyncWebhook.class, gsw);
    }
  }

  @Override
  public GitCommit saveCommit(GitCommit gitCommit) {
    return wingsPersistence.saveAndGet(GitCommit.class, gitCommit);
  }
}
