package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.HarnessStringUtils.nullIfEmpty;
import static io.harness.encryption.ScopeHelper.getScope;
import static io.harness.gitsync.common.YamlConstants.HARNESS_FOLDER_EXTENSION;
import static io.harness.gitsync.common.YamlConstants.PATH_DELIMITER;
import static io.harness.gitsync.common.remote.YamlGitConfigMapper.toYamlGitConfig;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.encryption.Scope;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.common.beans.YamlGitConfig;
import io.harness.gitsync.common.remote.YamlGitConfigMapper;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.repositories.repositories.yamlGitConfig.YamlGitConfigRepository;
import io.harness.tasks.DecryptGitApiAccessHelper;

import software.wings.utils.CryptoUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.StringValue;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(DX)
public class YamlGitConfigServiceImpl implements YamlGitConfigService {
  private final YamlGitConfigRepository yamlGitConfigRepository;
  private final ConnectorService connectorService;
  private final DecryptGitApiAccessHelper decryptScmApiAccess;
  private final Producer gitSyncConfigEventProducer;

  @Inject
  public YamlGitConfigServiceImpl(YamlGitConfigRepository yamlGitConfigRepository,
      @Named("connectorDecoratorService") ConnectorService connectorService,
      DecryptGitApiAccessHelper decryptScmApiAccess,
      @Named(EventsFrameworkConstants.GIT_CONFIG_STREAM) Producer gitSyncConfigEventProducer) {
    this.yamlGitConfigRepository = yamlGitConfigRepository;
    this.connectorService = connectorService;
    this.decryptScmApiAccess = decryptScmApiAccess;
    this.gitSyncConfigEventProducer = gitSyncConfigEventProducer;
  }

  @Override
  public YamlGitConfigDTO get(String projectIdentifier, String orgIdentifier, String accountId, String identifier) {
    Optional<YamlGitConfig> yamlGitConfig =
        yamlGitConfigRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountId, orgIdentifier, projectIdentifier, identifier);
    return yamlGitConfig.map(YamlGitConfigMapper::toYamlGitConfigDTO)
        .orElseThrow(()
                         -> new InvalidRequestException(
                             getYamlGitConfigNotFoundMessage(accountId, orgIdentifier, projectIdentifier, identifier)));
  }

  @Override
  public YamlGitConfigDTO getByFolderIdentifierAndIsEnabled(
      String projectIdentifier, String orgIdentifier, String accountId, String folderId) {
    // todo @deepak Implement this method when required
    return null;
  }

  private Optional<YamlGitConfig> getYamlGitConfigEntity(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    return yamlGitConfigRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifier(
        accountId, orgIdentifier, projectIdentifier, identifier);
  }

  @Override
  public List<YamlGitConfigDTO> list(String projectIdentifier, String orgIdentifier, String accountId) {
    Scope scope = getScope(accountId, orgIdentifier, projectIdentifier);
    List<YamlGitConfig> yamlGitConfigs =
        yamlGitConfigRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndScopeOrderByCreatedAtDesc(
            accountId, orgIdentifier, projectIdentifier, scope);
    return emptyIfNull(yamlGitConfigs)
        .stream()
        .map(YamlGitConfigMapper::toYamlGitConfigDTO)
        .collect(Collectors.toList());
  }

  private String findDefaultIfPresent(YamlGitConfigDTO yamlGitConfigDTO) {
    if (yamlGitConfigDTO.getDefaultRootFolder() != null) {
      return yamlGitConfigDTO.getDefaultRootFolder().getIdentifier();
    }
    return null;
  }

  @Override
  public YamlGitConfigDTO updateDefault(
      String projectIdentifier, String orgIdentifier, String accountId, String identifier, String folderIdentifier) {
    Optional<YamlGitConfig> yamlGitConfigOptional =
        getYamlGitConfigEntity(accountId, orgIdentifier, projectIdentifier, identifier);
    if (!yamlGitConfigOptional.isPresent()) {
      throw new InvalidRequestException(
          getYamlGitConfigNotFoundMessage(accountId, orgIdentifier, projectIdentifier, identifier));
    }
    YamlGitConfig yamlGitConfig = yamlGitConfigOptional.get();
    List<YamlGitConfigDTO.RootFolder> rootFolders = yamlGitConfig.getRootFolders();
    YamlGitConfigDTO.RootFolder newDefaultRootFolder = null;
    for (YamlGitConfigDTO.RootFolder folder : rootFolders) {
      if (folder.getIdentifier().equals(folderIdentifier)) {
        newDefaultRootFolder = folder;
      }
    }
    if (newDefaultRootFolder == null) {
      throw new InvalidRequestException("No folder exists with the identifier " + folderIdentifier);
    }
    yamlGitConfig.setDefaultRootFolder(newDefaultRootFolder);
    YamlGitConfig updatedYamlGitConfig = yamlGitConfigRepository.save(yamlGitConfig);
    return YamlGitConfigMapper.toYamlGitConfigDTO(updatedYamlGitConfig);
  }

  @Override
  public List<YamlGitConfigDTO> getByConnectorRepoAndBranch(
      String gitConnectorId, String repo, String branchName, String accountId) {
    List<YamlGitConfig> yamlGitConfigs = yamlGitConfigRepository.findByGitConnectorRefAndRepoAndBranchAndAccountId(
        gitConnectorId, repo, branchName, accountId);
    return emptyIfNull(yamlGitConfigs)
        .stream()
        .map(YamlGitConfigMapper::toYamlGitConfigDTO)
        .collect(Collectors.toList());
  }

  @Override
  public YamlGitConfigDTO save(YamlGitConfigDTO ygs) {
    return save(ygs, ygs.getAccountIdentifier(), true);
  }

  void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  @Override
  public YamlGitConfigDTO update(YamlGitConfigDTO gitSyncConfig) {
    validatePresenceOfRequiredFields(gitSyncConfig.getAccountIdentifier(), gitSyncConfig.getIdentifier());
    return updateInternal(gitSyncConfig);
  }

  private YamlGitConfigDTO updateInternal(YamlGitConfigDTO gitSyncConfigDTO) {
    validateTheGitConfigInput(gitSyncConfigDTO);
    Optional<YamlGitConfig> existingYamlGitConfigDTO =
        yamlGitConfigRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            gitSyncConfigDTO.getAccountIdentifier(), gitSyncConfigDTO.getOrganizationIdentifier(),
            gitSyncConfigDTO.getProjectIdentifier(), gitSyncConfigDTO.getIdentifier());
    if (!existingYamlGitConfigDTO.isPresent()) {
      throw new InvalidRequestException(getYamlGitConfigNotFoundMessage(gitSyncConfigDTO.getAccountIdentifier(),
          gitSyncConfigDTO.getOrganizationIdentifier(), gitSyncConfigDTO.getProjectIdentifier(),
          gitSyncConfigDTO.getIdentifier()));
    }
    YamlGitConfig yamlGitConfigToBeSaved = toYamlGitConfig(gitSyncConfigDTO, gitSyncConfigDTO.getAccountIdentifier());
    yamlGitConfigToBeSaved.setWebhookToken(existingYamlGitConfigDTO.get().getWebhookToken());
    yamlGitConfigToBeSaved.setUuid(existingYamlGitConfigDTO.get().getUuid());
    yamlGitConfigToBeSaved.setVersion(existingYamlGitConfigDTO.get().getVersion());
    YamlGitConfig yamlGitConfig = yamlGitConfigRepository.save(yamlGitConfigToBeSaved);
    return YamlGitConfigMapper.toYamlGitConfigDTO(yamlGitConfig);
  }

  private String getYamlGitConfigNotFoundMessage(
      String accountId, String organizationId, String projectId, String identifier) {
    return String.format("No yaml git config exists with the id %s, in account %s, org %s, project %s", identifier,
        accountId, organizationId, projectId);
  }

  @Override
  public Optional<ConnectorInfoDTO> getGitConnector(
      YamlGitConfigDTO ygs, String gitConnectorId, String repoName, String branchName) {
    Optional<ConnectorResponseDTO> connectorDTO = connectorService.get(
        ygs.getAccountIdentifier(), ygs.getOrganizationIdentifier(), ygs.getProjectIdentifier(), gitConnectorId);

    if (connectorDTO.isPresent()) {
      ConnectorInfoDTO connectorInfo = connectorDTO.get().getConnector();
      if (connectorInfo.getConnectorType() == ConnectorType.GIT) {
        return connectorDTO.map(connectorResponse -> connectorResponse.getConnector());
      }
    }
    return Optional.empty();
  }

  public YamlGitConfigDTO save(YamlGitConfigDTO ygs, String accountId, boolean performFullSync) {
    // TODO(abhinav): add full sync logic.
    return saveInternal(ygs, accountId);
  }

  private YamlGitConfigDTO saveInternal(YamlGitConfigDTO gitSyncConfigDTO, String accountId) {
    validateTheGitConfigInput(gitSyncConfigDTO);
    YamlGitConfig yamlGitConfigToBeSaved = toYamlGitConfig(gitSyncConfigDTO, accountId);
    yamlGitConfigToBeSaved.setWebhookToken(CryptoUtils.secureRandAlphaNumString(40));
    YamlGitConfig savedYamlGitConfig = null;
    try {
      savedYamlGitConfig = yamlGitConfigRepository.save(yamlGitConfigToBeSaved);
    } catch (Exception ex) {
      throw new InvalidRequestException(String.format("A git sync config with the repo %s and branch %s already exists",
          gitSyncConfigDTO.getRepo(), gitSyncConfigDTO.getBranch()));
    }
    sendEventForConfigChange(accountId, yamlGitConfigToBeSaved.getOrgIdentifier(),
        yamlGitConfigToBeSaved.getProjectIdentifier(), yamlGitConfigToBeSaved.getIdentifier(), "Save");
    return YamlGitConfigMapper.toYamlGitConfigDTO(savedYamlGitConfig);
  }

  private void sendEventForConfigChange(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier, String eventType) {
    EntityScopeInfo entityScopeInfo = EntityScopeInfo.newBuilder()
                                          .setOrgId(StringValue.newBuilder().setValue(orgIdentifier).build())
                                          .setProjectId(StringValue.newBuilder().setValue(projectIdentifier).build())
                                          .setAccountId(accountId)
                                          .build();
    try {
      gitSyncConfigEventProducer.send(Message.newBuilder()
                                          .putAllMetadata(ImmutableMap.of("accountId", accountId))
                                          .setData(entityScopeInfo.toByteString())
                                          .build());
    } catch (Exception e) {
      log.error("Event to send git config update failed for accountId {} during {} event for yamlgitconfig [{}]",
          accountId, eventType, identifier, e);
    }
  }

  private void validateTheGitConfigInput(YamlGitConfigDTO ygs) {
    ensureFolderEndsWithDelimiter(ygs);
    validateFolderFollowsHarnessParadigm(ygs);
  }

  private void ensureFolderEndsWithDelimiter(YamlGitConfigDTO ygs) {
    if (ygs.getRootFolders() == null) {
      return;
    }
    ygs.getRootFolders().forEach(folder -> {
      if (!folder.getRootFolder().endsWith(PATH_DELIMITER)) {
        folder.getRootFolder().concat(PATH_DELIMITER);
      }
    });
  }

  private void validateFolderFollowsHarnessParadigm(YamlGitConfigDTO ygs) {
    if (ygs.getRootFolders() == null) {
      return;
    }
    final Optional<YamlGitConfigDTO.RootFolder> rootFolder =
        ygs.getRootFolders()
            .stream()
            .filter(
                config -> config.getRootFolder().endsWith(PATH_DELIMITER + HARNESS_FOLDER_EXTENSION + PATH_DELIMITER))
            .findFirst();
    if (rootFolder.isPresent()) {
      throw new InvalidRequestException("Incorrect root folder configuration.");
    }
  }

  @Override
  public boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    Scope scope = getScope(accountId, orgIdentifier, projectIdentifier);
    boolean deleted =
        yamlGitConfigRepository.deleteByAccountIdAndOrgIdentifierAndProjectIdentifierAndScopeAndIdentifier(
            accountId, orgIdentifier, projectIdentifier, scope, identifier)
        != 0;
    if (deleted) {
      sendEventForConfigChange(accountId, orgIdentifier, projectIdentifier, identifier, "delete");
    }
    return deleted;
  }

  @Override
  public Boolean isGitSyncEnabled(String accountIdentifier, String organizationIdentifier, String projectIdentifier) {
    return yamlGitConfigRepository.existsByAccountIdAndOrgIdentifierAndProjectIdentifier(
        accountIdentifier, nullIfEmpty(organizationIdentifier), nullIfEmpty(projectIdentifier));
  }
}
