/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.core.fullsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.Microservice;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.FullSyncChangeSet;
import io.harness.gitsync.FullSyncResponse;
import io.harness.gitsync.FullSyncServiceGrpc;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;
import io.harness.ng.core.entitydetail.EntityDetailRestToProtoMapper;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class GitFullSyncProcessorServiceImpl implements GitFullSyncProcessorService {
  Map<Microservice, FullSyncServiceGrpc.FullSyncServiceBlockingStub> fullSyncServiceBlockingStubMap;
  YamlGitConfigService yamlGitConfigService;
  EntityDetailRestToProtoMapper entityDetailRestToProtoMapper;
  GitFullSyncEntityService gitFullSyncEntityService;

  private static int MAX_RETRY_COUNT = 2;

  public void processFile(GitFullSyncEntityInfo entityInfo) {
    boolean failed = false;
    try {
      FullSyncResponse fullSyncResponse = performSyncForEntity(entityInfo);
      failed = !fullSyncResponse.getSuccess();
    } catch (Exception e) {
      failed = true;
    }
    if (failed) {
      gitFullSyncEntityService.markQueuedOrFailed(
          entityInfo.getMessageId(), entityInfo.getAccountIdentifier(), entityInfo.getRetryCount(), MAX_RETRY_COUNT);
    }
  }

  private FullSyncResponse performSyncForEntity(GitFullSyncEntityInfo entityInfo) {
    final FullSyncServiceGrpc.FullSyncServiceBlockingStub fullSyncServiceBlockingStub =
        fullSyncServiceBlockingStubMap.get(Microservice.fromString(entityInfo.getMicroservice()));
    final YamlGitConfigDTO yamlGitConfigDTO = yamlGitConfigService.get(entityInfo.getProjectIdentifier(),
        entityInfo.getOrgIdentifier(), entityInfo.getAccountIdentifier(), entityInfo.getYamlGitConfigId());
    final FullSyncChangeSet changeSet = getFullSyncChangeSet(entityInfo, yamlGitConfigDTO, entityInfo.getMessageId());
    return fullSyncServiceBlockingStub.performEntitySync(changeSet);
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
        .build();
  }
}
