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
import io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.StringValue;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

  @Override
  public void triggerFullSync(FullSyncEventRequest fullSyncEventRequest, String messageId) {
    final EntityScopeInfo gitConfigScope = fullSyncEventRequest.getGitConfigScope();
    final ScopeDetails scopeDetails = getScopeDetails(gitConfigScope, messageId);
    fullSyncServiceBlockingStubMap.forEach((microservice, fullSyncServiceBlockingStub) -> {
      FileChanges entitiesForFullSync = null;
      try {
        // todo(abhinav): add retryInputSetReferenceProtoDTO
        entitiesForFullSync = GitSyncGrpcClientUtils.retryAndProcessException(
            fullSyncServiceBlockingStub::getEntitiesForFullSync, scopeDetails);
      } catch (Exception e) {
        log.error("Error encountered while getting entities while full sync for msvc {}", microservice, e);
        return;
      }
      emptyIfNull(entitiesForFullSync.getFileChangesList()).forEach(entityForFullSync -> {
        saveFullSyncEntityInfo(gitConfigScope, messageId, microservice, entityForFullSync);
      });
    });

    String projectIdentifier = gitConfigScope.getProjectId().getValue();
    String orgIdentifier = gitConfigScope.getOrgId().getValue();
    if (fullSyncEventRequest.getCreatePr()) {
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
