/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.core.fullsync;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.gitsync.core.beans.GitFullSyncEntityInfo.SyncStatus.QUEUED;

import io.harness.Microservice;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitPRCreateRequest;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.gitsync.FileChange;
import io.harness.gitsync.FileChanges;
import io.harness.gitsync.FullSyncEventRequest;
import io.harness.gitsync.FullSyncServiceGrpc.FullSyncServiceBlockingStub;
import io.harness.gitsync.ScopeDetails;
import io.harness.gitsync.common.helper.GitSyncGrpcClientUtils;
import io.harness.gitsync.common.service.ScmOrchestratorService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;
import io.harness.gitsync.core.fullsync.entity.GitFullSyncJob;
import io.harness.gitsync.core.fullsync.service.FullSyncJobService;
import io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.StringValue;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(DX)
@Slf4j
public class FullSyncAccumulatorServiceImpl implements FullSyncAccumulatorService {
  private final Map<Microservice, FullSyncServiceBlockingStub> fullSyncServiceBlockingStubMap;
  private final EntityDetailProtoToRestMapper entityDetailProtoToRestMapper;
  private final GitFullSyncEntityService gitFullSyncEntityService;
  private final ScmOrchestratorService scmOrchestratorService;
  private final YamlGitConfigService yamlGitConfigService;
  private final FullSyncJobService fullSyncJobService;

  @Override
  public void triggerFullSync(FullSyncEventRequest fullSyncEventRequest, String messageId) {
    log.info("Started triggering the git full sync job for the message Id {}", messageId);
    final EntityScopeInfo gitConfigScope = fullSyncEventRequest.getGitConfigScope();
    final ScopeDetails scopeDetails = getScopeDetails(gitConfigScope, messageId);
    for (Map.Entry<Microservice, FullSyncServiceBlockingStub> fullSyncStubEntry :
        fullSyncServiceBlockingStubMap.entrySet()) {
      FullSyncServiceBlockingStub fullSyncServiceBlockingStub = fullSyncStubEntry.getValue();
      Microservice microservice = fullSyncStubEntry.getKey();
      FileChanges entitiesForFullSync = null;
      try {
        // todo(abhinav): add retryInputSetReferenceProtoDTO
        log.info("Trying to get of the files for the message Id {} for the microservice {}", messageId, microservice);
        entitiesForFullSync = GitSyncGrpcClientUtils.retryAndProcessException(
            fullSyncServiceBlockingStub::getEntitiesForFullSync, scopeDetails);
      } catch (Exception e) {
        log.error("Error encountered while getting entities while full sync for msvc {}", microservice, e);
        continue;
      }
      int fileNumber = entitiesForFullSync == null ? 0 : emptyIfNull(entitiesForFullSync.getFileChangesList()).size();
      log.info("Saving {} files for the microservice {}", fileNumber, microservice);
      emptyIfNull(entitiesForFullSync.getFileChangesList()).forEach(entityForFullSync -> {
        saveFullSyncEntityInfo(gitConfigScope, messageId, microservice, entityForFullSync);
      });
    }
    GitFullSyncJob gitFullSyncJob = saveTheFullSyncJob(fullSyncEventRequest, messageId);
    if (gitFullSyncJob == null) {
      log.info("The job is not created for the message id {}, as a job with id already exists", messageId);
      return;
    }
    if (fullSyncEventRequest.getCreatePr()) {
      createAPullRequest(fullSyncEventRequest);
    }
  }

  private GitFullSyncJob saveTheFullSyncJob(FullSyncEventRequest fullSyncEventRequest, String messageId) {
    final EntityScopeInfo gitConfigScope = fullSyncEventRequest.getGitConfigScope();
    GitFullSyncJob fullSyncJob = GitFullSyncJob.builder()
                                     .accountIdentifier(gitConfigScope.getAccountId())
                                     .orgIdentifier(getStringValueFromProtoString(gitConfigScope.getOrgId()))
                                     .projectIdentifier(getStringValueFromProtoString(gitConfigScope.getProjectId()))
                                     .yamlGitConfigIdentifier(gitConfigScope.getIdentifier())
                                     .syncStatus(QUEUED.name())
                                     .messageId(messageId)
                                     .retryCount(0)
                                     .build();
    try {
      return fullSyncJobService.save(fullSyncJob);
    } catch (DuplicateKeyException ex) {
      log.error("A full sync job already exists for the message {}", messageId, ex);
    }
    return null;
  }

  private void createAPullRequest(FullSyncEventRequest fullSyncEventRequest) {
    final EntityScopeInfo gitConfigScope = fullSyncEventRequest.getGitConfigScope();
    String projectIdentifier = gitConfigScope.getProjectId().getValue();
    String orgIdentifier = gitConfigScope.getOrgId().getValue();
    YamlGitConfigDTO yamlGitConfigDTO = yamlGitConfigService.get(
        projectIdentifier, orgIdentifier, gitConfigScope.getAccountId(), gitConfigScope.getIdentifier());
    String title = "Full Sync for the project {}" + gitConfigScope.getProjectId();
    GitPRCreateRequest createPRRequest = GitPRCreateRequest.builder()
                                             .accountIdentifier(gitConfigScope.getAccountId())
                                             .orgIdentifier(orgIdentifier)
                                             .projectIdentifier(projectIdentifier)
                                             .yamlGitConfigRef(gitConfigScope.getIdentifier())
                                             .title(title)
                                             .sourceBranch(fullSyncEventRequest.getBranch())
                                             .targetBranch(fullSyncEventRequest.getTargetBranch())
                                             .build();
    scmOrchestratorService.processScmRequestUsingConnectorSettings(scmClientFacilitatorService
        -> scmClientFacilitatorService.createPullRequest(createPRRequest),
        projectIdentifier, orgIdentifier, gitConfigScope.getAccountId(), yamlGitConfigDTO.getGitConnectorRef());
  }

  private void saveFullSyncEntityInfo(
      EntityScopeInfo entityScopeInfo, String messageId, Microservice microservice, FileChange entityForFullSync) {
    final GitFullSyncEntityInfo gitFullSyncEntityInfo =
        GitFullSyncEntityInfo.builder()
            .accountIdentifier(entityScopeInfo.getAccountId())
            .filePath(entityForFullSync.getFilePath())
            .projectIdentifier(getStringValueFromProtoString(entityScopeInfo.getProjectId()))
            .orgIdentifier(getStringValueFromProtoString(entityScopeInfo.getOrgId()))
            .microservice(microservice.name())
            .messageId(messageId)
            .entityDetail(entityDetailProtoToRestMapper.createEntityDetailDTO(entityForFullSync.getEntityDetail()))
            .syncStatus(QUEUED.name())
            .yamlGitConfigId(entityScopeInfo.getIdentifier())
            .retryCount(0)
            .build();
    gitFullSyncEntityService.save(gitFullSyncEntityInfo);
  }

  private ScopeDetails getScopeDetails(EntityScopeInfo entityScopeInfo, String messageId) {
    Map<String, String> logContext = new HashMap<>();
    logContext.put("messageId", messageId);
    return ScopeDetails.newBuilder().setEntityScope(entityScopeInfo).putAllLogContext(logContext).build();
  }

  private String getStringValueFromProtoString(StringValue stringValue) {
    if (stringValue != null) {
      return stringValue.getValue();
    }
    return null;
  }
}
