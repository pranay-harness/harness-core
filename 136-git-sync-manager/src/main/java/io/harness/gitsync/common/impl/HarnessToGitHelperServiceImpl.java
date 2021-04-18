package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.common.EntityReference;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.PushInfo;
import io.harness.gitsync.common.beans.InfoForGitPush;
import io.harness.gitsync.common.beans.InfoForGitPush.InfoForGitPushBuilder;
import io.harness.gitsync.common.dtos.GitSyncEntityDTO;
import io.harness.gitsync.common.service.DecryptedScmKeySource;
import io.harness.gitsync.common.service.GitEntityService;
import io.harness.gitsync.common.service.HarnessToGitHelperService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.common.service.gittoharness.GitToHarnessProcessorService;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(DX)
public class HarnessToGitHelperServiceImpl implements HarnessToGitHelperService {
  private final GitEntityService gitEntityService;
  private final YamlGitConfigService yamlGitConfigService;
  private final EntityDetailProtoToRestMapper entityDetailRestToProtoMapper;
  private final GitToHarnessProcessorService gitToHarnessProcessorService;
  private final DecryptedScmKeySource decryptedScmKeySource;

  @Inject
  public HarnessToGitHelperServiceImpl(GitEntityService gitEntityService, YamlGitConfigService yamlGitConfigService,
      EntityDetailProtoToRestMapper entityDetailRestToProtoMapper,
      GitToHarnessProcessorService gitToHarnessProcessorService, DecryptedScmKeySource decryptedScmKeySource) {
    this.gitEntityService = gitEntityService;
    this.yamlGitConfigService = yamlGitConfigService;
    this.entityDetailRestToProtoMapper = entityDetailRestToProtoMapper;
    this.gitToHarnessProcessorService = gitToHarnessProcessorService;
    this.decryptedScmKeySource = decryptedScmKeySource;
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
    return infoForGitPushBuilder.scmConnector(getDecryptedScmConnector(accountId, yamlGitConfig))
        .filePath(filePath)
        .branch(branch)
        .isDefault(branch.equals(yamlGitConfig.getBranch()))
        .yamlGitConfigId(yamlGitConfig.getIdentifier())
        .accountId(accountId)
        .orgIdentifier(entityReference.getOrgIdentifier())
        .projectIdentifier(entityReference.getProjectIdentifier())
        .build();
  }

  private ScmConnector getDecryptedScmConnector(String accountId, YamlGitConfigDTO yamlGitConfig) {
    final String gitConnectorId = yamlGitConfig.getGitConnectorRef();
    final IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(gitConnectorId, accountId,
        yamlGitConfig.getOrganizationIdentifier(), yamlGitConfig.getProjectIdentifier(), null);
    return decryptedScmKeySource.fetchKey(identifierRef);
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
        yamlGitConfigDTO, pushInfo.getFilePath(), pushInfo.getCommitId());
    // todo(abhinav): record git commit and git file activity.
  }

  @Override
  public Boolean isGitSyncEnabled(EntityScopeInfo entityScopeInfo) {
    return yamlGitConfigService.isGitSyncEnabled(entityScopeInfo.getAccountId(), entityScopeInfo.getOrgId().getValue(),
        entityScopeInfo.getProjectId().getValue());
  }

  @Override
  public void onBranchCreationReadFilesAndProcessThem(
      String accountId, String gitSyncConfigId, String projectIdentifier, String orgIdentifier, String branch) {
    final YamlGitConfigDTO yamlGitConfigDTO =
        yamlGitConfigService.get(projectIdentifier, orgIdentifier, accountId, gitSyncConfigId);
    gitToHarnessProcessorService.readFilesFromBranchAndProcess(yamlGitConfigDTO, branch, accountId);
  }
}
