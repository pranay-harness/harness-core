package io.harness.gitsync.core.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.git.GitCommandType.DIFF;
import static io.harness.exception.WingsException.USER;
import static io.harness.gitsync.common.YamlProcessingLogContext.BRANCH_NAME;
import static io.harness.gitsync.common.YamlProcessingLogContext.CHANGESET_ID;
import static io.harness.gitsync.common.YamlProcessingLogContext.GIT_CONNECTOR_ID;
import static io.harness.gitsync.common.YamlProcessingLogContext.REPO_NAME;
import static io.harness.gitsync.common.YamlProcessingLogContext.WEBHOOK_TOKEN;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static software.wings.beans.yaml.GitCommandRequest.gitRequestTimeout;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.data.structure.NullSafeImmutableMap;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.git.GitCommandParams;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.DiffRequest;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.common.YamlProcessingLogContext;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.beans.ChangeWithErrorMsg;
import io.harness.gitsync.core.beans.GitCommit;
import io.harness.gitsync.core.beans.GitWebhookRequestAttributes;
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
import io.harness.utils.IdentifierRefHelper;
import io.harness.waiter.WaitNotifyEngine;

import software.wings.beans.TaskType;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ValidateOnExecution
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(DX)
public class YamlGitServiceImpl implements YamlGitService {
  private YamlGitConfigService yamlGitConfigService;
  private GitSyncErrorService gitSyncErrorService;
  private GitCommitService gitCommitService;
  private YamlChangeSetService yamlChangeSetService;
  private SecretManagerClientService ngSecretService;
  private WaitNotifyEngine waitNotifyEngine;

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

  private Optional<ConnectorInfoDTO> getGitConnector(YamlGitConfigDTO yamlGitConfig) {
    if (yamlGitConfig != null) {
      IdentifierRef identifierRef =
          IdentifierRefHelper.getIdentifierRef(yamlGitConfig.getGitConnectorRef(), yamlGitConfig.getAccountIdentifier(),
              yamlGitConfig.getOrganizationIdentifier(), yamlGitConfig.getProjectIdentifier());
      return yamlGitConfigService.getGitConnector(yamlGitConfig, identifierRef.getIdentifier());
    }
    return Optional.empty();
  }

  @Override
  public void removeGitSyncErrors(
      String accountId, String orgId, String projectId, List<GitFileChange> gitFileChangeList) {
    List<String> yamlFilePathList = gitFileChangeList.stream().map(GitFileChange::getFilePath).collect(toList());
    gitSyncErrorService.deleteByAccountIdOrgIdProjectIdAndFilePath(accountId, orgId, projectId, yamlFilePathList);
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

      // todo(abhinav): donot create delegate task when not reauired.
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
    //    waitNotifyEngine.waitForAllOn(NG_ORCHESTRATION,
    //        new GitCommandCallback(accountId, yamlChangeSet.getUuid(), DIFF, yamlGitConfig.getGitConnectorRef(),
    //            yamlGitConfig.getRepo(), yamlGitConfig.getBranch(), yamlGitConfig),
    //        taskId);

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

  private Optional<GitCommit> fetchLastProcessedGitCommitId(String accountId, String repo, String branch) {
    return gitCommitService.findLastProcessedGitCommit(accountId, repo, branch);
  }

  @Override
  public void processFailedChanges(String accountId, List<ChangeWithErrorMsg> failedChangesWithErrorMsg,
      YamlGitConfigDTO yamlGitConfig, boolean fullSync) {
    if (failedChangesWithErrorMsg != null) {
      failedChangesWithErrorMsg.forEach(failedChangeWithErrorMsg
          -> gitSyncErrorService.upsertGitSyncErrors(
              failedChangeWithErrorMsg.getChange(), failedChangeWithErrorMsg.getErrorMsg(), fullSync, yamlGitConfig));
    }
  }
}
