package software.wings.service.impl.yaml;

import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.ErrorCode;
import software.wings.beans.GitCommit;
import software.wings.beans.GitConfig;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.TaskType;
import software.wings.beans.alert.GitSyncErrorAlert;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitCommand.GitCommandType;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.beans.yaml.GitCommitRequest;
import software.wings.beans.yaml.GitDiffRequest;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.GitFileChange.Builder;
import software.wings.common.UUIDGenerator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.exception.YamlProcessingException;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.sync.YamlService;
import software.wings.utils.CryptoUtil;
import software.wings.utils.Util;
import software.wings.waitnotify.WaitNotifyEngine;
import software.wings.yaml.directory.FolderNode;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.gitSync.GitSyncWebhook;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlChangeSet.Status;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
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
  @Inject private YamlService yamlService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private SecretManager secretManager;
  @Inject private ExecutorService executorService;
  @Inject private DelegateService delegateService;
  @Inject private AlertService alertService;

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
        throw new WingsException(ErrorCode.INVALID_REQUEST)
            .addParam("message", gitCommandExecutionResponse.getErrorMessage());
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
        syncFiles(accountId, gitFileChanges);
      } catch (Exception ex) {
        logger.error("Failed to push directory for account {} ", yamlGitConfig.getAccountId(), ex);
      }
    }
  }

  @Override
  public void syncFiles(String accountId, List<GitFileChange> gitFileChangeList) {
    YamlGitConfig yamlGitConfig = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (yamlGitConfig != null) {
      try {
        YamlChangeSet yamlChangeSet = YamlChangeSet.builder()
                                          .accountId(accountId)
                                          .status(Status.QUEUED)
                                          .gitFileChanges(gitFileChangeList)
                                          .appId(GLOBAL_APP_ID)
                                          .build();
        yamlChangeSetService.save(yamlChangeSet);
      } catch (Exception ex) {
        logger.error("Failed to sync files for account {} ", yamlGitConfig.getAccountId(), ex);
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

  @Override
  public void processFailedOrUnprocessedChanges(
      List<GitFileChange> orderedFileChanges, Change failedChange, String errorMessage) {
    AtomicInteger index = new AtomicInteger(-1);

    Optional<GitFileChange> fileChangeOptional =
        orderedFileChanges.stream()
            .filter(gitFileChange -> {
              index.incrementAndGet();
              return gitFileChange.getFilePath().equals(failedChange.getFilePath());
            })
            .findFirst();

    if (fileChangeOptional.isPresent()) {
      List<GitFileChange> failedOrUnprocessedChanges =
          orderedFileChanges.subList(index.intValue(), orderedFileChanges.size());
      upsertGitSyncErrors(failedOrUnprocessedChanges, errorMessage);
    } else {
      logger.error("Failed yaml not found in the result set, only adding it to the list of failures");
      upsertGitSyncErrors(Arrays.asList(failedChange), errorMessage);
    }

    //    alertService.openAlert(failedChange.getAccountId(), GLOBAL_APP_ID, AlertType.GitSyncError,
    //        getGitSyncErrorAlert(failedChange.getAccountId()));
  }

  private GitSyncErrorAlert getGitSyncErrorAlert(String accountId) {
    return GitSyncErrorAlert.builder().accountId(accountId).message("Unable to process changes from Git").build();
  }

  private void upsertGitSyncErrors(List<? extends Change> failedOrUnprocessedChanges, String errorMessage) {
    failedOrUnprocessedChanges.stream().forEach(change -> {
      Query<GitSyncError> query = wingsPersistence.createQuery(GitSyncError.class)
                                      .field("accountId")
                                      .equal(change.getAccountId())
                                      .field("yamlFilePath")
                                      .equal(change.getFilePath());
      GitFileChange gitFileChange = (GitFileChange) change;
      String commitId = gitFileChange.getCommitId() != null ? gitFileChange.getCommitId() : "";
      UpdateOperations<GitSyncError> updateOperations = wingsPersistence.createUpdateOperations(GitSyncError.class)
                                                            .set("accountId", change.getAccountId())
                                                            .set("yamlFilePath", change.getFilePath())
                                                            .set("yamlContent", change.getFileContent())
                                                            .set("gitCommitId", commitId)
                                                            .set("changeType", change.getChangeType().name())
                                                            .set("failureReason", errorMessage);

      wingsPersistence.upsert(query, updateOperations);
    });
  }

  @Override
  public void removeGitSyncErrors(String accountId, List<GitFileChange> gitFileChangeList) {
    List<String> yamlFilePathList =
        gitFileChangeList.stream().map(GitFileChange::getFilePath).collect(Collectors.toList());
    Query query = wingsPersistence.createAuthorizedQuery(GitSyncError.class);
    query.field("accountId").equal(accountId);
    query.field("yamlFilePath").in(yamlFilePathList);
    wingsPersistence.delete(query);
    closeAlertIfApplicable(accountId);
  }

  @Override
  public RestResponse<List<GitSyncError>> listGitSyncErrors(String accountId) {
    PageRequest<GitSyncError> pageRequest =
        PageRequest.Builder.aPageRequest().addFilter("accountId", Operator.EQ, accountId).withLimit("500").build();
    PageResponse<GitSyncError> response = wingsPersistence.query(GitSyncError.class, pageRequest);
    return RestResponse.Builder.aRestResponse().withResource(response.getResponse()).build();
  }

  @Override
  public long getGitSyncErrorCount(String accountId) {
    PageRequest<GitSyncError> pageRequest =
        PageRequest.Builder.aPageRequest().addFilter("accountId", Operator.EQ, accountId).build();
    return wingsPersistence.getCount(GitSyncError.class, pageRequest);
  }

  @Override
  public RestResponse discardGitSyncError(String accountId, String yamlFilePath) {
    Query query = wingsPersistence.createAuthorizedQuery(GitSyncError.class);
    query.field("accountId").equal(accountId);
    query.field("yamlFilePath").equal(yamlFilePath);
    wingsPersistence.delete(query);
    closeAlertIfApplicable(accountId);
    return RestResponse.Builder.aRestResponse().build();
  }

  private void closeAlertIfApplicable(String accountId) {
    //    if (getGitSyncErrorCount(accountId) == 0) {
    //      alertService.closeAlert(accountId, GLOBAL_APP_ID, AlertType.GitSyncError, getGitSyncErrorAlert(accountId));
    //    }
  }

  @Override
  public RestResponse fixGitSyncErrors(String accountId, String yamlFilePath, String newYamlContent) {
    RestResponse<List<GitSyncError>> listRestResponse = listGitSyncErrors(accountId);
    List<GitSyncError> syncErrorList = listRestResponse.getResource();
    if (Util.isEmpty(syncErrorList)) {
      logger.error("No sync errors found to process for account {}", accountId);
      return RestResponse.Builder.aRestResponse().build();
    }

    List<GitFileChange> gitFileChangeList = Lists.newArrayList();

    syncErrorList.stream().forEach(syncError -> {

      String currentYamlFilePath = syncError.getYamlFilePath();
      String yamlContent;
      if (currentYamlFilePath.equals(yamlFilePath)) {
        yamlContent = newYamlContent;
      } else {
        yamlContent = syncError.getYamlContent();
      }

      ChangeType changeType = Util.getEnumFromString(ChangeType.class, syncError.getChangeType());
      GitFileChange gitFileChange = Builder.aGitFileChange()
                                        .withAccountId(accountId)
                                        .withFilePath(syncError.getYamlFilePath())
                                        .withFileContent(yamlContent)
                                        .withChangeType(changeType)
                                        .build();
      gitFileChangeList.add(gitFileChange);
    });

    try {
      List<ChangeContext> fileChangeContexts = yamlService.processChangeSet(gitFileChangeList);
      logger.info("Processed ChangeSet: [{}]", fileChangeContexts);
      removeGitSyncErrors(accountId, gitFileChangeList);
    } catch (YamlProcessingException ex) {
      logger.error("Processing changeSet failed", ex);
      processFailedOrUnprocessedChanges(gitFileChangeList, ex.getChange(), ex.getMessage());
    }

    return RestResponse.Builder.aRestResponse().build();
  }
}
