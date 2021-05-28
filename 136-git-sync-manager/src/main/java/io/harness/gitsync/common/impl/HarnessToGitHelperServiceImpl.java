package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.common.beans.BranchSyncStatus.SYNCED;
import static io.harness.gitsync.common.beans.BranchSyncStatus.SYNCING;
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
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessDTO;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessType;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernameTokenDTO;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.encryption.SecretRefData;
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
import io.harness.gitsync.common.beans.InfoForGitPush;
import io.harness.gitsync.common.beans.InfoForGitPush.InfoForGitPushBuilder;
import io.harness.gitsync.common.dtos.GitSyncEntityDTO;
import io.harness.gitsync.common.dtos.GitSyncSettingsDTO;
import io.harness.gitsync.common.service.GitBranchService;
import io.harness.gitsync.common.service.GitEntityService;
import io.harness.gitsync.common.service.GitSyncSettingsService;
import io.harness.gitsync.common.service.HarnessToGitHelperService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.common.service.gittoharness.GitToHarnessProcessorService;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.gitsync.scm.ScmGitUtils;
import io.harness.manage.GlobalContextManager;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper;
import io.harness.ng.userprofile.commons.GithubSCMDTO;
import io.harness.ng.userprofile.commons.SCMType;
import io.harness.ng.userprofile.commons.SourceCodeManagerDTO;
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
  private final GitToHarnessProcessorService gitToHarnessProcessorService;
  private final ExecutorService executorService;
  private final GitBranchService gitBranchService;
  private final EncryptionHelper encryptionHelper;
  private final SourceCodeManagerService sourceCodeManagerService;
  private final GitSyncSettingsService gitSyncSettingsService;

  @Inject
  public HarnessToGitHelperServiceImpl(@Named("connectorDecoratorService") ConnectorService connectorService,
      DecryptGitApiAccessHelper decryptScmApiAccess, GitEntityService gitEntityService,
      YamlGitConfigService yamlGitConfigService, EntityDetailProtoToRestMapper entityDetailRestToProtoMapper,
      GitToHarnessProcessorService gitToHarnessProcessorService, ExecutorService executorService,
      GitBranchService gitBranchService, EncryptionHelper encryptionHelper,
      SourceCodeManagerService sourceCodeManagerService, GitSyncSettingsService gitSyncSettingsService) {
    this.connectorService = connectorService;
    this.decryptScmApiAccess = decryptScmApiAccess;
    this.gitEntityService = gitEntityService;
    this.yamlGitConfigService = yamlGitConfigService;
    this.entityDetailRestToProtoMapper = entityDetailRestToProtoMapper;
    this.gitToHarnessProcessorService = gitToHarnessProcessorService;
    this.executorService = executorService;
    this.gitBranchService = gitBranchService;
    this.encryptionHelper = encryptionHelper;
    this.sourceCodeManagerService = sourceCodeManagerService;
    this.gitSyncSettingsService = gitSyncSettingsService;
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
    final Optional<GitSyncSettingsDTO> gitSyncSettingsDTO = gitSyncSettingsService.get(
        accountId, entityReference.getOrgIdentifier(), entityReference.getProjectIdentifier());
    final boolean executeOnDelegate = gitSyncSettingsDTO.isPresent() && gitSyncSettingsDTO.get().isExecuteOnDelegate();
    // todo(abhinav): Throw exception if optional not present.

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
      throw new InvalidRequestException("Connector doesn't exist.");
    }
    final ConnectorResponseDTO connector = connectorResponseDTO.get();
    setConnectorDetailsFromUserProfile(yamlGitConfig, userPrincipal, connector);
    return Optional.of(connector);
  }

  private void setConnectorDetailsFromUserProfile(
      YamlGitConfigDTO yamlGitConfig, UserPrincipal userPrincipal, ConnectorResponseDTO connector) {
    if (connector.getConnector().getConnectorType() != ConnectorType.GITHUB) {
      throw new InvalidRequestException("Git Sync only supported for github connector");
    }
    final GithubConnectorDTO githubConnectorDTO = (GithubConnectorDTO) connector.getConnector().getConnectorConfig();
    githubConnectorDTO.setUrl(yamlGitConfig.getRepo());
    final List<SourceCodeManagerDTO> sourceCodeManager =
        sourceCodeManagerService.get(userPrincipal.getUserId().getValue());
    final Optional<SourceCodeManagerDTO> sourceCodeManagerDTO =
        sourceCodeManager.stream().filter(scm -> scm.getType().equals(SCMType.GITHUB)).findFirst();
    if (!sourceCodeManagerDTO.isPresent()) {
      throw new InvalidRequestException("User profile doesn't contain github scm details");
    }
    final GithubSCMDTO githubUserProfile = (GithubSCMDTO) sourceCodeManagerDTO.get();
    final SecretRefData tokenRef;
    try {
      tokenRef =
          ((GithubUsernameTokenDTO) ((GithubHttpCredentialsDTO) githubUserProfile.getAuthentication().getCredentials())
                  .getHttpCredentialsSpec())
              .getTokenRef();
    } catch (Exception e) {
      throw new InvalidRequestException(
          "User Profile should contain github username token credentials for git sync", e);
    }
    githubConnectorDTO.setApiAccess(GithubApiAccessDTO.builder()
                                        .type(GithubApiAccessType.TOKEN)
                                        .spec(GithubTokenSpecDTO.builder().tokenRef(tokenRef).build())
                                        .build());
  }

  @Override
  public void postPushOperation(PushInfo pushInfo) {
    final EntityDetail entityDetailDTO =
        entityDetailRestToProtoMapper.createEntityDetailDTO(pushInfo.getEntityDetail());
    final EntityReference entityRef = entityDetailDTO.getEntityRef();
    final EntityDetailProtoDTO entityDetail = pushInfo.getEntityDetail();
    final YamlGitConfigDTO yamlGitConfigDTO = yamlGitConfigService.get(entityRef.getProjectIdentifier(),
        entityRef.getOrgIdentifier(), entityRef.getAccountIdentifier(), pushInfo.getYamlGitConfigId());
    gitEntityService.save(pushInfo.getAccountId(), entityDetailRestToProtoMapper.createEntityDetailDTO(entityDetail),
        yamlGitConfigDTO, pushInfo.getFolderPath(), pushInfo.getFilePath(), pushInfo.getCommitId(),
        pushInfo.getBranchName());
    shortListTheBranch(
        yamlGitConfigDTO, entityRef.getAccountIdentifier(), pushInfo.getBranchName(), pushInfo.getIsNewBranch());
    if (pushInfo.getIsNewBranch()) {
      executorService.submit(
          ()
              -> syncNewlyCreatedBranch(entityRef.getAccountIdentifier(), yamlGitConfigDTO.getIdentifier(),
                  yamlGitConfigDTO.getProjectIdentifier(), yamlGitConfigDTO.getOrganizationIdentifier(),
                  pushInfo.getBranchName(), createTheFilePath(pushInfo.getFilePath(), pushInfo.getFolderPath()),
                  yamlGitConfigDTO.getRepo()));
    }
    // todo(abhinav): record git commit and git file activity.
  }

  private String createTheFilePath(String filePath, String folderPath) {
    return ScmGitUtils.createFilePath(folderPath, filePath);
  }

  private void shortListTheBranch(
      YamlGitConfigDTO yamlGitConfigDTO, String accountIdentifier, String branchName, boolean isNewBranch) {
    GitBranch gitBranch = gitBranchService.get(accountIdentifier, yamlGitConfigDTO.getRepo(), branchName);
    BranchSyncStatus branchSyncStatus = isNewBranch ? SYNCING : SYNCED;
    if (gitBranch == null) {
      createGitBranch(yamlGitConfigDTO, accountIdentifier, branchName, branchSyncStatus);
      return;
    }
    if (gitBranch.getBranchSyncStatus() == UNSYNCED) {
      gitBranchService.updateBranchSyncStatus(
          accountIdentifier, yamlGitConfigDTO.getRepo(), branchName, branchSyncStatus);
    }
  }

  @Override
  public Boolean isGitSyncEnabled(EntityScopeInfo entityScopeInfo) {
    return yamlGitConfigService.isGitSyncEnabled(entityScopeInfo.getAccountId(), entityScopeInfo.getOrgId().getValue(),
        entityScopeInfo.getProjectId().getValue());
  }

  private void syncNewlyCreatedBranch(String accountId, String gitSyncConfigId, String projectIdentifier,
      String orgIdentifier, String branch, String filePathToBeExcluded, String repoURL) {
    processFilesInBranch(
        accountId, gitSyncConfigId, projectIdentifier, orgIdentifier, branch, filePathToBeExcluded, repoURL);
  }

  @Override
  public void processFilesInBranch(String accountId, String gitSyncConfigId, String projectIdentifier,
      String orgIdentifier, String branch, String filePathToBeExcluded, String repoURL) {
    final GitEntityInfo emptyRepoBranch =
        GitEntityInfo.builder().branch(null).yamlGitConfigId(null).findDefaultFromOtherBranches(true).build();
    try (GlobalContextManager.GlobalContextGuard guard = GlobalContextManager.ensureGlobalContextGuard()) {
      GlobalContextManager.upsertGlobalContextRecord(
          GitSyncBranchContext.builder().gitBranchInfo(emptyRepoBranch).build());
      final YamlGitConfigDTO yamlGitConfigDTO =
          yamlGitConfigService.get(projectIdentifier, orgIdentifier, accountId, gitSyncConfigId);
      gitToHarnessProcessorService.readFilesFromBranchAndProcess(
          yamlGitConfigDTO, branch, accountId, yamlGitConfigDTO.getBranch(), filePathToBeExcluded);
      log.info("Branch sync completed {}", branch);
      gitBranchService.updateBranchSyncStatus(accountId, repoURL, branch, SYNCED);
      log.info("Branch sync status updated completed {}", branch);
    }
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
    if (yamlGitConfigDTO != null) {
      return BranchDetails.newBuilder().build();
    }
    return BranchDetails.newBuilder().setDefaultBranch(yamlGitConfigDTO.getBranch()).build();
  }
}
