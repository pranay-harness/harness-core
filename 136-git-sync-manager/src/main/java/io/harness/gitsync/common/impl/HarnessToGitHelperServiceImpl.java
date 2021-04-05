package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.common.EntityReference;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.PushInfo;
import io.harness.gitsync.common.beans.InfoForGitPush;
import io.harness.gitsync.common.dtos.GitSyncEntityDTO;
import io.harness.gitsync.common.service.GitEntityService;
import io.harness.gitsync.common.service.HarnessToGitHelperService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.common.service.gittoharness.GitToHarnessProcessorService;
import io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper;
import io.harness.tasks.DecryptGitApiAccessHelper;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Optional;

@Singleton
@OwnedBy(DX)
public class HarnessToGitHelperServiceImpl implements HarnessToGitHelperService {
  private final ConnectorService connectorService;
  private final DecryptGitApiAccessHelper decryptScmApiAccess;
  private final GitEntityService gitEntityService;
  private final YamlGitConfigService yamlGitConfigService;
  private final EntityDetailProtoToRestMapper entityDetailRestToProtoMapper;
  private final GitToHarnessProcessorService gitToHarnessProcessorService;

  @Inject
  public HarnessToGitHelperServiceImpl(@Named("connectorDecoratorService") ConnectorService connectorService,
      DecryptGitApiAccessHelper decryptScmApiAccess, GitEntityService gitEntityService,
      YamlGitConfigService yamlGitConfigService, EntityDetailProtoToRestMapper entityDetailRestToProtoMapper,
      GitToHarnessProcessorService gitToHarnessProcessorService) {
    this.connectorService = connectorService;
    this.decryptScmApiAccess = decryptScmApiAccess;
    this.gitEntityService = gitEntityService;
    this.yamlGitConfigService = yamlGitConfigService;
    this.entityDetailRestToProtoMapper = entityDetailRestToProtoMapper;
    this.gitToHarnessProcessorService = gitToHarnessProcessorService;
  }

  @Override
  public InfoForGitPush getInfoForPush(String yamlGitConfigId, String branch, String filePath, String accountId,
      EntityReference entityReference, EntityType entityType) {
    final YamlGitConfigDTO yamlGitConfig = yamlGitConfigService.get(yamlGitConfigId, accountId);
    final GitSyncEntityDTO gitSyncEntityDTO = gitEntityService.get(entityReference, entityType);
    if (gitSyncEntityDTO != null) {
      if (filePath != null) {
        if (!gitSyncEntityDTO.getFilePath().equals(filePath)) {
          throw new InvalidRequestException("Incorrect file path");
        } else {
          return InfoForGitPush.builder()
              .scmConnector(getDecryptedScmConnector(accountId, yamlGitConfig))
              .filePath(filePath)
              .build();
        }
      }
    }
    return InfoForGitPush.builder()
        .scmConnector(getDecryptedScmConnector(accountId, yamlGitConfig))
        .filePath(filePath)
        .build();
  }

  private ScmConnector getDecryptedScmConnector(String accountId, YamlGitConfigDTO yamlGitConfig) {
    final String gitConnectorId = yamlGitConfig.getGitConnectorRef();
    final String identifier = IdentifierRefHelper.getIdentifier(gitConnectorId);
    final Optional<ConnectorResponseDTO> connectorResponseDTO = connectorService.get(
        accountId, yamlGitConfig.getOrganizationIdentifier(), yamlGitConfig.getProjectIdentifier(), identifier);
    return connectorResponseDTO
        .map(connector
            -> decryptScmApiAccess.decryptScmApiAccess((ScmConnector) connector.getConnector().getConnectorConfig(),
                accountId, yamlGitConfig.getProjectIdentifier(), yamlGitConfig.getOrganizationIdentifier()))
        .orElseThrow(() -> new InvalidRequestException("Connector doesn't exist."));
  }

  @Override
  public void postPushOperation(PushInfo pushInfo) {
    final YamlGitConfigDTO yamlGitConfigDTO =
        yamlGitConfigService.get(pushInfo.getYamlGitConfigId(), pushInfo.getAccountId());
    gitEntityService.save(pushInfo.getAccountId(),
        entityDetailRestToProtoMapper.createEntityDetailDTO(pushInfo.getEntityDetail()), yamlGitConfigDTO,
        pushInfo.getFilePath(), pushInfo.getCommitId());
    // todo(abhinav): record git commit and git file activity.
  }

  @Override
  public Boolean isGitSyncEnabled(EntityScopeInfo entityScopeInfo) {
    return yamlGitConfigService.isGitSyncEnabled(entityScopeInfo.getAccountId(), entityScopeInfo.getOrgId().getValue(),
        entityScopeInfo.getProjectId().getValue());
  }

  @Override
  public void onBranchCreationReadFilesAndProcessThem(String accountId, String gitSyncConfigId, String branch) {
    final YamlGitConfigDTO yamlGitConfigDTO = yamlGitConfigService.get(gitSyncConfigId, accountId);
    gitToHarnessProcessorService.readFilesFromBranchAndProcess(yamlGitConfigDTO, branch, accountId);
  }
}
