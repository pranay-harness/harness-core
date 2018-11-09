package software.wings.service.impl.yaml;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.GitCommit.STATUS_KEY;
import static software.wings.beans.GitCommit.YAML_GIT_CONFIG_IDS_KEY;
import static software.wings.beans.GitCommit.YAML_GIT_CONFIG_ID_KEY;
import static software.wings.beans.yaml.YamlConstants.APPLICATIONS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.APPLICATION_FOLDER_PATH;
import static software.wings.beans.yaml.YamlConstants.ARTIFACT_SOURCES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.CLOUD_PROVIDERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.COLLABORATION_PROVIDERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.DEFAULTS_YAML;
import static software.wings.beans.yaml.YamlConstants.GIT_YAML_LOG_PREFIX;
import static software.wings.beans.yaml.YamlConstants.LOAD_BALANCERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.NOTIFICATION_GROUPS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.beans.yaml.YamlConstants.SETUP_FOLDER;
import static software.wings.beans.yaml.YamlConstants.VERIFICATION_PROVIDERS_FOLDER;
import static software.wings.service.impl.WebHookServiceImpl.X_BIT_BUCKET_EVENT;
import static software.wings.service.impl.WebHookServiceImpl.X_GIT_HUB_EVENT;
import static software.wings.service.impl.WebHookServiceImpl.X_GIT_LAB_EVENT;
import static software.wings.utils.Validator.notNullCheck;
import static software.wings.yaml.gitSync.YamlGitConfig.BRANCH_NAME_KEY;
import static software.wings.yaml.gitSync.YamlGitConfig.GIT_CONNECTOR_ID_KEY;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SortOrder.OrderType;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.EntityType;
import software.wings.beans.GitCommit;
import software.wings.beans.GitConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.alert.AlertData;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.GitConnectionErrorAlert;
import software.wings.beans.alert.GitSyncErrorAlert;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitCommand.GitCommandType;
import software.wings.beans.yaml.GitCommitRequest;
import software.wings.beans.yaml.GitDiffRequest;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.GitFileChange.Builder;
import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.dl.WingsPersistence;
import software.wings.exception.YamlProcessingException;
import software.wings.exception.YamlProcessingException.ChangeWithErrorMsg;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.sync.YamlService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.CryptoUtil;
import software.wings.utils.JsonUtils;
import software.wings.utils.Util;
import software.wings.waitnotify.WaitNotifyEngine;
import software.wings.yaml.directory.DirectoryPath;
import software.wings.yaml.directory.FolderNode;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.gitSync.GitSyncWebhook;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlChangeSet.Status;
import software.wings.yaml.gitSync.YamlGitConfig;
import software.wings.yaml.gitSync.YamlGitConfig.SyncMode;

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
import javax.ws.rs.core.HttpHeaders;

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
  @Inject private AppService appService;
  @Inject private YamlGitService yamlGitSyncService;
  @Inject YamlHelper yamlHelper;

  /**
   * Gets the yaml git sync info by entityId
   *
   * @return the rest response
   */
  @Override
  public YamlGitConfig get(String accountId, String entityId, EntityType entityType) {
    return wingsPersistence.createQuery(YamlGitConfig.class)
        .filter(ACCOUNT_ID_KEY, accountId)
        .filter(YamlGitConfig.ENTITY_ID_KEY, entityId)
        .filter(YamlGitConfig.ENTITY_TYPE_KEY, entityType)
        .get();
  }

  @Override
  public YamlGitConfig save(YamlGitConfig ygs) {
    return save(ygs, true);
  }

  @Override
  public YamlGitConfig save(YamlGitConfig ygs, boolean performFullSync) {
    notNullCheck("application id cannot be empty", ygs.getAppId());

    ygs.setSyncMode(SyncMode.BOTH);
    YamlGitConfig yamlGitSync = wingsPersistence.saveAndGet(YamlGitConfig.class, ygs);
    if (performFullSync) {
      executorService.submit(() -> fullSync(ygs.getAccountId(), ygs.getEntityId(), ygs.getEntityType(), true));
    }

    return yamlGitSync;
  }

  @Override
  public GitConfig getGitConfig(YamlGitConfig ygs) {
    GitConfig gitConfig = null;
    if (EmptyPredicate.isNotEmpty(ygs.getGitConnectorId())) {
      SettingAttribute settingAttributeForGitConnector = settingsService.get(ygs.getGitConnectorId());
      gitConfig = (GitConfig) settingAttributeForGitConnector.getValue();
      if (gitConfig != null) {
        gitConfig.setBranch(ygs.getBranchName());
        if (EmptyPredicate.isNotEmpty(gitConfig.getSshSettingId())) {
          SettingAttribute settingAttributeForSshKey = getAndDecryptSettingAttribute(gitConfig.getSshSettingId());
          gitConfig.setSshSettingAttribute(settingAttributeForSshKey);
        }
      }
    } else {
      // This is to support backward compatibility. Should be removed once we move to using gitConnector completely
      if (EmptyPredicate.isNotEmpty(ygs.getSshSettingId())) {
        SettingAttribute settingAttributeForSshKey = getAndDecryptSettingAttribute(ygs.getSshSettingId());
        gitConfig = ygs.getGitConfig(settingAttributeForSshKey);
      } else {
        gitConfig = ygs.getGitConfig(null);
      }
    }

    return gitConfig;
  }

  public SettingAttribute getAndDecryptSettingAttribute(String sshSettingId) {
    SettingAttribute settingAttributeForSshKey = settingsService.get(sshSettingId);
    if (settingAttributeForSshKey != null) {
      HostConnectionAttributes attributeValue = (HostConnectionAttributes) settingAttributeForSshKey.getValue();
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails(attributeValue, GLOBAL_APP_ID, null);
      managerDecryptionService.decrypt(attributeValue, encryptionDetails);
      return settingAttributeForSshKey;
    }

    logger.warn(GIT_YAML_LOG_PREFIX + "Could not find setting attribute");
    return null;
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

  @Override
  public void fullSync(String accountId, String entityId, EntityType entityType, boolean forcePush) {
    logger.info(
        format(GIT_YAML_LOG_PREFIX + "Performing git full-sync for account %s and entity %s", accountId, entityId));

    String appId = accountId.equals(entityId) ? GLOBAL_APP_ID : entityId;
    YamlGitConfig yamlGitConfig = yamlDirectoryService.weNeedToPushChanges(accountId, appId);

    if (yamlGitConfig != null) {
      try {
        List<GitFileChange> gitFileChanges = new ArrayList<>();
        List<GitFileChange> deletedGitFileChanges = new ArrayList<>();

        if (EntityType.ACCOUNT.equals(entityType)) {
          // Handle everything except for application
          gitFileChanges = obtainAccountOnlyGitFileChanges(accountId, true);
          deletedGitFileChanges = obtainAccountOnlyGitFileChangeForDelete(accountId);

        } else if (EntityType.APPLICATION.equals(entityType)) {
          // Fetch application changeSets. The reason for special handling is that with application level yamlGitConfig,
          // each app can refer to different yamlGitConfig
          Application app = appService.get(appId);
          if (app != null) {
            gitFileChanges = obtainApplicationYamlGitFileChanges(accountId, app);
            deletedGitFileChanges = asList(generateGitFileChangeForApplicationDelete(accountId, app.getName()));
          }
        }

        if (gitFileChanges.size() > 0 && forcePush) {
          for (GitFileChange gitFileChange : deletedGitFileChanges) {
            gitFileChanges.add(0, gitFileChange);
          }
        }
        YamlChangeSet yamlChangeSet = obtainYamlChangeSet(accountId, appId, gitFileChanges, forcePush);

        discardGitSyncErrorForFullSync(accountId, appId);

        yamlChangeSetService.save(yamlChangeSet);
        logger.info(format(GIT_YAML_LOG_PREFIX + "Performed git full-sync for account %s and entity %s successfully",
            accountId, entityId));
      } catch (Exception ex) {
        logger.error(format(GIT_YAML_LOG_PREFIX + "Failed to perform git full-sync for account %s and entity %s",
                         yamlGitConfig.getAccountId(), entityId),
            ex);
      }
    }
  }

  @Deprecated
  private void syncFiles(String accountId, String entityId, YamlChangeSet yamlChangeSet) {
    try {
      // Get all Queued. Failed tasks to be processed before this full sync one.
      // find most recent change set with Completed status and get all changesets after that in Queued/Failed state
      List<YamlChangeSet> yamlChangeSetsToBeMarkedSkipped =
          yamlChangeSetService.getChangeSetsToBeMarkedSkipped(accountId);
      List<String> yamlChangesetIdsToBeSkipped = new ArrayList<>();
      yamlChangesetIdsToBeSkipped.addAll(
          yamlChangeSetsToBeMarkedSkipped.stream().map(changeSet -> changeSet.getUuid()).collect(toList()));

      // mark these change sets as Skipped
      yamlChangeSetService.updateStatusForGivenYamlChangeSets(
          accountId, Status.SKIPPED, asList(Status.QUEUED), yamlChangesetIdsToBeSkipped);
    } catch (Exception ex) {
      logger.error(
          format(GIT_YAML_LOG_PREFIX + "Failed to sync files for account %s and entity %s", accountId, entityId), ex);
    }
  }

  private List<GitFileChange> obtainAccountOnlyGitFileChangeForDelete(String accountId) {
    List<GitFileChange> gitFileChanges = new ArrayList<>();

    gitFileChanges.add(generateGitFileChangeForDelete(accountId, CLOUD_PROVIDERS_FOLDER));
    gitFileChanges.add(generateGitFileChangeForDelete(accountId, ARTIFACT_SOURCES_FOLDER));
    gitFileChanges.add(generateGitFileChangeForDelete(accountId, COLLABORATION_PROVIDERS_FOLDER));
    gitFileChanges.add(generateGitFileChangeForDelete(accountId, LOAD_BALANCERS_FOLDER));
    gitFileChanges.add(generateGitFileChangeForDelete(accountId, VERIFICATION_PROVIDERS_FOLDER));
    gitFileChanges.add(generateGitFileChangeForDelete(accountId, NOTIFICATION_GROUPS_FOLDER));
    gitFileChanges.add(generateGitFileChangeForDelete(accountId, DEFAULTS_YAML));

    return gitFileChanges;
  }

  private GitFileChange generateGitFileChangeForDelete(String accountId, String entity) {
    return Builder.aGitFileChange()
        .withAccountId(accountId)
        .withChangeType(ChangeType.DELETE)
        .withFilePath(SETUP_FOLDER + "/" + entity)
        .build();
  }

  private GitFileChange generateGitFileChangeForApplicationDelete(String accountId, String appName) {
    return Builder.aGitFileChange()
        .withAccountId(accountId)
        .withChangeType(ChangeType.DELETE)
        .withFilePath(SETUP_FOLDER + "/" + APPLICATIONS_FOLDER + "/" + appName)
        .build();
  }

  private YamlChangeSet obtainYamlChangeSet(
      String accountId, String appId, List<GitFileChange> gitFileChangeList, boolean forcePush) {
    return YamlChangeSet.builder()
        .accountId(accountId)
        .status(Status.QUEUED)
        .queuedOn(System.currentTimeMillis())
        .forcePush(forcePush)
        .gitFileChanges(gitFileChangeList)
        .appId(appId)
        .fullSync(true)
        .build();
  }

  private List<GitFileChange> obtainApplicationYamlGitFileChanges(String accountId, Application app) {
    DirectoryPath directoryPath = new DirectoryPath(SETUP_FOLDER);

    FolderNode applicationsFolder = new FolderNode(
        accountId, APPLICATIONS_FOLDER, Application.class, directoryPath.add(APPLICATIONS_FOLDER), yamlGitSyncService);

    yamlDirectoryService.doApplication(app.getUuid(), false, null, applicationsFolder, directoryPath);

    List<GitFileChange> gitFileChanges = new ArrayList<>();
    gitFileChanges = yamlDirectoryService.traverseDirectory(
        gitFileChanges, accountId, applicationsFolder, SETUP_FOLDER, true, false, Optional.empty());

    return gitFileChanges;
  }

  private List<YamlChangeSet> obtainAllApplicationYamlChangeSet(String accountId, boolean forcePush) {
    List<YamlChangeSet> yamlChangeSets = new ArrayList<>();
    List<Application> apps = appService.getAppsByAccountId(accountId);

    if (isEmpty(apps)) {
      return yamlChangeSets;
    }

    for (Application app : apps) {
      List<GitFileChange> gitFileChanges = obtainApplicationYamlGitFileChanges(accountId, app);
      yamlChangeSets.add(obtainYamlChangeSet(accountId, app.getUuid(), gitFileChanges, forcePush));
    }

    return yamlChangeSets;
  }

  @Override
  public List<GitFileChange> performFullSyncDryRun(String accountId) {
    List<GitFileChange> gitFileChanges = new ArrayList<>();

    List<YamlChangeSet> yamlChangeSets = obtainChangeSetFromFullSyncDryRun(accountId);
    for (YamlChangeSet yamlChangeSet : yamlChangeSets) {
      gitFileChanges.addAll(yamlChangeSet.getGitFileChanges());
    }

    return gitFileChanges;
  }

  @Override
  public List<YamlChangeSet> obtainChangeSetFromFullSyncDryRun(String accountId) {
    try {
      logger.info("Performing full-sync dry-run for account {}", accountId);
      List<YamlChangeSet> yamlChangeSets = new ArrayList<>();

      List<GitFileChange> gitFileChanges = obtainAccountOnlyGitFileChanges(accountId, false);
      yamlChangeSets.add(obtainYamlChangeSet(accountId, GLOBAL_APP_ID, gitFileChanges, false));

      yamlChangeSets.addAll(obtainAllApplicationYamlChangeSet(accountId, false));

      logger.info("Performed full-sync dry-run for account {}", accountId);
      return yamlChangeSets;
    } catch (Exception ex) {
      logger.error(format("Failed to perform full-sync dry-run for account %s", accountId), ex);
    }

    return new ArrayList<>();
  }

  private List<GitFileChange> obtainAccountOnlyGitFileChanges(String accountId, boolean includeFiles) {
    List<GitFileChange> gitFileChanges = new ArrayList<>();

    FolderNode top = yamlDirectoryService.getDirectory(accountId, SETUP_ENTITY_ID, false, null);
    gitFileChanges = yamlDirectoryService.traverseDirectory(
        gitFileChanges, accountId, top, "", includeFiles, true, Optional.empty());

    return gitFileChanges;
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
      logger.error(format("Failed to get all Yaml errors for account %s", accountId), ex);
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
    // With GIT_BATCH_SYNC flag disabled, the assumption is that yamlChangeSets list should have only one changeset
    String appId = yamlChangeSets.get(0).getAppId();
    YamlGitConfig yamlGitConfig = yamlDirectoryService.weNeedToPushChanges(accountId, appId);

    if (yamlGitConfig == null) {
      logger.warn(
          format(GIT_YAML_LOG_PREFIX + "YamlGitConfig is null for accountId %s and entity %s", accountId, appId));

      // We reach here if the changeSet doesn't have a valid yaml git configuration. This can happen in case of rename
      // where yamlchangeSets are generated for all apps and some apps may not have valid yamlGitConfig. Going forward
      // we wont be even generating those changeSets.
      // GIT_BATCH_SYNC flag disabled, the assumption is that yamlChangeSets list should have only one changeset
      String yamlChangeSetId = yamlChangeSets.get(0).getUuid();
      yamlChangeSetService.updateStatus(accountId, yamlChangeSetId, Status.SKIPPED);
      return true;
    }

    List<String> yamlChangeSetIds =
        yamlChangeSets.stream().map(yamlChangeSet -> yamlChangeSet.getUuid()).collect(toList());
    String mostRecentYamlChangesetId = yamlChangeSets.get(yamlChangeSets.size() - 1).getUuid();

    List<GitFileChange> gitFileChanges = (yamlChangeSets.size() > 1) ? getGitFileChangesToBeApplied(yamlChangeSets)
                                                                     : yamlChangeSets.get(0).getGitFileChanges();
    checkForValidNameSyntax(gitFileChanges);

    // @TODO_GITLOG add accountId here
    logger.info(format(GIT_YAML_LOG_PREFIX + "Creating COMMIT_AND_PUSH git delegate task for account %s and entity %s",
        accountId, appId));

    StringBuilder builder = new StringBuilder();
    yamlChangeSets.forEach(yamlChangeSet -> builder.append(yamlChangeSet.getUuid()).append("  "));
    logger.info(GIT_YAML_LOG_PREFIX + "Change sets [{}] files", builder.toString());

    String waitId = generateUuid();
    GitConfig gitConfig = getGitConfig(yamlGitConfig);

    if (yamlChangeSets.size() > 1) {
      logger.info(new StringBuilder(GIT_YAML_LOG_PREFIX)
                      .append("Processing YamlChangeSets for account: ")
                      .append(accountId)
                      .append(" and entity: ")
                      .append(appId)
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
                                            .yamlGitConfig(yamlGitConfig)
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
   *
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
  public void processWebhookPost(
      String accountId, String webhookToken, String yamlWebHookPayload, HttpHeaders headers) {
    try {
      List<SettingAttribute> settingAttributes =
          wingsPersistence.createQuery(SettingAttribute.class)
              .filter(ACCOUNT_ID_KEY, accountId)
              .filter(SettingAttribute.VALUE_TYPE_KEY, SettingVariableTypes.GIT.name())
              .asList();

      if (isEmpty(settingAttributes)) {
        logger.error(GIT_YAML_LOG_PREFIX + "Invalid git webhook request [{}]", webhookToken);
        return;
      }

      String gitConnectorId = null;
      for (SettingAttribute settingAttribute : settingAttributes) {
        SettingValue settingValue = settingAttribute.getValue();

        if (settingValue instanceof GitConfig && webhookToken.equals(((GitConfig) settingValue).getWebhookToken())) {
          gitConnectorId = settingAttribute.getUuid();
          break;
        }
      }

      if (isEmpty(gitConnectorId)) {
        logger.error(GIT_YAML_LOG_PREFIX + "Could not obtain valid git connector for webhook token " + webhookToken);
        return;
      }

      String branchName = obtainBranchFromPayload(yamlWebHookPayload, headers);
      if (isEmpty(branchName)) {
        logger.error(GIT_YAML_LOG_PREFIX
                + "Could not obtain valid branch from yaml webhook payload for git webhook request [{}]",
            webhookToken);
        return;
      }

      List<YamlGitConfig> yamlGitConfigs = wingsPersistence.createQuery(YamlGitConfig.class)
                                               .filter(ACCOUNT_ID_KEY, accountId)
                                               .filter(GIT_CONNECTOR_ID_KEY, gitConnectorId)
                                               .filter(BRANCH_NAME_KEY, branchName)
                                               .asList();
      if (isEmpty(yamlGitConfigs)) {
        logger.error(
            GIT_YAML_LOG_PREFIX + "Could not obtain valid git configuration for webhook token " + webhookToken);
        return;
      }

      YamlGitConfig yamlGitConfig = yamlGitConfigs.get(0);
      GitCommit gitCommit = fetchLastProcessedGitCommitId(accountId, yamlGitConfig.getUuid());

      String processedCommit = gitCommit == null ? null : gitCommit.getCommitId();

      String waitId = generateUuid();
      GitConfig gitConfig = getGitConfig(yamlGitConfig);
      DelegateTask delegateTask =
          aDelegateTask()
              .withTaskType(TaskType.GIT_COMMAND)
              .withAccountId(accountId)
              .withAppId(GLOBAL_APP_ID)
              .withWaitId(waitId)
              .withParameters(new Object[] {GitCommandType.DIFF, gitConfig,
                  secretManager.getEncryptionDetails(gitConfig, GLOBAL_APP_ID, null),
                  GitDiffRequest.builder().lastProcessedCommitId(processedCommit).yamlGitConfig(yamlGitConfig).build()})
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
    String appId = obtainAppIdFromGitFileChange(failedChange.getAccountId(), failedChange.getFilePath());

    UpdateOperations<GitSyncError> failedUpdateOperations =
        wingsPersistence.createUpdateOperations(GitSyncError.class)
            .set("accountId", failedChange.getAccountId())
            .set("yamlFilePath", failedChange.getFilePath())
            .set("yamlContent", failedChange.getFileContent())
            .set("gitCommitId", failedCommitId)
            .set("changeType", failedChange.getChangeType().name())
            .set("failureReason",
                errorMessage != null ? errorMessage : "Reason could not be captured. Logs might have some info")
            .set("fullSyncPath", fullSyncPath)
            .set(APP_ID_KEY, appId);

    wingsPersistence.upsert(failedQuery, failedUpdateOperations);
  }

  private String obtainAppIdFromGitFileChange(String accountId, String yamlFilePath) {
    String appId = GLOBAL_APP_ID;

    // Fetch appName from yamlPath, e.g. Setup/Applications/App1/Services/S1/index.yaml -> App1,
    // Setup/Artifact Servers/server.yaml -> null
    String appName = yamlHelper.getAppName(yamlFilePath);
    if (StringUtils.isNotBlank(appName)) {
      Application app = appService.getAppByName(accountId, appName);
      if (app != null) {
        appId = app.getUuid();
      }
    }

    return appId;
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
  public RestResponse discardGitSyncErrorForFullSync(String accountId, String appId) {
    Query query = wingsPersistence.createAuthorizedQuery(GitSyncError.class);
    query.filter("accountId", accountId);
    query.filter("fullSyncPath", true);
    query.filter(APP_ID_KEY, appId);
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

  @Override
  public boolean checkApplicationChange(GitFileChange gitFileChange) {
    return StringUtils.startsWith(gitFileChange.getFilePath(), APPLICATION_FOLDER_PATH);
  }

  @Override
  public String obtainAppNameFromGitFileChange(GitFileChange gitFileChange) {
    String filePath = gitFileChange.getFilePath();

    String appFolderPath = APPLICATION_FOLDER_PATH + PATH_DELIMITER;
    String appPath = filePath.substring(appFolderPath.length());
    return appPath.substring(0, appPath.indexOf('/'));
  }

  @Override
  public void delete(String accountId, String entityId, EntityType entityType) {
    YamlGitConfig yamlGitConfig = get(accountId, entityId, entityType);
    if (yamlGitConfig == null) {
      return;
    }

    wingsPersistence.delete(yamlGitConfig);
  }

  private String obtainBranchFromPayload(String yamlWebHookPayload, HttpHeaders headers) {
    try {
      if (headers == null) {
        return null;
      }

      Map<String, Object> map = JsonUtils.asObject(yamlWebHookPayload, new TypeReference<Map<String, Object>>() {});

      if ((headers.getHeaderString(X_GIT_HUB_EVENT) != null) || (headers.getHeaderString(X_GIT_LAB_EVENT) != null)) {
        String ref = (String) map.get("ref");
        int lastIndex = ref.lastIndexOf('/');
        if (lastIndex != -1) {
          return ref.substring(lastIndex + 1);
        }
      } else if (headers.getHeaderString(X_BIT_BUCKET_EVENT) != null) {
        ArrayList arrayList = (ArrayList) ((LinkedHashMap) map.get("push")).get("changes");
        return (String) ((LinkedHashMap) (((LinkedHashMap) (arrayList.get(0))).get("new"))).get("name");
      } else {
        logger.error(format(GIT_YAML_LOG_PREFIX + "Unsupported webhook token header %s", headers));
      }

      return "";
    } catch (Exception e) {
      logger.info(format(GIT_YAML_LOG_PREFIX + "Failed to obtain branch from payload %s ", yamlWebHookPayload) + e);
      return null;
    }
  }

  @Override
  public void fullSyncForEntireAccount(String accountId) {
    logger.info(format(GIT_YAML_LOG_PREFIX + "Performing fullsync from migration service for account %s", accountId));

    // Perform fullsync for account level entities
    fullSync(accountId, accountId, EntityType.ACCOUNT, false);

    try (HIterator<Application> apps = new HIterator<>(
             wingsPersistence.createQuery(Application.class).filter(ACCOUNT_ID_KEY, accountId).fetch())) {
      while (apps.hasNext()) {
        Application application = apps.next();
        fullSync(accountId, application.getUuid(), EntityType.APPLICATION, false);
      }
    }
    logger.info(format(GIT_YAML_LOG_PREFIX + "Performed fullsync from migration service for account %s", accountId));
  }

  private GitCommit fetchLastProcessedGitCommitId(String accountId, String yamlGitConfigId) {
    // After MultiGit support gitCommit record would have list of yamlGitConfigs.

    GitCommit gitCommit = wingsPersistence.createQuery(GitCommit.class)
                              .filter(ACCOUNT_ID_KEY, accountId)
                              .filter(STATUS_KEY, Status.COMPLETED)
                              .field(YAML_GIT_CONFIG_IDS_KEY)
                              .contains(yamlGitConfigId)
                              .order("-lastUpdatedAt")
                              .get();

    // This is to handle the old git commit records which doesn't have yamlGitConfigId
    if (gitCommit == null) {
      gitCommit = wingsPersistence.createQuery(GitCommit.class)
                      .filter(ACCOUNT_ID_KEY, accountId)
                      .filter(YAML_GIT_CONFIG_ID_KEY, yamlGitConfigId)
                      .filter(STATUS_KEY, Status.COMPLETED)
                      .order("-lastUpdatedAt")
                      .get();
    }

    return gitCommit;
  }
}
