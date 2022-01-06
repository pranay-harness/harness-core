/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.core.fullsync;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;

import io.harness.Microservice;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.FullSyncChangeSet;
import io.harness.gitsync.FullSyncResponse;
import io.harness.gitsync.FullSyncServiceGrpc;
import io.harness.gitsync.common.helper.GitSyncGrpcClientUtils;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;
import io.harness.gitsync.core.fullsync.beans.GitFullSyncEntityProcessingResponse;
import io.harness.gitsync.core.fullsync.entity.GitFullSyncJob;
import io.harness.gitsync.core.fullsync.service.FullSyncJobService;
import io.harness.ng.core.entitydetail.EntityDetailRestToProtoMapper;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(DX)
public class GitFullSyncProcessorServiceImpl implements io.harness.gitsync.core.fullsync.GitFullSyncProcessorService {
  Map<Microservice, FullSyncServiceGrpc.FullSyncServiceBlockingStub> fullSyncServiceBlockingStubMap;
  YamlGitConfigService yamlGitConfigService;
  EntityDetailRestToProtoMapper entityDetailRestToProtoMapper;
  GitFullSyncEntityService gitFullSyncEntityService;
  FullSyncJobService fullSyncJobService;

  private static int MAX_RETRY_COUNT = 2;

  @Override
  public GitFullSyncEntityProcessingResponse processFile(GitFullSyncEntityInfo entityInfo) {
    boolean failed = false;
    FullSyncResponse fullSyncResponse = null;
    try {
      fullSyncResponse = performSyncForEntity(entityInfo);
      failed = !fullSyncResponse.getSuccess();
    } catch (Exception e) {
      failed = true;
    }
    if (failed) {
      String errorMsg = "";
      if (fullSyncResponse != null) {
        errorMsg = fullSyncResponse.getErrorMsg();
      }
      gitFullSyncEntityService.markQueuedOrFailed(entityInfo.getUuid(), entityInfo.getAccountIdentifier(),
          entityInfo.getRetryCount(), MAX_RETRY_COUNT, errorMsg);
      return GitFullSyncEntityProcessingResponse.builder().syncStatus(GitFullSyncEntityInfo.SyncStatus.FAILED).build();
    } else {
      gitFullSyncEntityService.markSuccessful(entityInfo.getUuid(), entityInfo.getAccountIdentifier());
      return GitFullSyncEntityProcessingResponse.builder().syncStatus(GitFullSyncEntityInfo.SyncStatus.PUSHED).build();
    }
  }

  private FullSyncResponse performSyncForEntity(GitFullSyncEntityInfo entityInfo) {
    final FullSyncServiceGrpc.FullSyncServiceBlockingStub fullSyncServiceBlockingStub =
        fullSyncServiceBlockingStubMap.get(Microservice.fromString(entityInfo.getMicroservice()));
    final YamlGitConfigDTO yamlGitConfigDTO = yamlGitConfigService.get(entityInfo.getProjectIdentifier(),
        entityInfo.getOrgIdentifier(), entityInfo.getAccountIdentifier(), entityInfo.getYamlGitConfigId());
    final FullSyncChangeSet changeSet = getFullSyncChangeSet(entityInfo, yamlGitConfigDTO, entityInfo.getMessageId());
    return GitSyncGrpcClientUtils.retryAndProcessException(fullSyncServiceBlockingStub::performEntitySync, changeSet);
  }

  private FullSyncChangeSet getFullSyncChangeSet(
      GitFullSyncEntityInfo entityInfo, YamlGitConfigDTO yamlGitConfigDTO, String messageId) {
    Map<String, String> logContext = new HashMap<>();
    logContext.put("messageId", messageId);

    return FullSyncChangeSet.newBuilder()
        .setBranchName(yamlGitConfigDTO.getBranch())
        .setEntityDetail(entityDetailRestToProtoMapper.createEntityDetailDTO(entityInfo.getEntityDetail()))
        .setFilePath(entityInfo.getFilePath())
        .setYamlGitConfigIdentifier(yamlGitConfigDTO.getIdentifier())
        .putAllLogContext(logContext)
        .setAccountIdentifier(entityInfo.getAccountIdentifier())
        .setFolderPath(yamlGitConfigDTO.getDefaultRootFolder().getRootFolder())
        .setCommitMessage(getCommitMessageForTheFullSyncFlow(entityInfo.getFilePath()))
        .build();
  }

  private String getCommitMessageForTheFullSyncFlow(String filePath) {
    return "Harness Full Sync: "
        + "Add file " + filePath;
  }

  @Override
  public void performFullSync(GitFullSyncJob fullSyncJob) {
    log.info("Started full sync for the job {}", fullSyncJob.getMessageId());
    List<GitFullSyncEntityInfo> allEntitiesToBeSynced =
        gitFullSyncEntityService.list(fullSyncJob.getAccountIdentifier(), fullSyncJob.getMessageId());
    log.info("Number of files is {}", emptyIfNull(allEntitiesToBeSynced).size());
    boolean processingFailed = false;
    for (GitFullSyncEntityInfo gitFullSyncEntityInfo : emptyIfNull(allEntitiesToBeSynced)) {
      if (gitFullSyncEntityInfo.getSyncStatus().equals(GitFullSyncEntityInfo.SyncStatus.PUSHED.name())) {
        continue;
      }
      final GitFullSyncEntityProcessingResponse gitFullSyncEntityProcessingResponse =
          processFile(gitFullSyncEntityInfo);
      log.info("Processed the file with status {} {}", gitFullSyncEntityInfo.getFilePath(),
          gitFullSyncEntityProcessingResponse.getSyncStatus());
      if (gitFullSyncEntityProcessingResponse.getSyncStatus() != GitFullSyncEntityInfo.SyncStatus.PUSHED) {
        processingFailed = true;
      }
    }

    updateTheStatusOfJob(processingFailed, fullSyncJob);
    log.info("Completed full sync for the job {}", fullSyncJob.getMessageId());
  }

  private void updateTheStatusOfJob(boolean processingFailed, GitFullSyncJob fullSyncJob) {
    if (processingFailed) {
      if (fullSyncJob.getRetryCount() >= MAX_RETRY_COUNT) {
        fullSyncJobService.markFullSyncJobAsFailed(
            fullSyncJob.getAccountIdentifier(), fullSyncJob.getUuid(), GitFullSyncJob.SyncStatus.FAILED);
      } else {
        fullSyncJobService.markFullSyncJobAsFailed(fullSyncJob.getAccountIdentifier(), fullSyncJob.getUuid(),
            GitFullSyncJob.SyncStatus.FAILED_WITH_RETRIES_LEFT);
      }
    } else {
      fullSyncJobService.markFullSyncJobAsSuccess(fullSyncJob.getAccountIdentifier(), fullSyncJob.getUuid());
    }
  }
}
