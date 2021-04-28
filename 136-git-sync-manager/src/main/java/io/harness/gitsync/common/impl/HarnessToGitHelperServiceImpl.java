package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.common.beans.BranchSyncStatus.SYNCED;
import static io.harness.gitsync.common.beans.BranchSyncStatus.SYNCING;

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
import io.harness.gitsync.PushInfo;
import io.harness.gitsync.common.beans.InfoForGitPush;
import io.harness.gitsync.common.beans.InfoForGitPush.InfoForGitPushBuilder;
import io.harness.gitsync.common.dtos.GitSyncEntityDTO;
import io.harness.gitsync.common.service.GitBranchService;
import io.harness.gitsync.common.service.GitEntityService;
import io.harness.gitsync.common.service.HarnessToGitHelperService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.common.service.gittoharness.GitToHarnessProcessorService;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.tasks.DecryptGitApiAccessHelper;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import org.apache.commons.lang3.tuple.Pair;

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

  @Inject
  public HarnessToGitHelperServiceImpl(@Named("connectorDecoratorService") ConnectorService connectorService,
      DecryptGitApiAccessHelper decryptScmApiAccess, GitEntityService gitEntityService,
      YamlGitConfigService yamlGitConfigService, EntityDetailProtoToRestMapper entityDetailRestToProtoMapper,
      GitToHarnessProcessorService gitToHarnessProcessorService, ExecutorService executorService,
      GitBranchService gitBranchService, EncryptionHelper encryptionHelper) {
    this.connectorService = connectorService;
    this.decryptScmApiAccess = decryptScmApiAccess;
    this.gitEntityService = gitEntityService;
    this.yamlGitConfigService = yamlGitConfigService;
    this.entityDetailRestToProtoMapper = entityDetailRestToProtoMapper;
    this.gitToHarnessProcessorService = gitToHarnessProcessorService;
    this.executorService = executorService;
    this.gitBranchService = gitBranchService;
    this.encryptionHelper = encryptionHelper;
  }

  @Override
  public InfoForGitPush getInfoForPush(String yamlGitConfigId, String branch, String filePath, String accountId,
      EntityReference entityReference, EntityType entityType) {
    final InfoForGitPushBuilder infoForGitPushBuilder = InfoForGitPush.builder();
    final YamlGitConfigDTO yamlGitConfig = yamlGitConfigService.get(
        entityReference.getProjectIdentifier(), entityReference.getOrgIdentifier(), accountId, yamlGitConfigId);
    final GitSyncEntityDTO gitSyncEntityDTO = gitEntityService.get(entityReference, entityType);
    if (gitSyncEntityDTO != null) {
      if (filePath != null) {
        if (!gitSyncEntityDTO.getEntityGitPath().equals(filePath)) {
          throw new InvalidRequestException("Incorrect file path");
        }
      }
    }
    if (yamlGitConfig.isExecuteOnDelegate()) {
      final Pair<ScmConnector, List<EncryptedDataDetail>> connectorWithEncryptionDetails =
          getConnectorWithEncryptionDetails(accountId, yamlGitConfig);
      infoForGitPushBuilder.encryptedDataDetailList(connectorWithEncryptionDetails.getRight())
          .scmConnector(connectorWithEncryptionDetails.getLeft());
    } else {
      infoForGitPushBuilder.scmConnector(getDecryptedScmConnector(accountId, yamlGitConfig));
    }
    return infoForGitPushBuilder.filePath(filePath)
        .branch(branch)
        .isDefault(branch.equals(yamlGitConfig.getBranch()))
        .yamlGitConfigId(yamlGitConfig.getIdentifier())
        .accountId(accountId)
        .orgIdentifier(entityReference.getOrgIdentifier())
        .projectIdentifier(entityReference.getProjectIdentifier())
        .defaultBranchName(yamlGitConfig.getBranch())
        .build();
  }

  private Pair<ScmConnector, List<EncryptedDataDetail>> getConnectorWithEncryptionDetails(
      String accountId, YamlGitConfigDTO yamlGitConfig) {
    final Optional<ConnectorResponseDTO> connectorResponseDTO = getConnector(accountId, yamlGitConfig);
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

  private ScmConnector getDecryptedScmConnector(String accountId, YamlGitConfigDTO yamlGitConfig) {
    final Optional<ConnectorResponseDTO> connectorResponseDTO = getConnector(accountId, yamlGitConfig);
    return connectorResponseDTO
        .map(connector
            -> decryptScmApiAccess.decryptScmApiAccess((ScmConnector) connector.getConnector().getConnectorConfig(),
                accountId, yamlGitConfig.getProjectIdentifier(), yamlGitConfig.getOrganizationIdentifier()))
        .orElseThrow(() -> new InvalidRequestException("Connector doesn't exist."));
  }

  private Optional<ConnectorResponseDTO> getConnector(String accountId, YamlGitConfigDTO yamlGitConfig) {
    final String gitConnectorId = yamlGitConfig.getGitConnectorRef();
    final IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(gitConnectorId, accountId,
        yamlGitConfig.getOrganizationIdentifier(), yamlGitConfig.getProjectIdentifier(), null);
    return connectorService.get(accountId, identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(),
        identifierRef.getIdentifier());
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
    if (pushInfo.getIsNewBranch()) {
      executorService.submit(
          ()
              -> processFilesInBranch(entityRef.getAccountIdentifier(), yamlGitConfigDTO.getIdentifier(),
                  yamlGitConfigDTO.getProjectIdentifier(), yamlGitConfigDTO.getOrganizationIdentifier(),
                  pushInfo.getBranchName(), pushInfo.getFilePath(), yamlGitConfigDTO.getRepo()));
    }
    // todo(abhinav): record git commit and git file activity.
  }

  @Override
  public Boolean isGitSyncEnabled(EntityScopeInfo entityScopeInfo) {
    return yamlGitConfigService.isGitSyncEnabled(entityScopeInfo.getAccountId(), entityScopeInfo.getOrgId().getValue(),
        entityScopeInfo.getProjectId().getValue());
  }

  @Override
  public void processFilesInBranch(String accountId, String gitSyncConfigId, String projectIdentifier,
      String orgIdentifier, String branch, String filePathToBeExcluded, String repoURL) {
    gitBranchService.updateBranchSyncStatus(accountId, repoURL, branch, SYNCING);
    final YamlGitConfigDTO yamlGitConfigDTO =
        yamlGitConfigService.get(projectIdentifier, orgIdentifier, accountId, gitSyncConfigId);
    gitToHarnessProcessorService.readFilesFromBranchAndProcess(
        yamlGitConfigDTO, branch, accountId, yamlGitConfigDTO.getBranch(), filePathToBeExcluded);
    gitBranchService.updateBranchSyncStatus(accountId, repoURL, branch, SYNCED);
  }
}
