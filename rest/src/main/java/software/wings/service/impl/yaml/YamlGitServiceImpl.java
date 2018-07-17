package software.wings.service.impl.yaml;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.yaml.YamlConstants.GIT_YAML_LOG_PREFIX;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;
import static software.wings.exception.WingsException.USER;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.data.structure.EmptyPredicate;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.DelegateTask;
import software.wings.beans.ErrorCode;
import software.wings.beans.GitCommit;
import software.wings.beans.GitConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.TaskType;
import software.wings.beans.alert.AlertData;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.GitConnectionErrorAlert;
import software.wings.beans.alert.GitSyncErrorAlert;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitCommand.GitCommandType;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.beans.yaml.GitCommitRequest;
import software.wings.beans.yaml.GitDiffRequest;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.GitFileChange.Builder;
import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.exception.YamlProcessingException;
import software.wings.exception.YamlProcessingException.ChangeWithErrorMsg;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.ManagerDecryptionService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.validation.executable.ValidateOnExecution;
/**
 * The type Yaml git sync service.
 */
@ValidateOnExecution
public class YamlGitServiceImpl implements YamlGitService {
  private static final Logger logger = LoggerFactory.getLogger(YamlGitServiceImpl.class);

  /**
   * The constant SETUP_ENTITY_ID.
   */
  public static final String SETUP_ENTITY_ID = "setup";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject private YamlDirectoryService yamlDirectoryService;
  @Inject private YamlService yamlService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private SecretManager secretManager;
  @Inject private ExecutorService executorService;
  @Inject private DelegateService delegateService;
  @Inject private AlertService alertService;
  @Inject private SettingsService settingsService;
  @Inject private ManagerDecryptionService managerDecryptionService;

  /**
   * Gets the yaml git sync info by entityId
   *
   * @return the rest response
   */
  @Override
  public YamlGitConfig get(String accountId, String entityId) {
    return wingsPersistence.createQuery(YamlGitConfig.class).filter("accountId", accountId).get();
  }

  /**
   * Creates a new yaml git sync info by object type and entitytId (uuid)
   *
   * @param ygs the yamlGitSync info
   * @return the rest response
   */
  @Override
  public YamlGitConfig save(YamlGitConfig ygs) {
    GitConfig gitConfig = getGitConfig(ygs);
    gitConfig.setDecrypted(true);
    validateGit(gitConfig);
    gitConfig.setDecrypted(false);

    YamlGitConfig yamlGitSync = wingsPersistence.saveAndGet(YamlGitConfig.class, ygs);
    executorService.submit(() -> fullSync(ygs.getAccountId(), true));
    return yamlGitSync;
  }

  private GitConfig getGitConfig(YamlGitConfig ygs) {
    SettingAttribute settingAttribute = null;
    if (!EmptyPredicate.isEmpty(ygs.getSshSettingId())) {
      settingAttribute = settingsService.get(ygs.getSshSettingId());
      HostConnectionAttributes attributeValue = (HostConnectionAttributes) settingAttribute.getValue();
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails(attributeValue, GLOBAL_APP_ID, null);
      managerDecryptionService.decrypt(attributeValue, encryptionDetails);
    }

    return ygs.getGitConfig(settingAttribute);
  }

  /**
   * Updates the yaml git sync info by object type and entitytId (uuid)
   *
   * @param ygs the yamlGitSync info
   * @return the rest response
   */
  @Override
  public YamlGitConfig update(YamlGitConfig ygs) {
    return save(ygs);
  }

  private void validateGit(GitConfig gitConfig) {
    /*
    1. Invalid repoUrl
    2. Invalid credentials
    3. No write access
    4. Branch doesn't exist
    */

    // Validate if SSH key is present
    if (gitConfig.isKeyAuth()) {
      if (gitConfig.getSshSettingAttribute() == null) {
        throw new InvalidRequestException("SSH key can not be empty");
      }
    }
    // Validate if username and password present
    else {
      if (gitConfig.getUsername() == null || gitConfig.getPassword() == null) {
        throw new InvalidRequestException("Username and password can not be empty", USER);
      }
    }

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
      logger.info(GIT_YAML_LOG_PREFIX + "GitConfigValidation [{}]", gitCommandExecutionResponse);
      if (gitCommandExecutionResponse.getGitCommandStatus().equals(GitCommandStatus.FAILURE)) {
        raiseAlertForGitFailure(gitConfig.getAccountId(), GLOBAL_APP_ID, ErrorCode.GIT_CONNECTION_ERROR,
            gitCommandExecutionResponse.getErrorMessage());
        throw new InvalidRequestException(gitCommandExecutionResponse.getErrorMessage());
      } else {
        closeAlertForGitFailureIfOpen(gitConfig.getAccountId(), GLOBAL_APP_ID, AlertType.GitConnectionError,
            GitConnectionErrorAlert.builder().accountId(gitConfig.getAccountId()).build());
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void fullSync(String accountId, boolean forcePush) {
    logger.info(GIT_YAML_LOG_PREFIX + "Performing git full-sync for account {} " + accountId);
    YamlGitConfig yamlGitConfig = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (yamlGitConfig != null) {
      try {
        FolderNode top = yamlDirectoryService.getDirectory(accountId, SETUP_ENTITY_ID, false, null);
        List<GitFileChange> gitFileChanges = new ArrayList<>();
        gitFileChanges =
            yamlDirectoryService.traverseDirectory(gitFileChanges, accountId, top, "", true, true, Optional.empty());
        discardGitSyncErrorForFullSync(accountId);

        if (gitFileChanges.size() > 0 && forcePush) {
          gitFileChanges.add(0,
              GitFileChange.Builder.aGitFileChange()
                  .withAccountId(accountId)
                  .withChangeType(ChangeType.DELETE)
                  .withFilePath(YamlConstants.SETUP_FOLDER)
                  .build());
        }

        syncFiles(accountId, gitFileChanges, forcePush);
        logger.info(GIT_YAML_LOG_PREFIX + "Performed git full-sync for account {} successfully" + accountId);
      } catch (Exception ex) {
        logger.error(
            GIT_YAML_LOG_PREFIX + "Failed to perform git full-sync for account {} ", yamlGitConfig.getAccountId(), ex);
      }
    }
  }

  @Override
  public void syncFiles(String accountId, List<GitFileChange> gitFileChangeList, boolean forcePush) {
    YamlGitConfig yamlGitConfig = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (yamlGitConfig != null) {
      try {
        YamlChangeSet yamlChangeSet = YamlChangeSet.builder()
                                          .accountId(accountId)
                                          .status(Status.QUEUED)
                                          .forcePush(forcePush)
                                          .gitFileChanges(gitFileChangeList)
                                          .appId(GLOBAL_APP_ID)
                                          .fullSync(true)
                                          .build();

        // Get all Queued. Failed tasks to be processed before this full sync one.
        // find most recent change set with Completed status and get all changesets after that in Queued/Failed state
        List<YamlChangeSet> yamlChangeSets = yamlChangeSetService.getChangeSetsToBeMarkedSkipped(accountId);
        List<String> yamlChangesetIdsToBeSkipped = new ArrayList<>();
        yamlChangesetIdsToBeSkipped.addAll(
            yamlChangeSets.stream().map(changeSet -> changeSet.getUuid()).collect(toList()));

        yamlChangeSetService.save(yamlChangeSet);
        // mark these change sets as Skipped
        yamlChangeSetService.updateStatusForGivenYamlChangeSets(
            accountId, Status.SKIPPED, Arrays.asList(Status.QUEUED), yamlChangesetIdsToBeSkipped);
      } catch (Exception ex) {
        logger.error(GIT_YAML_LOG_PREFIX + "Failed to sync files for account {} ", yamlGitConfig.getAccountId(), ex);
      }
    }
  }

  @Override
  public List<GitFileChange> performFullSyncDryRun(String accountId) {
    try {
      logger.info("Performing full-sync dry-run for account {}", accountId);
      FolderNode top = yamlDirectoryService.getDirectory(accountId, SETUP_ENTITY_ID, false, null);
      List<GitFileChange> gitFileChanges = new ArrayList<>();
      yamlDirectoryService.traverseDirectory(gitFileChanges, accountId, top, "", false, true, Optional.empty());
      logger.info("Performed full-sync dry-run for account {}", accountId);
      return gitFileChanges;
    } catch (Exception ex) {
      logger.error("Failed to perform full-sync dry-run for account {}", accountId, ex);
    }
    return new ArrayList<>();
  }

  @Override
  public List<String> getAllYamlErrorsForAccount(String accountId) {
    try {
      logger.info("Getting all Yaml errors for account {}", accountId);
      FolderNode top = yamlDirectoryService.getDirectory(accountId, SETUP_ENTITY_ID, false, null);
      List<GitFileChange> gitFileChanges = new ArrayList<>();
      List<String> errorLog = new ArrayList<>();
      yamlDirectoryService.traverseDirectory(gitFileChanges, accountId, top, "", false, false, Optional.of(errorLog));
      logger.info("Got all Yaml errors for account {}", accountId);
      return errorLog;
    } catch (Exception ex) {
      logger.error("Failed to get all Yaml errors for account {}", accountId, ex);
    }
    return new ArrayList<>();
  }

  private List<Account> getAllAccounts() {
    PageRequest<Account> request = aPageRequest()
                                       .withLimit(UNLIMITED)
                                       .addFieldsIncluded("uuid")
                                       .addFilter("appId", EQ, "__GLOBAL_APP_ID__")
                                       .build();
    return accountService.list(request);
  }

  @Override
  public void performFullSyncDryRunOnAllAccounts() {
    List<Account> accounts = getAllAccounts();
    accounts.forEach(account -> performFullSyncDryRun(account.getUuid()));
  }

  @Override
  public List<String> getAllYamlErrorsForAllAccounts() {
    List<Account> accounts = getAllAccounts();
    List<String> allErrors = new ArrayList<>();
    accounts.forEach(account -> allErrors.addAll(getAllYamlErrorsForAccount(account.getUuid())));
    return allErrors;
  }

  @Override
  public boolean handleChangeSet(List<YamlChangeSet> yamlChangeSets, String accountId) {
    YamlGitConfig yamlGitConfig = get(accountId, accountId);
    if (yamlGitConfig == null) {
      logger.warn(GIT_YAML_LOG_PREFIX + "YamlGitConfig is null for accountId: " + accountId);
      throw new WingsException(ErrorCode.YAML_GIT_SYNC_ERROR);
    }

    List<String> yamlChangeSetIds =
        yamlChangeSets.stream().map(yamlChangeSet -> yamlChangeSet.getUuid()).collect(toList());
    String mostRecentYamlChangesetId = yamlChangeSets.get(yamlChangeSets.size() - 1).getUuid();
    List<GitFileChange> gitFileChanges = getGitFileChangesToBeApplied(yamlChangeSets);
    checkForValidNameSyntax(gitFileChanges);

    // @TODO_GITLOG add accountId here
    logger.info(GIT_YAML_LOG_PREFIX + "Creating COMMIT_AND_PUSH git delegate task for account: {}", accountId);

    StringBuilder builder = new StringBuilder();
    yamlChangeSets.forEach(yamlChangeSet -> builder.append(yamlChangeSet.getUuid()).append("  "));
    logger.info(GIT_YAML_LOG_PREFIX + "Change sets [{}] files", builder.toString());

    String waitId = generateUuid();
    GitConfig gitConfig = getGitConfig(yamlGitConfig);

    if (yamlChangeSets.size() > 1) {
      logger.info(new StringBuilder(GIT_YAML_LOG_PREFIX)
                      .append("Processing YamlChangeSets for account: ")
                      .append(accountId)
                      .append(" from ")
                      .append(yamlChangeSets.get(0).getUuid())
                      .append(" - ")
                      .append(yamlChangeSets.get(yamlChangeSets.size() - 1).getUuid())
                      .toString());
    }
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.GIT_COMMAND)
                                    .withAccountId(accountId)
                                    .withAppId(GLOBAL_APP_ID)
                                    .withWaitId(waitId)
                                    .withParameters(new Object[] {GitCommandType.COMMIT_AND_PUSH, gitConfig,
                                        secretManager.getEncryptionDetails(gitConfig, GLOBAL_APP_ID, null),
                                        GitCommitRequest.builder()
                                            .gitFileChanges(gitFileChanges)
                                            .forcePush(true)
                                            .yamlChangeSetIds(yamlChangeSetIds)
                                            .build()})
                                    .withTimeout(TimeUnit.MINUTES.toMillis(20))
                                    .build();

    waitNotifyEngine.waitForAll(
        new GitCommandCallback(accountId, mostRecentYamlChangesetId, yamlGitConfig.getUuid()), waitId);
    delegateService.queueTask(delegateTask);
    return true;
  }

  private List<GitFileChange> getGitFileChangesToBeApplied(List<YamlChangeSet> yamlChangeSets) {
    Map<String, GitFileChange> gitFileChangeToFilePathMap = new LinkedHashMap<>();

    // Making sure order is maintianed, so yamlChangeSets create at later point of time, replaces changes
    // made by earlier ones
    for (int index = 0; index < yamlChangeSets.size(); index++) {
      yamlChangeSets.get(index).getGitFileChanges().forEach(
          gitFileChange -> updateFileChangeToFilePathMap(gitFileChangeToFilePathMap, gitFileChange));
    }

    return new ArrayList<>(gitFileChangeToFilePathMap.values());
  }

  private void updateFileChangeToFilePathMap(
      Map<String, GitFileChange> gitFileChangeToFilePathMap, GitFileChange gitFileChange) {
    String filePath = gitFileChange.getFilePath();
    if (gitFileChangeToFilePathMap.containsKey(filePath)) {
      gitFileChangeToFilePathMap.remove(filePath);
    }
    gitFileChangeToFilePathMap.put(filePath, gitFileChange);
  }

  /**
   * Check filePath is valid.
   * @param gitFileChanges
   */
  private void checkForValidNameSyntax(List<GitFileChange> gitFileChanges) {
    // Get all yamlTypes having non-empty filepath prefixes (these yaml types represent different file paths)
    List<YamlType> folderYamlTypes =
        Arrays.stream(YamlType.values()).filter(yamlType -> isNotEmpty(yamlType.getPathExpression())).collect(toList());

    // make sure, all filepaths to be synced with git are in proper format
    // e.g. Setup/Application/app_name/index.yaml is valid one, but
    // Setup/Application/app/name/index.yaml is invalid. (this case is happening id app was names as "app/name")
    // we do not want to allow this scenario.
    gitFileChanges.forEach(gitFileChange
        -> matchPathPrefix(gitFileChange.getFilePath().charAt(0) == '/' ? gitFileChange.getFilePath().substring(1)
                                                                        : gitFileChange.getFilePath(),
            folderYamlTypes));
  }

  private void matchPathPrefix(String filePath, List<YamlType> folderYamlTypes) {
    // only check for file sand not directories
    if (filePath.endsWith(YamlConstants.YAML_EXTENSION)) {
      if (!folderYamlTypes.stream().anyMatch(
              yamlType -> Pattern.compile(yamlType.getPathExpression()).matcher(filePath).matches())) {
        throw new WingsException(
            "Invalid entity name, entity can not contain / in the name. Caused invalid file path: " + filePath);
      }
    }
  }

  @Override
  public void processWebhookPost(String accountId, String webhookToken, YamlWebHookPayload yamlWebHookPayload) {
    try {
      YamlGitConfig yamlGitConfig = wingsPersistence.createQuery(YamlGitConfig.class)
                                        .filter("accountId", accountId)
                                        .filter("webhookToken", webhookToken)
                                        .get();
      if (yamlGitConfig == null) {
        logger.error(GIT_YAML_LOG_PREFIX + "Invalid git webhook request [{}]", webhookToken);
        return;
      }

      GitCommit gitCommit = wingsPersistence.createQuery(GitCommit.class)
                                .filter("accountId", accountId)
                                .filter("yamlGitConfigId", yamlGitConfig.getUuid())
                                .filter("status", Status.COMPLETED)
                                .order("-lastUpdatedAt")
                                .get();

      String processedCommit = gitCommit == null ? null : gitCommit.getCommitId();

      String waitId = generateUuid();
      GitConfig gitConfig = getGitConfig(yamlGitConfig);
      DelegateTask delegateTask = aDelegateTask()
                                      .withTaskType(TaskType.GIT_COMMAND)
                                      .withAccountId(accountId)
                                      .withAppId(GLOBAL_APP_ID)
                                      .withWaitId(waitId)
                                      .withParameters(new Object[] {GitCommandType.DIFF, gitConfig,
                                          secretManager.getEncryptionDetails(gitConfig, GLOBAL_APP_ID, null),
                                          GitDiffRequest.builder().lastProcessedCommitId(processedCommit).build()})
                                      .build();

      waitNotifyEngine.waitForAll(new GitCommandCallback(accountId, null, yamlGitConfig.getUuid()), waitId);
      delegateService.queueTask(delegateTask);

    } catch (Exception ex) {
      logger.error(GIT_YAML_LOG_PREFIX + "Error while processing git webhook post", ex);
    }
  }

  @Override
  public boolean isCommitAlreadyProcessed(String accountId, String headCommit) {
    GitCommit gitCommit = wingsPersistence.createQuery(GitCommit.class)
                              .filter("accountId", accountId)
                              .filter("commitId", headCommit)
                              .filter("status", Status.COMPLETED)
                              .get();
    if (gitCommit != null) {
      logger.info(GIT_YAML_LOG_PREFIX + "Commit [id:{}] already processed [status:{}] on [date:{}] mode:[{}]",
          gitCommit.getCommitId(), gitCommit.getStatus(), gitCommit.getLastUpdatedAt(),
          gitCommit.getYamlChangeSet().isGitToHarness());
      return true;
    }
    return false;
  }

  public GitSyncWebhook getWebhook(String entityId, String accountId) {
    GitSyncWebhook gsw = wingsPersistence.createQuery(GitSyncWebhook.class)
                             .filter("entityId", entityId)
                             .filter("accountId", accountId)
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
  public void processFailedChanges(
      String accountId, Map<String, ChangeWithErrorMsg> failedYamlFileChangeMap, boolean gitToHarness) {
    if (failedYamlFileChangeMap.size() > 0) {
      failedYamlFileChangeMap.values().forEach(changeWithErrorMsg
          -> upsertGitSyncErrors(changeWithErrorMsg.getChange(), changeWithErrorMsg.getErrorMsg(), false));
      alertService.openAlert(
          accountId, GLOBAL_APP_ID, AlertType.GitSyncError, getGitSyncErrorAlert(accountId, gitToHarness));
    }
  }

  @Override
  public void raiseAlertForGitFailure(String accountId, String appId, ErrorCode errorCode, String errorMessage) {
    if (ErrorCode.GIT_CONNECTION_ERROR.equals(errorCode)) {
      alertService.openAlert(
          accountId, appId, AlertType.GitConnectionError, getGitConnectionErrorAlert(accountId, errorMessage));
    } else {
      alertService.openAlert(
          accountId, appId, AlertType.GitSyncError, getGitSyncErrorAlert(accountId, errorMessage, false));
    }
  }

  @Override
  public void closeAlertForGitFailureIfOpen(String accountId, String appId, AlertType alertType, AlertData alertData) {
    alertService.closeAlert(accountId, appId, alertType, alertData);
  }

  private GitSyncErrorAlert getGitSyncErrorAlert(String accountId, boolean gitToHarness) {
    return GitSyncErrorAlert.builder()
        .accountId(accountId)
        .message("Unable to process changes from Git")
        .gitToHarness(gitToHarness)
        .build();
  }

  private GitSyncErrorAlert getGitSyncErrorAlert(String accountId, String errorMessage, boolean gitToHarness) {
    return GitSyncErrorAlert.builder().accountId(accountId).message(errorMessage).gitToHarness(gitToHarness).build();
  }

  private GitConnectionErrorAlert getGitConnectionErrorAlert(String accountId, String message) {
    return GitConnectionErrorAlert.builder().accountId(accountId).message(message).build();
  }

  public <T extends Change> void upsertGitSyncErrors(T failedChange, String errorMessage, boolean fullSyncPath) {
    Query<GitSyncError> failedQuery = wingsPersistence.createQuery(GitSyncError.class)
                                          .filter("accountId", failedChange.getAccountId())
                                          .filter("yamlFilePath", failedChange.getFilePath());
    GitFileChange failedGitFileChange = (GitFileChange) failedChange;
    String failedCommitId = failedGitFileChange.getCommitId() != null ? failedGitFileChange.getCommitId() : "";
    UpdateOperations<GitSyncError> failedUpdateOperations =
        wingsPersistence.createUpdateOperations(GitSyncError.class)
            .set("accountId", failedChange.getAccountId())
            .set("yamlFilePath", failedChange.getFilePath())
            .set("yamlContent", failedChange.getFileContent())
            .set("gitCommitId", failedCommitId)
            .set("changeType", failedChange.getChangeType().name())
            .set("failureReason",
                errorMessage != null ? errorMessage : "Reason could not be captured. Logs might have some info")
            .set("fullSyncPath", fullSyncPath);

    wingsPersistence.upsert(failedQuery, failedUpdateOperations);
  }

  @Override
  public void removeGitSyncErrors(String accountId, List<GitFileChange> gitFileChangeList, boolean gitToHarness) {
    List<String> yamlFilePathList = gitFileChangeList.stream().map(GitFileChange::getFilePath).collect(toList());
    Query query = wingsPersistence.createAuthorizedQuery(GitSyncError.class);
    query.filter("accountId", accountId);
    query.field("yamlFilePath").in(yamlFilePathList);
    wingsPersistence.delete(query);
    closeAlertIfApplicable(accountId, gitToHarness);
  }

  @Override
  public RestResponse<List<GitSyncError>> listGitSyncErrors(String accountId) {
    PageRequest<GitSyncError> pageRequest = aPageRequest()
                                                .addFilter("accountId", EQ, accountId)
                                                .withLimit("500")
                                                .addOrder("lastUpdatedAt", OrderType.ASC)
                                                .build();
    PageResponse<GitSyncError> response = wingsPersistence.query(GitSyncError.class, pageRequest);
    return RestResponse.Builder.aRestResponse().withResource(response.getResponse()).build();
  }

  @Override
  public long getGitSyncErrorCount(String accountId) {
    PageRequest<GitSyncError> pageRequest = aPageRequest().addFilter("accountId", EQ, accountId).build();
    return wingsPersistence.getCount(GitSyncError.class, pageRequest);
  }

  @Override
  public RestResponse discardGitSyncError(String accountId, String errorId) {
    Query query = wingsPersistence.createAuthorizedQuery(GitSyncError.class);
    query.filter("accountId", accountId);
    query.filter("_id", errorId);
    wingsPersistence.delete(query);
    closeAlertIfApplicable(accountId, false);
    return RestResponse.Builder.aRestResponse().build();
  }

  @Override
  public RestResponse discardGitSyncErrorsForGivenIds(String accountId, List<String> errorIds) {
    Query query = wingsPersistence.createAuthorizedQuery(GitSyncError.class);
    query.filter("accountId", accountId);
    query.field("_id").in(errorIds);
    wingsPersistence.delete(query);
    closeAlertIfApplicable(accountId, false);
    return RestResponse.Builder.aRestResponse().build();
  }

  @Override
  public RestResponse discardAllGitSyncError(String accountId) {
    Query query = wingsPersistence.createAuthorizedQuery(GitSyncError.class);
    query.filter("accountId", accountId);
    wingsPersistence.delete(query);
    closeAlertIfApplicable(accountId, false);
    return RestResponse.Builder.aRestResponse().build();
  }

  @Override
  public RestResponse discardGitSyncErrorForFullSync(String accountId) {
    Query query = wingsPersistence.createAuthorizedQuery(GitSyncError.class);
    query.filter("accountId", accountId);
    query.filter("fullSyncPath", true);
    wingsPersistence.delete(query);
    closeAlertIfApplicable(accountId, false);
    return RestResponse.Builder.aRestResponse().build();
  }

  private void closeAlertIfApplicable(String accountId, boolean gitToHarness) {
    if (getGitSyncErrorCount(accountId) == 0) {
      alertService.closeAlert(
          accountId, GLOBAL_APP_ID, AlertType.GitSyncError, getGitSyncErrorAlert(accountId, gitToHarness));
    }
  }

  @Override
  public RestResponse fixGitSyncErrors(String accountId, String yamlFilePath, String newYamlContent) {
    logger.info(
        GIT_YAML_LOG_PREFIX + "Fixing git sync errors for account {} and yaml file {}", accountId, yamlFilePath);
    RestResponse<List<GitSyncError>> listRestResponse = listGitSyncErrors(accountId);
    List<GitSyncError> syncErrorList = listRestResponse.getResource();
    if (isEmpty(syncErrorList)) {
      logger.warn("No sync errors found to process for account {}", accountId);
      return RestResponse.Builder.aRestResponse().build();
    }

    List<GitFileChange> gitFileChangeList = Lists.newArrayList();

    syncErrorList.forEach(syncError -> {
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
      logger.info(GIT_YAML_LOG_PREFIX + "Processing fix Git Sync Errors for account {}", accountId);
      yamlService.processChangeSet(gitFileChangeList);
      logger.info(GIT_YAML_LOG_PREFIX + "Processed fix Git Sync Errors for account {}", accountId);
      removeGitSyncErrors(accountId, gitFileChangeList, false);
    } catch (YamlProcessingException ex) {
      logger.warn(GIT_YAML_LOG_PREFIX + "Unable to process Git sync errors for account {}", accountId, ex);
      // gitToHarness is false, as this action is initiated from UI
      processFailedChanges(accountId, ex.getFailedYamlFileChangeMap(), false);
    }

    return RestResponse.Builder.aRestResponse().build();
  }
}
