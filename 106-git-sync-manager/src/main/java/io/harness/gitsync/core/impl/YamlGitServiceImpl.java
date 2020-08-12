package io.harness.gitsync.core.impl;

import static io.harness.delegate.beans.git.GitCommand.GitCommandType.COMMIT_AND_PUSH;
import static io.harness.waiter.NgOrchestrationNotifyEventListener.NG_ORCHESTRATION;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.yaml.GitCommandRequest.gitRequestTimeout;
import static software.wings.beans.yaml.YamlConstants.GIT_YAML_LOG_PREFIX;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ManagerDelegateServiceDriver;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.gitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.git.GitCommandParams;
import io.harness.delegate.beans.git.GitCommitAndPushRequest;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.exception.GeneralException;
import io.harness.gitsync.common.CommonsMapper;
import io.harness.gitsync.common.beans.GitFileChange;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.callback.GitCommandCallback;
import io.harness.gitsync.core.service.YamlGitService;
import io.harness.gitsync.gitsyncerror.service.GitSyncErrorService;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.waiter.WaitNotifyEngine;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.TaskType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class YamlGitServiceImpl implements YamlGitService {
  private WaitNotifyEngine waitNotifyEngine;
  private SecretManagerClientService ngSecretService;
  private YamlGitConfigService yamlGitConfigService;
  private GitSyncErrorService gitSyncErrorService;
  private ManagerDelegateServiceDriver managerDelegateServiceDriver;
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
    logger.info(GIT_YAML_LOG_PREFIX + "Started handling harness -> git changeset");

    List<GitFileChange> gitFileChanges = yamlChangeSet.getGitFileChanges();

    for (GitFileChange gitFileChange : gitFileChanges) {
      createDelegateTaskForCommitAndPush(yamlChangeSet, accountId, gitFileChange);
    }
  }

  private void createDelegateTaskForCommitAndPush(
      YamlChangeSet yamlChangeSet, String accountId, GitFileChange gitFileChange) {
    YamlGitConfigDTO yamlGitConfig = getYamlGitConfigForHarnessToGitChangeSet(gitFileChange, yamlChangeSet);
    if (yamlGitConfig == null) {
      logger.info("No git sync configured for [{}]", gitFileChange.getFilePath());
    }
    Optional<GitConfigDTO> gitConfig = getGitConfig(yamlGitConfig);

    if (yamlGitConfig == null || !gitConfig.isPresent()) {
      throw new GeneralException(
          format(GIT_YAML_LOG_PREFIX + "YamlGitConfig: [%s] and gitConfig: [%s]  shouldn't be null for accountId [%s]",
              yamlGitConfig, gitConfig, accountId));
    }

    logger.info(GIT_YAML_LOG_PREFIX + "Creating COMMIT_AND_PUSH git delegate task for entity");
    TaskData taskData = getTaskDataForCommitAndPush(yamlChangeSet, gitFileChange, yamlGitConfig, gitConfig.get(),
        accountId, yamlChangeSet.getOrganizationId(), yamlChangeSet.getProjectId());
    Map<String, String> setupAbstractions = ImmutableMap.of("accountId", accountId);
    String taskId = managerDelegateServiceDriver.sendTaskAsync(accountId, setupAbstractions, taskData);

    waitNotifyEngine.waitForAllOn(NG_ORCHESTRATION,
        new GitCommandCallback(accountId, yamlChangeSet.getUuid(), COMMIT_AND_PUSH, yamlGitConfig.getGitConnectorId(),
            yamlGitConfig.getRepo(), yamlGitConfig.getBranch()),
        taskId);

    logger.info(
        GIT_YAML_LOG_PREFIX + "Successfully queued harness->git changeset for processing with delegate taskId=[{}]",
        taskId);
  }

  private TaskData getTaskDataForCommitAndPush(YamlChangeSet yamlChangeSet, GitFileChange gitFileChange,
      YamlGitConfigDTO yamlGitConfig, GitConfigDTO gitConfig, String accountIdentifier, String orgIdentifier,
      String projectIdentifier) {
    io.harness.delegate.beans.git.GitFileChange gitFileChangeDelegate =
        CommonsMapper.toDelegateGitFileChange(gitFileChange);
    GitAuthenticationDTO gitAuthenticationDecryptableEntity = gitConfig.getGitAuth();
    NGAccess basicNGAccessObject = BaseNGAccess.builder()
                                       .accountIdentifier(accountIdentifier)
                                       .orgIdentifier(orgIdentifier)
                                       .projectIdentifier(projectIdentifier)
                                       .build();
    List<EncryptedDataDetail> encryptedDataDetailList =
        ngSecretService.getEncryptionDetails(basicNGAccessObject, gitAuthenticationDecryptableEntity);
    GitCommandParams gitCommandParams = buildGitCommandParamsForCommitAndPush(
        yamlGitConfig, gitConfig, yamlChangeSet.getUuid(), gitFileChangeDelegate, encryptedDataDetailList);
    return TaskData.builder()
        .taskType(TaskType.NG_GIT_COMMAND.name())
        .async(true)
        .timeout(gitRequestTimeout)
        .parameters(new Object[] {gitCommandParams})
        .build();
  }

  private GitCommandParams buildGitCommandParamsForCommitAndPush(YamlGitConfigDTO yamlGitConfig, GitConfigDTO gitConfig,
      String yamlChangeSetIds, io.harness.delegate.beans.git.GitFileChange gitFileChangeDelegate,
      List<EncryptedDataDetail> encryptedDataDetailList) {
    return GitCommandParams.builder()
        .encryptionDetails(encryptedDataDetailList)
        .gitCommandType(COMMIT_AND_PUSH)
        .gitCommandRequest(GitCommitAndPushRequest.builder()
                               .gitFileChanges(Collections.singletonList(gitFileChangeDelegate))
                               .yamlGitConfigs(yamlGitConfig)
                               .forcePush(false)
                               .yamlChangeSetId(yamlChangeSetIds)
                               .build())
        .gitConfig(gitConfig)
        .build();
  }

  private Optional<GitConfigDTO> getGitConfig(YamlGitConfigDTO yamlGitConfig) {
    if (yamlGitConfig != null) {
      String branchName = yamlGitConfig.getBranch();
      String gitConnectorIdentifier = yamlGitConfig.getGitConnectorId();
      String repoName = yamlGitConfig.getRepo();
      return yamlGitConfigService.getGitConfig(yamlGitConfig, gitConnectorIdentifier, repoName, branchName);
    }
    return Optional.empty();
  }

  @Override
  public void removeGitSyncErrors(
      String accountId, String orgId, String projectId, List<GitFileChange> gitFileChangeList, boolean gitToHarness) {
    List<String> yamlFilePathList = gitFileChangeList.stream().map(GitFileChange::getFilePath).collect(toList());
    gitSyncErrorService.deleteByAccountIdOrgIdProjectIdAndFilePath(accountId, orgId, projectId, yamlFilePathList);
  }
}
