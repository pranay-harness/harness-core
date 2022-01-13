/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.common.beans.BranchSyncStatus.UNSYNCED;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.common.EntityReference;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.helper.EncryptionHelper;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.gitsync.BranchDetails;
import io.harness.gitsync.ChangeType;
import io.harness.gitsync.FileInfo;
import io.harness.gitsync.Principal;
import io.harness.gitsync.PushFileResponse;
import io.harness.gitsync.PushInfo;
import io.harness.gitsync.RepoDetails;
import io.harness.gitsync.common.beans.BranchSyncStatus;
import io.harness.gitsync.common.beans.GitBranch;
import io.harness.gitsync.common.beans.GitSyncDirection;
import io.harness.gitsync.common.beans.InfoForGitPush;
import io.harness.gitsync.common.helper.UserProfileHelper;
import io.harness.gitsync.common.service.GitBranchService;
import io.harness.gitsync.common.service.GitBranchSyncService;
import io.harness.gitsync.common.service.GitEntityService;
import io.harness.gitsync.common.service.HarnessToGitHelperService;
import io.harness.gitsync.common.service.ScmOrchestratorService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.beans.GitCommit.GitCommitProcessingStatus;
import io.harness.gitsync.core.dtos.GitCommitDTO;
import io.harness.gitsync.core.service.GitCommitService;
import io.harness.gitsync.gitfileactivity.beans.GitFileProcessingSummary;
import io.harness.gitsync.gitsyncerror.service.GitSyncErrorService;
import io.harness.gitsync.scm.ScmGitUtils;
import io.harness.grpc.utils.StringValueUtils;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper;
import io.harness.product.ci.scm.proto.CreateFileResponse;
import io.harness.product.ci.scm.proto.DeleteFileResponse;
import io.harness.product.ci.scm.proto.UpdateFileResponse;
import io.harness.tasks.DecryptGitApiAccessHelper;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(DX)
public class HarnessToGitHelperServiceImpl implements HarnessToGitHelperService {
  private final ConnectorService connectorService;
  private final DecryptGitApiAccessHelper decryptScmApiAccess;
  private final GitEntityService gitEntityService;
  private final YamlGitConfigService yamlGitConfigService;
  private final EntityDetailProtoToRestMapper entityDetailRestToProtoMapper;
  private final ExecutorService executorService;
  private final GitBranchService gitBranchService;
  private final EncryptionHelper encryptionHelper;
  private final ScmOrchestratorService scmOrchestratorService;
  private final GitBranchSyncService gitBranchSyncService;
  private final GitCommitService gitCommitService;
  private final UserProfileHelper userProfileHelper;
  private final GitSyncErrorService gitSyncErrorService;

  @Inject
  public HarnessToGitHelperServiceImpl(@Named("connectorDecoratorService") ConnectorService connectorService,
      DecryptGitApiAccessHelper decryptScmApiAccess, GitEntityService gitEntityService,
      YamlGitConfigService yamlGitConfigService, EntityDetailProtoToRestMapper entityDetailRestToProtoMapper,
      ExecutorService executorService, GitBranchService gitBranchService, EncryptionHelper encryptionHelper,
      ScmOrchestratorService scmOrchestratorService, GitBranchSyncService gitBranchSyncService,
      GitCommitService gitCommitService, UserProfileHelper userProfileHelper, GitSyncErrorService gitSyncErrorService) {
    this.connectorService = connectorService;
    this.decryptScmApiAccess = decryptScmApiAccess;
    this.gitEntityService = gitEntityService;
    this.yamlGitConfigService = yamlGitConfigService;
    this.entityDetailRestToProtoMapper = entityDetailRestToProtoMapper;
    this.executorService = executorService;
    this.gitBranchService = gitBranchService;
    this.encryptionHelper = encryptionHelper;
    this.scmOrchestratorService = scmOrchestratorService;
    this.gitBranchSyncService = gitBranchSyncService;
    this.gitCommitService = gitCommitService;
    this.userProfileHelper = userProfileHelper;
    this.gitSyncErrorService = gitSyncErrorService;
  }

  private Optional<ConnectorResponseDTO> getConnector(
      String accountId, YamlGitConfigDTO yamlGitConfig, Principal principal) {
    final String gitConnectorId = yamlGitConfig.getGitConnectorRef();
    final IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(gitConnectorId, accountId,
        yamlGitConfig.getOrganizationIdentifier(), yamlGitConfig.getProjectIdentifier(), null);
    final Optional<ConnectorResponseDTO> connectorResponseDTO = connectorService.get(accountId,
        identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
    if (!connectorResponseDTO.isPresent()) {
      throw new InvalidRequestException(String.format("Ref Connector [{}] doesn't exist.", gitConnectorId));
    }
    final ConnectorResponseDTO connector = connectorResponseDTO.get();
    if (principal.hasUserPrincipal()) {
      userProfileHelper.setConnectorDetailsFromUserProfile(yamlGitConfig, principal.getUserPrincipal(), connector);
    }
    setRepoUrlInConnector(yamlGitConfig, connector);
    return Optional.of(connector);
  }

  private void setRepoUrlInConnector(YamlGitConfigDTO yamlGitConfig, ConnectorResponseDTO connector) {
    final ScmConnector connectorConfigDTO = (ScmConnector) connector.getConnector().getConnectorConfig();
    connectorConfigDTO.setUrl(yamlGitConfig.getRepo());
  }

  @Override
  public void postPushOperation(PushInfo pushInfo) {
    log.info("Post push info {}", pushInfo);
    final EntityDetail entityDetailDTO =
        entityDetailRestToProtoMapper.createEntityDetailDTO(pushInfo.getEntityDetail());
    final EntityReference entityRef = entityDetailDTO.getEntityRef();
    final EntityDetailProtoDTO entityDetail = pushInfo.getEntityDetail();
    final YamlGitConfigDTO yamlGitConfigDTO = yamlGitConfigService.get(entityRef.getProjectIdentifier(),
        entityRef.getOrgIdentifier(), entityRef.getAccountIdentifier(), pushInfo.getYamlGitConfigId());
    // todo(abhinav): Think about what if something happens in middle of operations.
    saveGitEntity(pushInfo, entityDetail, yamlGitConfigDTO);
    resolveConnectivityErrors(pushInfo, yamlGitConfigDTO);
    if (!pushInfo.getIsSyncFromGit()) {
      saveGitCommit(pushInfo, yamlGitConfigDTO);
      resolveGitToHarnessErrors(pushInfo, yamlGitConfigDTO);
    }
    shortListTheBranch(
        yamlGitConfigDTO, entityRef.getAccountIdentifier(), pushInfo.getBranchName(), pushInfo.getIsNewBranch());

    if (pushInfo.getIsNewBranch()) {
      executorService.submit(
          ()
              -> gitBranchSyncService.createBranchSyncEvent(yamlGitConfigDTO.getAccountIdentifier(),
                  yamlGitConfigDTO.getOrganizationIdentifier(), yamlGitConfigDTO.getProjectIdentifier(),
                  yamlGitConfigDTO.getIdentifier(), yamlGitConfigDTO.getRepo(), pushInfo.getBranchName(),
                  ScmGitUtils.createFilePath(pushInfo.getFolderPath(), pushInfo.getFilePath())));
    }
  }

  private void resolveConnectivityErrors(PushInfo pushInfo, YamlGitConfigDTO yamlGitConfigDTO) {
    gitSyncErrorService.resolveConnectivityErrors(pushInfo.getAccountId(), yamlGitConfigDTO.getRepo());
  }

  private void resolveGitToHarnessErrors(PushInfo pushInfo, YamlGitConfigDTO yamlGitConfigDTO) {
    String completeFilePath = ScmGitUtils.createFilePath(pushInfo.getFolderPath(), pushInfo.getFilePath());
    gitSyncErrorService.resolveGitToHarnessErrors(pushInfo.getAccountId(), yamlGitConfigDTO.getRepo(),
        pushInfo.getBranchName(), new HashSet<>(Collections.singleton(completeFilePath)), pushInfo.getCommitId());
  }

  private void saveGitEntity(PushInfo pushInfo, EntityDetailProtoDTO entityDetail, YamlGitConfigDTO yamlGitConfigDTO) {
    gitEntityService.save(pushInfo.getAccountId(), entityDetailRestToProtoMapper.createEntityDetailDTO(entityDetail),
        yamlGitConfigDTO, pushInfo.getFolderPath(), pushInfo.getFilePath(), pushInfo.getCommitId(),
        pushInfo.getBranchName());
  }

  private void saveGitCommit(PushInfo pushInfo, YamlGitConfigDTO yamlGitConfigDTO) {
    gitCommitService.upsertOnCommitIdAndRepoUrlAndGitSyncDirection(
        GitCommitDTO.builder()
            .commitId(pushInfo.getCommitId())
            .accountIdentifier(pushInfo.getAccountId())
            .branchName(pushInfo.getBranchName())
            .gitSyncDirection(GitSyncDirection.HARNESS_TO_GIT)
            .fileProcessingSummary(GitFileProcessingSummary.builder().successCount(1L).build())
            .repoURL(yamlGitConfigDTO.getRepo())
            .status(GitCommitProcessingStatus.COMPLETED)
            .build());
  }

  private void shortListTheBranch(
      YamlGitConfigDTO yamlGitConfigDTO, String accountIdentifier, String branchName, boolean isNewBranch) {
    GitBranch gitBranch = gitBranchService.get(accountIdentifier, yamlGitConfigDTO.getRepo(), branchName);
    if (gitBranch == null) {
      createGitBranch(yamlGitConfigDTO, accountIdentifier, branchName, UNSYNCED);
    }
  }

  @Override
  public Boolean isGitSyncEnabled(EntityScopeInfo entityScopeInfo) {
    return yamlGitConfigService.isGitSyncEnabled(entityScopeInfo.getAccountId(), entityScopeInfo.getOrgId().getValue(),
        entityScopeInfo.getProjectId().getValue());
  }

  private void createGitBranch(
      YamlGitConfigDTO yamlGitConfigDTO, String accountId, String branch, BranchSyncStatus synced) {
    GitBranch gitBranch = GitBranch.builder()
                              .accountIdentifier(accountId)
                              .branchName(branch)
                              .branchSyncStatus(synced)
                              .repoURL(yamlGitConfigDTO.getRepo())
                              .build();
    gitBranchService.save(gitBranch);
  }

  @Override
  public BranchDetails getBranchDetails(RepoDetails repoDetails) {
    final YamlGitConfigDTO yamlGitConfigDTO = yamlGitConfigService.get(repoDetails.getProjectIdentifier().getValue(),
        repoDetails.getOrgIdentifier().getValue(), repoDetails.getAccountId(), repoDetails.getYamlGitConfigId());
    if (yamlGitConfigDTO == null) {
      return BranchDetails.newBuilder().build();
    }
    return BranchDetails.newBuilder().setDefaultBranch(yamlGitConfigDTO.getBranch()).build();
  }

  @Override
  public PushFileResponse pushFile(FileInfo request) {
    final EntityDetail entityDetailDTO = entityDetailRestToProtoMapper.createEntityDetailDTO(request.getEntityDetail());
    final EntityReference entityReference = entityDetailDTO.getEntityRef();
    final String accountId = request.getAccountId();
    final String yamlGitConfigId = request.getYamlGitConfigId();
    final ChangeType changeType = request.getChangeType();
    final YamlGitConfigDTO yamlGitConfig = yamlGitConfigService.get(
        entityReference.getProjectIdentifier(), entityReference.getOrgIdentifier(), accountId, yamlGitConfigId);

    final InfoForGitPush infoForGitPush = getInfoForGitPush(request, entityReference, accountId, yamlGitConfig);

    switch (changeType) {
      case MODIFY:
        final UpdateFileResponse updateFileResponse =
            scmOrchestratorService.processScmRequest(scmClientFacilitatorService
                -> scmClientFacilitatorService.updateFile(infoForGitPush),
                entityReference.getProjectIdentifier(), entityReference.getOrgIdentifier(), accountId);
        return PushFileResponse.newBuilder()
            .setAccountId(accountId)
            .setError(updateFileResponse.getError())
            .setScmResponseCode(updateFileResponse.getStatus())
            .setStatus(1)
            .setIsDefault(request.getBranch().equals(yamlGitConfig.getBranch()))
            .setDefaultBranchName(yamlGitConfig.getBranch())
            .setCommitId(updateFileResponse.getCommitId())
            .build();
      case ADD:
        final CreateFileResponse createFileResponse =
            scmOrchestratorService.processScmRequest(scmClientFacilitatorService
                -> scmClientFacilitatorService.createFile(infoForGitPush),
                entityReference.getProjectIdentifier(), entityReference.getOrgIdentifier(), accountId);
        return PushFileResponse.newBuilder()
            .setAccountId(accountId)
            .setError(createFileResponse.getError())
            .setScmResponseCode(createFileResponse.getStatus())
            .setStatus(1)
            .setIsDefault(request.getBranch().equals(yamlGitConfig.getBranch()))
            .setDefaultBranchName(yamlGitConfig.getBranch())
            .setCommitId(createFileResponse.getCommitId())
            .build();
      case DELETE:
        final DeleteFileResponse deleteFileResponse =
            scmOrchestratorService.processScmRequest(scmClientFacilitatorService
                -> scmClientFacilitatorService.deleteFile(infoForGitPush),
                entityReference.getProjectIdentifier(), entityReference.getOrgIdentifier(), accountId);
        return PushFileResponse.newBuilder()
            .setAccountId(accountId)
            .setError(deleteFileResponse.getError())
            .setScmResponseCode(deleteFileResponse.getStatus())
            .setStatus(1)
            .setIsDefault(request.getBranch().equals(yamlGitConfig.getBranch()))
            .setDefaultBranchName(yamlGitConfig.getBranch())
            .setCommitId(deleteFileResponse.getCommitId())
            .build();
      default:
        throw new UnexpectedException("Unknown change type encountered.");
    }
  }

  private InfoForGitPush getInfoForGitPush(
      FileInfo request, EntityReference entityReference, String accountId, YamlGitConfigDTO yamlGitConfig) {
    final Optional<ConnectorResponseDTO> connector = getConnector(accountId, yamlGitConfig, request.getPrincipal());
    if (!connector.isPresent()) {
      throw new InvalidRequestException(
          String.format("Connector with identifier %s deleted", yamlGitConfig.getGitConnectorRef()));
    }
    final ScmConnector connectorConfig = (ScmConnector) connector.get().getConnector().getConnectorConfig();
    connectorConfig.setUrl(yamlGitConfig.getRepo());

    return InfoForGitPush.builder()
        .accountId(accountId)
        .orgIdentifier(entityReference.getOrgIdentifier())
        .projectIdentifier(entityReference.getProjectIdentifier())
        .branch(request.getBranch())
        .baseBranch(StringValueUtils.getStringFromStringValue(request.getBaseBranch()))
        .isNewBranch(request.getIsNewBranch())
        .commitMsg(StringValueUtils.getStringFromStringValue(request.getCommitMsg()))
        .filePath(request.getFilePath())
        .folderPath(request.getFolderPath())
        .oldFileSha(StringValueUtils.getStringFromStringValue(request.getOldFileSha()))
        .yaml(request.getYaml())
        .scmConnector(connectorConfig)
        .build();
  }
}
