package io.harness.gitsync.common.impl;

import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.encryption.ScopeHelper.getScope;
import static io.harness.gitsync.common.YamlConstants.HARNESS_FOLDER_EXTENSION;
import static io.harness.gitsync.common.YamlConstants.PATH_DELIMITER;
import static io.harness.gitsync.common.remote.YamlGitConfigMapper.toYamlGitConfig;

import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.encryption.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.common.beans.YamlGitConfig;
import io.harness.gitsync.common.remote.YamlGitConfigMapper;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.repositories.repositories.yamlGitConfig.YamlGitConfigRepository;

import software.wings.utils.CryptoUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class YamlGitConfigServiceImpl implements YamlGitConfigService {
  private final YamlGitConfigRepository yamlGitConfigRepository;
  private final ConnectorService connectorService;

  @Inject
  public YamlGitConfigServiceImpl(YamlGitConfigRepository yamlGitConfigRepository,
      @Named(DEFAULT_CONNECTOR_SERVICE) ConnectorService connectorService) {
    this.yamlGitConfigRepository = yamlGitConfigRepository;
    this.connectorService = connectorService;
  }

  @Override
  public YamlGitConfigDTO get(String projectIdentifier, String orgIdentifier, String accountId, String identifier) {
    Optional<YamlGitConfig> yamlGitConfig = yamlGitConfigRepository.findByAccountIdAndOrganizationIdAndProjectIdAndUuid(
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
    return yamlGitConfigRepository.findByAccountIdAndOrganizationIdAndProjectIdAndUuid(
        accountId, orgIdentifier, projectIdentifier, identifier);
  }

  @Override
  public List<YamlGitConfigDTO> list(String projectIdentifier, String orgIdentifier, String accountId) {
    Scope scope = getScope(accountId, orgIdentifier, projectIdentifier);
    List<YamlGitConfig> yamlGitConfigs =
        yamlGitConfigRepository.findByAccountIdAndOrganizationIdAndProjectIdAndScopeOrderByCreatedAtAsc(
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
    List<YamlGitConfig> yamlGitConfigs = yamlGitConfigRepository.findByGitConnectorIdAndRepoAndBranchAndAccountId(
        gitConnectorId, repo, branchName, accountId);
    return emptyIfNull(yamlGitConfigs)
        .stream()
        .map(YamlGitConfigMapper::toYamlGitConfigDTO)
        .collect(Collectors.toList());
  }

  @Override
  public YamlGitConfigDTO save(YamlGitConfigDTO ygs) {
    return save(ygs, true);
  }

  void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  @Override
  public YamlGitConfigDTO update(YamlGitConfigDTO gitSyncConfig) {
    validatePresenceOfRequiredFields(gitSyncConfig.getAccountId(), gitSyncConfig.getIdentifier());
    return updateInternal(gitSyncConfig);
  }

  private YamlGitConfigDTO updateInternal(YamlGitConfigDTO gitSyncConfigDTO) {
    validateTheGitConfigInput(gitSyncConfigDTO);
    Optional<YamlGitConfig> existingYamlGitConfigDTO =
        yamlGitConfigRepository.findByAccountIdAndOrganizationIdAndProjectIdAndUuid(gitSyncConfigDTO.getAccountId(),
            gitSyncConfigDTO.getOrganizationId(), gitSyncConfigDTO.getProjectId(), gitSyncConfigDTO.getIdentifier());
    if (!existingYamlGitConfigDTO.isPresent()) {
      throw new InvalidRequestException(getYamlGitConfigNotFoundMessage(gitSyncConfigDTO.getAccountId(),
          gitSyncConfigDTO.getOrganizationId(), gitSyncConfigDTO.getProjectId(), gitSyncConfigDTO.getIdentifier()));
    }
    YamlGitConfig yamlGitConfigToBeSaved = toYamlGitConfig(gitSyncConfigDTO);
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
    Optional<ConnectorResponseDTO> connectorDTO =
        connectorService.get(ygs.getAccountId(), ygs.getOrganizationId(), ygs.getProjectId(), gitConnectorId);

    if (connectorDTO.isPresent()) {
      ConnectorInfoDTO connectorInfo = connectorDTO.get().getConnector();
      if (connectorInfo.getConnectorType() == ConnectorType.GIT) {
        return connectorDTO.map(connectorResponse -> connectorResponse.getConnector());
      }
    }
    return Optional.empty();
  }

  public YamlGitConfigDTO save(YamlGitConfigDTO ygs, boolean performFullSync) {
    // TODO(abhinav): add full sync logic.
    return saveInternal(ygs);
  }

  private YamlGitConfigDTO saveInternal(YamlGitConfigDTO gitSyncConfigDTO) {
    validateTheGitConfigInput(gitSyncConfigDTO);
    YamlGitConfig yamlGitConfigToBeSaved = toYamlGitConfig(gitSyncConfigDTO);
    yamlGitConfigToBeSaved.setWebhookToken(CryptoUtils.secureRandAlphaNumString(40));
    YamlGitConfig savedYamlGitConfig = null;
    try {
      savedYamlGitConfig = yamlGitConfigRepository.save(yamlGitConfigToBeSaved);
    } catch (Exception ex) {
      throw new InvalidRequestException(
          String.format("A git sync config with the id %s already exists", gitSyncConfigDTO.getIdentifier()));
    }
    return YamlGitConfigMapper.toYamlGitConfigDTO(savedYamlGitConfig);
  }

  private void validateTheGitConfigInput(YamlGitConfigDTO ygs) {
    ensureFolderEndsWithDelimiter(ygs);
    validateFolderFollowsHarnessParadigm(ygs);
  }

  private void ensureFolderEndsWithDelimiter(YamlGitConfigDTO ygs) {
    ygs.getRootFolders().forEach(folder -> {
      if (!folder.getRootFolder().endsWith(PATH_DELIMITER)) {
        folder.getRootFolder().concat(PATH_DELIMITER);
      }
    });
  }

  private void validateFolderFollowsHarnessParadigm(YamlGitConfigDTO ygs) {
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
    return yamlGitConfigRepository.removeByAccountIdAndOrganizationIdAndProjectIdAndScopeAndUuid(
               accountId, orgIdentifier, projectIdentifier, scope, identifier)
        != 0;
  }
}
