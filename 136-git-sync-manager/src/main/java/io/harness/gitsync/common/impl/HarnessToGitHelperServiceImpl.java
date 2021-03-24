package io.harness.gitsync.common.impl;

import io.harness.EntityType;
import io.harness.common.EntityReference;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.PushInfo;
import io.harness.gitsync.common.beans.InfoForPush;
import io.harness.gitsync.common.dtos.GitSyncEntityDTO;
import io.harness.gitsync.common.service.GitEntityService;
import io.harness.gitsync.common.service.HarnessToGitHelperService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper;
import io.harness.tasks.DecryptGitApiAccessHelper;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Optional;

@Singleton
public class HarnessToGitHelperServiceImpl implements HarnessToGitHelperService {
  private final ConnectorService connectorService;
  private final DecryptGitApiAccessHelper decryptScmApiAccess;
  private final GitEntityService gitEntityService;
  private final YamlGitConfigService yamlGitConfigService;
  private final EntityDetailProtoToRestMapper entityDetailRestToProtoMapper;

  @Inject
  public HarnessToGitHelperServiceImpl(@Named("connectorDecoratorService") ConnectorService connectorService,
      DecryptGitApiAccessHelper decryptScmApiAccess, GitEntityService gitEntityService,
      YamlGitConfigService yamlGitConfigService, EntityDetailProtoToRestMapper entityDetailRestToProtoMapper) {
    this.connectorService = connectorService;
    this.decryptScmApiAccess = decryptScmApiAccess;
    this.gitEntityService = gitEntityService;
    this.yamlGitConfigService = yamlGitConfigService;
    this.entityDetailRestToProtoMapper = entityDetailRestToProtoMapper;
  }

  @Override
  public InfoForPush getInfoForPush(String yamlGitConfigId, String branch, String filePath, String accountId,
      EntityReference entityReference, EntityType entityType) {
    final YamlGitConfigDTO yamlGitConfig = yamlGitConfigService.get(yamlGitConfigId, accountId);
    final GitSyncEntityDTO gitSyncEntityDTO = gitEntityService.get(entityReference, entityType);
    if (gitSyncEntityDTO != null) {
      if (filePath != null) {
        if (!gitSyncEntityDTO.getFilePath().equals(filePath)) {
          throw new InvalidRequestException("Incorrect file path");
        } else {
          return InfoForPush.builder()
              .scmConnector(getDecryptedScmConnector(accountId, yamlGitConfig))
              .filePath(filePath)
              .build();
        }
      }
    }
    return InfoForPush.builder()
        .scmConnector(getDecryptedScmConnector(accountId, yamlGitConfig))
        .filePath(filePath)
        .build();
  }

  private ScmConnector getDecryptedScmConnector(String accountId, YamlGitConfigDTO yamlGitConfig) {
    final String gitConnectorId = yamlGitConfig.getGitConnectorId();
    final String identifier = IdentifierRefHelper.getIdentifier(gitConnectorId);
    final Optional<ConnectorResponseDTO> connectorResponseDTO =
        connectorService.get(accountId, yamlGitConfig.getOrganizationId(), yamlGitConfig.getProjectId(), identifier);
    return connectorResponseDTO
        .map(connector
            -> decryptScmApiAccess.decryptScmApiAccess((ScmConnector) connector.getConnector().getConnectorConfig(),
                accountId, yamlGitConfig.getProjectId(), yamlGitConfig.getOrganizationId()))
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
}
