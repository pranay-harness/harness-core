package io.harness.gitsync.core.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.git.GitCommandType.COMMIT_AND_PUSH;
import static io.harness.delegate.beans.git.GitCommandType.DIFF;
import static io.harness.exception.WingsException.USER;
import static io.harness.gitsync.common.YamlProcessingLogContext.BRANCH_NAME;
import static io.harness.gitsync.common.YamlProcessingLogContext.CHANGESET_ID;
import static io.harness.gitsync.common.YamlProcessingLogContext.GIT_CONNECTOR_ID;
import static io.harness.gitsync.common.YamlProcessingLogContext.REPO_NAME;
import static io.harness.gitsync.common.YamlProcessingLogContext.WEBHOOK_TOKEN;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.waiter.NgOrchestrationNotifyEventListener.NG_ORCHESTRATION;

import static software.wings.beans.yaml.GitCommandRequest.gitRequestTimeout;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;

import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.data.structure.NullSafeImmutableMap;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.gitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.git.GitCommandParams;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.CommitAndPushRequest;
import io.harness.git.model.DiffRequest;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.common.YamlProcessingLogContext;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.beans.ChangeWithErrorMsg;
import io.harness.gitsync.core.beans.GitCommit;
import io.harness.gitsync.core.beans.GitWebhookRequestAttributes;
import io.harness.gitsync.core.callback.GitCommandCallback;
import io.harness.gitsync.core.service.GitCommitService;
import io.harness.gitsync.core.service.YamlChangeSetService;
import io.harness.gitsync.core.service.YamlGitService;
import io.harness.gitsync.gitsyncerror.service.GitSyncErrorService;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.mongo.ProcessTimeLogContext;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.JsonUtils;
import io.harness.waiter.WaitNotifyEngine;

import software.wings.beans.TaskType;
import software.wings.beans.trigger.WebhookSource;
import software.wings.service.impl.trigger.WebhookEventUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.core.HttpHeaders;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ValidateOnExecution
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class YamlGitServiceImpl implements YamlGitService {
  public static final String RESPONSE_FOR_PING_EVENT = "Found ping event. Only push events are supported";
  public static final String WEBHOOK_SUCCESS_MSG = "Successfully accepted webhook request for processing";
  private WaitNotifyEngine waitNotifyEngine;
  private SecretManagerClientService ngSecretService;
  private YamlGitConfigService yamlGitConfigService;
  private GitSyncErrorService gitSyncErrorService;
  private GitCommitService gitCommitService;
  private WebhookEventUtils webhookEventUtils;
  private YamlChangeSetService yamlChangeSetService;

  private final String GIT_YAML_LOG_PREFIX = "GIT_YAML_LOG_PREFIX";

  @Override
  public YamlGitConfigDTO weNeedToPushChanges(
      String accountId, String orgIdentifier, String projectIdentifier, String rootFolderId) {
    return yamlGitConfigService.getByFolderIdentifierAndIsEnabled(
        projectIdentifier, orgIdentifier, accountId, rootFolderId);
  }

  @Override
  public YamlGitConfigDTO getYamlGitConfigForHarnessToGitChangeSet(
      GitFileChange harnessToGitFileChange, YamlChangeSet harnessToGitChangeSet) {
    String rootFolderId = harnessToGitFileChange.getRootPathId();
    return weNeedToPushChanges(harnessToGitChangeSet.getAccountId(), harnessToGitChangeSet.getOrganizationId(),
        harnessToGitChangeSet.getProjectId(), rootFolderId);
  }

  @Override
  public void handleHarnessChangeSet(YamlChangeSet yamlChangeSet, String accountId) {
    log.info(GIT_YAML_LOG_PREFIX + "Started handling harness -> git changeset");

    List<GitFileChange> gitFileChanges = yamlChangeSet.getGitFileChanges();

    // TODO(abhinav): Optimise so that single delegate task is created for git file changes going to same location.
    for (GitFileChange gitFileChange : gitFileChanges) {
      try {
        createDelegateTaskForCommitAndPush(yamlChangeSet, accountId, gitFileChange);
      } catch (Exception e) {
        log.info("git Sync failed for filepath [{}] with exception [{}]", gitFileChange.getFilePath(), e);
      }
    }
  }

  private void createDelegateTaskForCommitAndPush(
      YamlChangeSet yamlChangeSet, String accountId, GitFileChange gitFileChange) {
    YamlGitConfigDTO yamlGitConfig = getYamlGitConfigForHarnessToGitChangeSet(gitFileChange, yamlChangeSet);
    if (yamlGitConfig == null) {
      throw new InvalidRequestException(String.format("No git sync configured for [%s]", gitFileChange.getFilePath()));
    }
    Optional<ConnectorInfoDTO> connector = getGitConnector(yamlGitConfig);

    if (!connector.isPresent()) {
      throw new GeneralException(
          format(GIT_YAML_LOG_PREFIX + "GitConfig shouldn't be null for accountId [%s]", yamlGitConfig, accountId));
    }

    log.info(GIT_YAML_LOG_PREFIX + "Creating COMMIT_AND_PUSH git delegate task for entity");
    TaskData taskData = getTaskDataForCommitAndPush(yamlChangeSet, gitFileChange, yamlGitConfig, connector.get(),
        accountId, yamlChangeSet.getOrganizationId(), yamlChangeSet.getProjectId());
    Map<String, String> setupAbstractions = ImmutableMap.of("accountId", accountId);
    // TODO (abhinav) : Adopt this to use delegate 2.0 . Meanwhile replacing taskId with some dummy string
    // String taskId = managerDelegateServiceDriver.sendTaskAsync(accountId, setupAbstractions, taskData);
    String taskId = generateUuid();

    waitNotifyEngine.waitForAllOn(NG_ORCHESTRATION,
        new GitCommandCallback(accountId, yamlChangeSet.getUuid(), COMMIT_AND_PUSH, yamlGitConfig.getGitConnectorId(),
            yamlGitConfig.getRepo(), yamlGitConfig.getBranch(), yamlGitConfig),
        taskId);

    log.info(
        GIT_YAML_LOG_PREFIX + "Successfully queued harness->git changeset for processing with delegate taskId=[{}]",
        taskId);
  }

  private TaskData getTaskDataForCommitAndPush(YamlChangeSet yamlChangeSet, GitFileChange gitFileChange,
      YamlGitConfigDTO yamlGitConfig, ConnectorInfoDTO connector, String accountIdentifier, String orgIdentifier,
      String projectIdentifier) {
    GitConfigDTO gitConfig = (GitConfigDTO) connector.getConnectorConfig();
    GitAuthenticationDTO gitAuthenticationDecryptableEntity = gitConfig.getGitAuth();
    NGAccess basicNGAccessObject = BaseNGAccess.builder()
                                       .accountIdentifier(accountIdentifier)
                                       .orgIdentifier(orgIdentifier)
                                       .projectIdentifier(projectIdentifier)
                                       .build();
    List<EncryptedDataDetail> encryptedDataDetailList =
        ngSecretService.getEncryptionDetails(basicNGAccessObject, gitAuthenticationDecryptableEntity);
    GitCommandParams gitCommandParams = buildGitCommandParamsForCommitAndPush(
        gitConfig, gitFileChange, encryptedDataDetailList, connector.getIdentifier());
    return TaskData.builder()
        .taskType(TaskType.NG_GIT_COMMAND.name())
        .async(true)
        .timeout(gitRequestTimeout)
        .parameters(new Object[] {gitCommandParams})
        .build();
  }

  private GitCommandParams buildGitCommandParamsForCommitAndPush(GitConfigDTO gitConfig,
      io.harness.git.model.GitFileChange gitFileChangeDelegate, List<EncryptedDataDetail> encryptedDataDetailList,
      String connectorId) {
    // todo(abhinav): refactor push Only If head seen
    return GitCommandParams.builder()
        .encryptionDetails(encryptedDataDetailList)
        .gitCommandType(COMMIT_AND_PUSH)
        .gitCommandRequest(CommitAndPushRequest.builder()
                               .gitFileChanges(Collections.singletonList(gitFileChangeDelegate))
                               .forcePush(false)
                               .connectorId(connectorId)
                               .pushOnlyIfHeadSeen(false)
                               .build())
        .gitConfig(gitConfig)
        .build();
  }

  private Optional<ConnectorInfoDTO> getGitConnector(YamlGitConfigDTO yamlGitConfig) {
    if (yamlGitConfig != null) {
      String branchName = yamlGitConfig.getBranch();
      String gitConnectorIdentifier = yamlGitConfig.getGitConnectorId();
      String repoName = yamlGitConfig.getRepo();
      return yamlGitConfigService.getGitConnector(yamlGitConfig, gitConnectorIdentifier, repoName, branchName);
    }
    return Optional.empty();
  }

  @Override
  public void removeGitSyncErrors(
      String accountId, String orgId, String projectId, List<GitFileChange> gitFileChangeList, boolean gitToHarness) {
    List<String> yamlFilePathList = gitFileChangeList.stream().map(GitFileChange::getFilePath).collect(toList());
    gitSyncErrorService.deleteByAccountIdOrgIdProjectIdAndFilePath(accountId, orgId, projectId, yamlFilePathList);
  }

  @VisibleForTesting
  List<ConnectorInfoDTO> getGitConnectors(String accountId) {
    // TODO(abhinav): Refactor after connector impl
    return null;
  }

  @VisibleForTesting
  String getGitConnectorIdByWebhookToken(List<ConnectorInfoDTO> connectors, String webhookToken) {
    String gitConnectorId = null;
    for (ConnectorInfoDTO connector : connectors) {
      final ConnectorType type = connector.getConnectorType();
      // TODO(abhinav): Change name to webhook token
      if (type.equals(ConnectorType.GIT) && webhookToken.equals(connector.getName())) {
        return connector.getIdentifier();
      }
    }
    return gitConnectorId;
  }

  @Override
  public String validateAndQueueWebhookRequest(
      String accountId, String webhookToken, String yamlWebHookPayload, HttpHeaders headers) {
    final Stopwatch startedStopWatch = Stopwatch.createStarted();

    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         YamlProcessingLogContext ignore2 =
             YamlProcessingLogContext.builder().webhookToken(webhookToken).build(OVERRIDE_ERROR)) {
      log.info(GIT_YAML_LOG_PREFIX + "Started processing webhook request");

      List<ConnectorInfoDTO> connectors = getGitConnectors(accountId);

      if (isEmpty(connectors)) {
        log.info(GIT_YAML_LOG_PREFIX + "Git connector not found for account");
        throw new InvalidRequestException("Git connector not found with webhook token " + webhookToken, USER);
      }

      String gitConnectorId = getGitConnectorIdByWebhookToken(connectors, webhookToken);

      if (isEmpty(gitConnectorId)) {
        throw new InvalidRequestException("Git connector not found with webhook token " + webhookToken, USER);
      }

      boolean gitPingEvent = webhookEventUtils.isGitPingEvent(headers);
      if (gitPingEvent) {
        log.info(GIT_YAML_LOG_PREFIX + "Ping event found. Skip processing");
        return RESPONSE_FOR_PING_EVENT;
      }

      final Optional<String> repoName = obtainRepoFromPayload(yamlWebHookPayload, headers);

      if (!repoName.isPresent()) {
        log.info(GIT_YAML_LOG_PREFIX + "Repo not found. webhookToken: {}, yamlWebHookPayload: {}, headers: {}",
            webhookToken, yamlWebHookPayload, headers);
        throw new InvalidRequestException("Repo not found from webhook payload", USER);
      }

      final String branchName = obtainBranchFromPayload(yamlWebHookPayload, headers);

      if (isEmpty(branchName)) {
        log.info(GIT_YAML_LOG_PREFIX + "Branch not found. webhookToken: {}, yamlWebHookPayload: {}, headers: {}",
            webhookToken, yamlWebHookPayload, headers);
        throw new InvalidRequestException("Branch not found from webhook payload", USER);
      }

      List<YamlGitConfigDTO> yamlGitConfigs =
          yamlGitConfigService.getByConnectorRepoAndBranch(gitConnectorId, repoName.get(), branchName, accountId);

      if (isEmpty(yamlGitConfigs)) {
        log.info(
            GIT_YAML_LOG_PREFIX + "No git sync configured for repo = [{}], branch =[{}]", repoName.get(), branchName);
        throw new InvalidRequestException("No git sync configured for the repo and branch.", USER);
      }

      String headCommitId = obtainCommitIdFromPayload(yamlWebHookPayload, headers);

      if (isNotEmpty(headCommitId)
          && gitCommitService.isCommitAlreadyProcessed(accountId, headCommitId, repoName.get(), branchName)) {
        log.info(GIT_YAML_LOG_PREFIX + "CommitId: [{}] already processed.", headCommitId);
        return "Commit already processed";
      }

      log.info(GIT_YAML_LOG_PREFIX + " Found branch name =[{}], headCommitId=[{}]", branchName, headCommitId);

      YamlChangeSet yamlChangeSet = buildYamlChangeSetForGitToHarness(
          accountId, yamlWebHookPayload, headers, gitConnectorId, repoName.get(), branchName, headCommitId);
      final YamlChangeSet savedYamlChangeSet = yamlChangeSetService.save(yamlChangeSet);

      try (ProcessTimeLogContext ignore3 =
               new ProcessTimeLogContext(startedStopWatch.elapsed(MILLISECONDS), OVERRIDE_ERROR)) {
        log.info(GIT_YAML_LOG_PREFIX + "Successfully accepted webhook request for processing as yamlChangeSetId=[{}]",
            savedYamlChangeSet.getUuid());
      }

      return WEBHOOK_SUCCESS_MSG;
    }
  }

  private YamlChangeSet buildYamlChangeSetForGitToHarness(String accountId, String yamlWebHookPayload,
      HttpHeaders headers, String gitConnectorId, String repoName, String branchName, String headCommitId) {
    return YamlChangeSet.builder()
        .accountId(accountId)
        .gitToHarness(true)
        .status(YamlChangeSet.Status.QUEUED)
        .gitWebhookRequestAttributes(GitWebhookRequestAttributes.builder()
                                         .webhookBody(yamlWebHookPayload)
                                         .gitConnectorId(gitConnectorId)
                                         .webhookHeaders(convertHeadersToJsonString(headers))
                                         .repo(repoName)
                                         .branchName(branchName)
                                         .headCommitId(headCommitId)
                                         .build())
        .gitFileChanges(new ArrayList<>())
        .retryCount(0)
        .build();
  }

  private String convertHeadersToJsonString(HttpHeaders headers) {
    try {
      return JsonUtils.asJson(headers.getRequestHeaders());
    } catch (Exception ex) {
      log.warn("Failed to convert request headers in json string", ex);
      return null;
    }
  }

  @Override
  public List<YamlGitConfigDTO> getYamlGitConfigsForGitToHarnessChangeSet(YamlChangeSet gitToHarnessChangeSet) {
    final String accountId = gitToHarnessChangeSet.getAccountId();
    checkNotNull(gitToHarnessChangeSet.getGitWebhookRequestAttributes(),
        "GitWebhookRequestAttributes not available in changeset = [%s]", gitToHarnessChangeSet.getUuid());

    final String gitConnectorId = gitToHarnessChangeSet.getGitWebhookRequestAttributes().getGitConnectorId();
    final String branchName = gitToHarnessChangeSet.getGitWebhookRequestAttributes().getBranchName();
    final String repo = gitToHarnessChangeSet.getGitWebhookRequestAttributes().getRepo();

    checkState(isNotEmpty(gitConnectorId), "gitConnectorId should not be empty");
    checkState(isNotEmpty(branchName), "branchName should not be empty");
    checkState(isNotEmpty(repo), "repo should not be empty");

    return yamlGitConfigService.getByConnectorRepoAndBranch(gitConnectorId, repo, branchName, accountId);
  }

  @Override
  public void handleGitChangeSet(YamlChangeSet yamlChangeSet, String accountId) {
    final Stopwatch stopwatch = Stopwatch.createStarted();
    GitWebhookRequestAttributes gitWebhookRequestAttributes = yamlChangeSet.getGitWebhookRequestAttributes();
    String gitConnectorId = gitWebhookRequestAttributes.getGitConnectorId();
    String branchName = gitWebhookRequestAttributes.getBranchName();
    String repo = gitWebhookRequestAttributes.getRepo();
    String headCommitId = gitWebhookRequestAttributes.getHeadCommitId();

    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         YamlProcessingLogContext ignore3 =
             getYamlProcessingLogContext(gitConnectorId, repo, branchName, null, yamlChangeSet.getUuid())) {
      log.info(
          GIT_YAML_LOG_PREFIX + "Started handling Git -> harness changeset with headCommit Id =[{}]", headCommitId);

      if (isNotEmpty(headCommitId)
          && gitCommitService.isCommitAlreadyProcessed(accountId, headCommitId, repo, branchName)) {
        log.info(GIT_YAML_LOG_PREFIX + "CommitId: [{}] already processed.", headCommitId);
        yamlChangeSetService.updateStatus(accountId, yamlChangeSet.getUuid(), YamlChangeSet.Status.SKIPPED);
        return;
      }

      final List<YamlGitConfigDTO> yamlGitConfigs = getYamlGitConfigsForGitToHarnessChangeSet(yamlChangeSet);

      if (yamlGitConfigs == null) {
        log.info(GIT_YAML_LOG_PREFIX + "Git sync configuration not found");
        throw new InvalidRequestException("Git sync configuration not found with repo and branch " + branchName, USER);
      }

      final Optional<GitCommit> lastProcessedGitCommitId = fetchLastProcessedGitCommitId(accountId, repo, branchName);

      final String processedCommit = lastProcessedGitCommitId.map(GitCommit::getCommitId).orElse(null);
      log.info(GIT_YAML_LOG_PREFIX + "Last processed git commit found =[{}]", processedCommit);

      String taskId =
          createDelegateTaskForDiff(yamlChangeSet, accountId, yamlGitConfigs, processedCommit, headCommitId);

      try (ProcessTimeLogContext ignore2 = new ProcessTimeLogContext(stopwatch.elapsed(MILLISECONDS), OVERRIDE_ERROR)) {
        log.info(
            GIT_YAML_LOG_PREFIX + "Successfully queued git->harness changeset for processing with delegate taskId=[{}]",
            taskId);
      }

    } catch (Exception ex) {
      log.error(format(GIT_YAML_LOG_PREFIX + "Unexpected error while processing git->harness changeset [%s]",
                    yamlChangeSet.getUuid()),
          ex);
      yamlChangeSetService.updateStatus(accountId, yamlChangeSet.getUuid(), YamlChangeSet.Status.SKIPPED);
    }
  }

  private String createDelegateTaskForDiff(YamlChangeSet yamlChangeSet, String accountId,
      List<YamlGitConfigDTO> yamlGitConfigs, String lastProcessedCommitId, String endCommitId) {
    YamlGitConfigDTO yamlGitConfig = yamlGitConfigs.get(0);
    final Optional<ConnectorInfoDTO> connector = getGitConnector(yamlGitConfig);

    if (!connector.isPresent()) {
      throw new GeneralException(
          format(GIT_YAML_LOG_PREFIX + "No git sync configured for accountId [{%s}], orgId [{%s}], projectId [{%s}]",
              yamlChangeSet.getAccountId(), yamlChangeSet.getOrganizationId(), yamlChangeSet.getProjectId()));
    }

    log.info(GIT_YAML_LOG_PREFIX + "Creating DIFF git delegate task for entity");
    TaskData taskData = getTaskDataForDiff(yamlChangeSet, yamlGitConfig, connector.get(), accountId,
        yamlChangeSet.getOrganizationId(), yamlChangeSet.getProjectId(), lastProcessedCommitId, endCommitId);
    Map<String, String> setupAbstractions = ImmutableMap.of("accountId", accountId);
    // TODO (abhinav) : Adopt this to use delegate 2.0 . Meanwhile replacing taskId with some dummy string
    // String taskId = managerDelegateServiceDriver.sendTaskAsync(accountId, setupAbstractions, taskData);
    String taskId = generateUuid();
    waitNotifyEngine.waitForAllOn(NG_ORCHESTRATION,
        new GitCommandCallback(accountId, yamlChangeSet.getUuid(), DIFF, yamlGitConfig.getGitConnectorId(),
            yamlGitConfig.getRepo(), yamlGitConfig.getBranch(), yamlGitConfig),
        taskId);

    log.info(
        GIT_YAML_LOG_PREFIX + "Successfully queued git->harness changeset for processing with delegate taskId=[{}]",
        taskId);
    return taskId;
  }

  private TaskData getTaskDataForDiff(YamlChangeSet yamlChangeSet, YamlGitConfigDTO yamlGitConfig,
      ConnectorInfoDTO connector, String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String lastCommitId, String currentCommit) {
    GitConfigDTO gitConfig = (GitConfigDTO) connector.getConnectorConfig();
    GitAuthenticationDTO gitAuthenticationDecryptableEntity = gitConfig.getGitAuth();
    NGAccess basicNGAccessObject = BaseNGAccess.builder()
                                       .accountIdentifier(accountIdentifier)
                                       .orgIdentifier(orgIdentifier)
                                       .projectIdentifier(projectIdentifier)
                                       .build();
    List<EncryptedDataDetail> encryptedDataDetailList =
        ngSecretService.getEncryptionDetails(basicNGAccessObject, gitAuthenticationDecryptableEntity);
    final DiffRequest diffRequest =
        DiffRequest.builder().lastProcessedCommitId(lastCommitId).endCommitId(currentCommit).build();
    GitCommandParams gitCommandParams = GitCommandParams.builder()
                                            .encryptionDetails(encryptedDataDetailList)
                                            .gitCommandType(DIFF)
                                            .gitCommandRequest(diffRequest)
                                            .gitConfig(gitConfig)
                                            .build();
    return TaskData.builder()
        .taskType(TaskType.NG_GIT_COMMAND.name())
        .async(true)
        .timeout(gitRequestTimeout)
        .parameters(new Object[] {gitCommandParams})
        .build();
  }

  private YamlProcessingLogContext getYamlProcessingLogContext(
      String gitConnectorId, String repoName, String branch, String webhookToken, String yamlChangeSetId) {
    return new YamlProcessingLogContext(NullSafeImmutableMap.<String, String>builder()
                                            .putIfNotNull(GIT_CONNECTOR_ID, gitConnectorId)
                                            .putIfNotNull(REPO_NAME, repoName)
                                            .putIfNotNull(BRANCH_NAME, branch)
                                            .putIfNotNull(WEBHOOK_TOKEN, webhookToken)
                                            .putIfNotNull(CHANGESET_ID, yamlChangeSetId)
                                            .build(),
        OVERRIDE_ERROR);
  }

  private String obtainCommitIdFromPayload(String yamlWebHookPayload, HttpHeaders headers) {
    if (headers == null) {
      log.info("Empty header found");
      return null;
    }

    WebhookSource webhookSource = webhookEventUtils.obtainWebhookSource(headers);
    webhookEventUtils.validatePushEvent(webhookSource, headers);

    Map<String, Object> payLoadMap =
        JsonUtils.asObject(yamlWebHookPayload, new TypeReference<Map<String, Object>>() {});

    return webhookEventUtils.obtainCommitId(webhookSource, headers, payLoadMap);
  }

  private Optional<String> obtainRepoFromPayload(String yamlWebHookPayload, HttpHeaders headers) {
    if (headers == null) {
      log.info("Empty header found");
      return Optional.empty();
    }

    WebhookSource webhookSource = webhookEventUtils.obtainWebhookSource(headers);
    webhookEventUtils.validatePushEvent(webhookSource, headers);

    Map<String, Object> payLoadMap;
    try {
      payLoadMap = JsonUtils.asObject(yamlWebHookPayload, new TypeReference<Map<String, Object>>() {});
    } catch (Exception ex) {
      log.info("Webhook payload: " + yamlWebHookPayload, ex);
      throw new InvalidRequestException(
          "Failed to parse the webhook payload. Error " + ExceptionUtils.getMessage(ex), USER);
    }

    return webhookEventUtils.obtainRepositoryName(webhookSource, headers, payLoadMap);
  }

  private String obtainBranchFromPayload(String yamlWebHookPayload, HttpHeaders headers) {
    if (headers == null) {
      log.info("Empty header found");
      return null;
    }

    WebhookSource webhookSource = webhookEventUtils.obtainWebhookSource(headers);
    webhookEventUtils.validatePushEvent(webhookSource, headers);

    Map<String, Object> payLoadMap;
    try {
      payLoadMap = JsonUtils.asObject(yamlWebHookPayload, new TypeReference<Map<String, Object>>() {});
    } catch (Exception ex) {
      log.info("Webhook payload: " + yamlWebHookPayload, ex);
      throw new InvalidRequestException(
          "Failed to parse the webhook payload. Error " + ExceptionUtils.getMessage(ex), USER);
    }

    return webhookEventUtils.obtainBranchName(webhookSource, headers, payLoadMap);
  }

  private Optional<GitCommit> fetchLastProcessedGitCommitId(String accountId, String repo, String branch) {
    return gitCommitService.findLastProcessedGitCommit(accountId, repo, branch);
  }

  @Override
  public void processFailedChanges(String accountId, List<ChangeWithErrorMsg> failedChangesWithErrorMsg,
      YamlGitConfigDTO yamlGitConfig, boolean gitToHarness, boolean fullSync) {
    if (failedChangesWithErrorMsg != null) {
      failedChangesWithErrorMsg.forEach(failedChangeWithErrorMsg
          -> gitSyncErrorService.upsertGitSyncErrors(failedChangeWithErrorMsg.getChange(),
              failedChangeWithErrorMsg.getErrorMsg(), fullSync, yamlGitConfig, gitToHarness));
    }
  }
}
