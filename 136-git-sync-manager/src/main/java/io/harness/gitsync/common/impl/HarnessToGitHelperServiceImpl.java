package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.common.beans.BranchSyncStatus.UNSYNCED;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.beans.IdentifierRef;
import io.harness.common.EntityReference;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.helper.EncryptionHelper;
import io.harness.connector.helper.GitApiAccessDecryptionHelper;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.BranchDetails;
import io.harness.gitsync.FileInfo;
import io.harness.gitsync.PushInfo;
import io.harness.gitsync.RepoDetails;
import io.harness.gitsync.UserPrincipal;
import io.harness.gitsync.common.beans.BranchSyncStatus;
import io.harness.gitsync.common.beans.GitBranch;
import io.harness.gitsync.common.beans.GitSyncDirection;
import io.harness.gitsync.common.beans.InfoForGitPush;
import io.harness.gitsync.common.beans.InfoForGitPush.InfoForGitPushBuilder;
import io.harness.gitsync.common.dtos.GitSyncEntityDTO;
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
import io.harness.gitsync.scm.ScmGitUtils;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper;
import io.harness.ng.userprofile.services.api.SourceCodeManagerService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.tasks.DecryptGitApiAccessHelper;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

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
  private final SourceCodeManagerService sourceCodeManagerService;
  private final ScmOrchestratorService scmOrchestratorService;
  private final GitBranchSyncService gitBranchSyncService;
  private final GitCommitService gitCommitService;
  private final UserProfileHelper userProfileHelper;

  @Inject
  public HarnessToGitHelperServiceImpl(@Named("connectorDecoratorService") ConnectorService connectorService,
      DecryptGitApiAccessHelper decryptScmApiAccess, GitEntityService gitEntityService,
      YamlGitConfigService yamlGitConfigService, EntityDetailProtoToRestMapper entityDetailRestToProtoMapper,
      ExecutorService executorService, GitBranchService gitBranchService, EncryptionHelper encryptionHelper,
      SourceCodeManagerService sourceCodeManagerService, ScmOrchestratorService scmOrchestratorService,
      GitBranchSyncService gitBranchSyncService, GitCommitService gitCommitService,
      UserProfileHelper userProfileHelper) {
    this.connectorService = connectorService;
    this.decryptScmApiAccess = decryptScmApiAccess;
    this.gitEntityService = gitEntityService;
    this.yamlGitConfigService = yamlGitConfigService;
    this.entityDetailRestToProtoMapper = entityDetailRestToProtoMapper;
    this.executorService = executorService;
    this.gitBranchService = gitBranchService;
    this.encryptionHelper = encryptionHelper;
    this.sourceCodeManagerService = sourceCodeManagerService;
    this.scmOrchestratorService = scmOrchestratorService;
    this.gitBranchSyncService = gitBranchSyncService;
    this.gitCommitService = gitCommitService;
    this.userProfileHelper = userProfileHelper;
  }

  @Override
  public InfoForGitPush getInfoForPush(FileInfo fileInfo, EntityReference entityReference, EntityType entityType) {
    final String accountId = fileInfo.getAccountId();
    final String filePath = fileInfo.getFilePath();
    final String branch = fileInfo.getBranch();
    final String yamlGitConfigId = fileInfo.getYamlGitConfigId();
    final UserPrincipal userPrincipal = fileInfo.getUserPrincipal();

    final InfoForGitPushBuilder infoForGitPushBuilder = InfoForGitPush.builder();
    final YamlGitConfigDTO yamlGitConfig = yamlGitConfigService.get(
        entityReference.getProjectIdentifier(), entityReference.getOrgIdentifier(), accountId, yamlGitConfigId);
    final GitSyncEntityDTO gitSyncEntityDTO = gitEntityService.get(entityReference, entityType, branch);
    if (gitSyncEntityDTO != null) {
      if (filePath != null) {
        if (!gitSyncEntityDTO.getEntityGitPath().equals(filePath)) {
          throw new InvalidRequestException("Incorrect file path");
        }
      }
    }
    final boolean executeOnDelegate = scmOrchestratorService.isExecuteOnDelegate(
        entityReference.getProjectIdentifier(), entityReference.getOrgIdentifier(), accountId);
    log.info("Configuration for git push operation to execute on delegate {}", executeOnDelegate);
    if (executeOnDelegate) {
      final Pair<ScmConnector, List<EncryptedDataDetail>> connectorWithEncryptionDetails =
          getConnectorWithEncryptionDetails(accountId, yamlGitConfig, userPrincipal);
      infoForGitPushBuilder.encryptedDataDetailList(connectorWithEncryptionDetails.getRight())
          .scmConnector(connectorWithEncryptionDetails.getLeft());
    } else {
      infoForGitPushBuilder.scmConnector(getDecryptedScmConnector(accountId, yamlGitConfig, userPrincipal));
    }
    return infoForGitPushBuilder.filePath(filePath)
        .branch(branch)
        .isDefault(branch.equals(yamlGitConfig.getBranch()))
        .yamlGitConfigId(yamlGitConfig.getIdentifier())
        .accountId(accountId)
        .orgIdentifier(entityReference.getOrgIdentifier())
        .projectIdentifier(entityReference.getProjectIdentifier())
        .defaultBranchName(yamlGitConfig.getBranch())
        .executeOnDelegate(executeOnDelegate)
        .build();
  }

  private Pair<ScmConnector, List<EncryptedDataDetail>> getConnectorWithEncryptionDetails(
      String accountId, YamlGitConfigDTO yamlGitConfig, UserPrincipal userPrincipal) {
    final Optional<ConnectorResponseDTO> connectorResponseDTO = getConnector(accountId, yamlGitConfig, userPrincipal);
    return connectorResponseDTO
        .map(connector -> {
          final DecryptableEntity apiAccessDecryptableEntity =
              GitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity(
                  (ScmConnector) connector.getConnector().getConnectorConfig());
          final BaseNGAccess ngAccess = BaseNGAccess.builder()
                                            .accountIdentifier(accountId)
                                            .orgIdentifier(connector.getConnector().getOrgIdentifier())
                                            .projectIdentifier(connector.getConnector().getProjectIdentifier())
                                            .build();
          final List<EncryptedDataDetail> encryptionDetail =
              encryptionHelper.getEncryptionDetail(apiAccessDecryptableEntity, ngAccess.getAccountIdentifier(),
                  ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());
          return Pair.of((ScmConnector) connector.getConnector().getConnectorConfig(), encryptionDetail);
        })
        .orElseThrow(() -> new InvalidRequestException("Connector doesn't exist."));
  }

  private ScmConnector getDecryptedScmConnector(
      String accountId, YamlGitConfigDTO yamlGitConfig, UserPrincipal userPrincipal) {
    final Optional<ConnectorResponseDTO> connectorResponseDTO = getConnector(accountId, yamlGitConfig, userPrincipal);
    return connectorResponseDTO
        .map(connector
            -> decryptScmApiAccess.decryptScmApiAccess((ScmConnector) connector.getConnector().getConnectorConfig(),
                accountId, yamlGitConfig.getProjectIdentifier(), yamlGitConfig.getOrganizationIdentifier()))
        .orElseThrow(() -> new InvalidRequestException("Connector doesn't exist."));
  }

  private Optional<ConnectorResponseDTO> getConnector(
      String accountId, YamlGitConfigDTO yamlGitConfig, UserPrincipal userPrincipal) {
    final String gitConnectorId = yamlGitConfig.getGitConnectorRef();
    final IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(gitConnectorId, accountId,
        yamlGitConfig.getOrganizationIdentifier(), yamlGitConfig.getProjectIdentifier(), null);
    final Optional<ConnectorResponseDTO> connectorResponseDTO = connectorService.get(accountId,
        identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
    if (!connectorResponseDTO.isPresent()) {
      throw new InvalidRequestException(String.format("Ref Connector [{}] doesn't exist.", gitConnectorId));
    }
    final ConnectorResponseDTO connector = connectorResponseDTO.get();
    userProfileHelper.setConnectorDetailsFromUserProfile(yamlGitConfig, userPrincipal, connector);
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
    if (!pushInfo.getIsSyncFromGit()) {
      saveGitCommit(pushInfo, yamlGitConfigDTO);
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
}
