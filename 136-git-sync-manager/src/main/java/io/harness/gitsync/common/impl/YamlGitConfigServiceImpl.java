package io.harness.gitsync.common.impl;

import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.encryption.ScopeHelper.getScope;
import static io.harness.gitsync.common.remote.YamlGitConfigMapper.toYamlGitConfig;
import static io.harness.gitsync.common.remote.YamlGitConfigMapper.toYamlGitConfigDTOFromFolderConfigWithSameYamlGitConfigId;
import static io.harness.gitsync.common.remote.YamlGitConfigMapper.toYamlGitFolderConfig;

import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.connector.apis.dto.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.encryption.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.common.beans.YamlGitConfig;
import io.harness.gitsync.common.beans.YamlGitFolderConfig;
import io.harness.gitsync.common.dao.api.repositories.yamlGitConfig.YamlGitConfigRepository;
import io.harness.gitsync.common.dao.api.repositories.yamlGitFolderConfig.YamlGitFolderConfigRepository;
import io.harness.gitsync.common.helper.YamlGitConfigDTOComparator;
import io.harness.gitsync.common.remote.YamlGitConfigMapper;
import io.harness.gitsync.common.service.YamlGitConfigService;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Singleton
@Slf4j
public class YamlGitConfigServiceImpl implements YamlGitConfigService {
  private final YamlGitConfigRepository yamlGitConfigRepository;
  private final YamlGitFolderConfigRepository yamlGitConfigFolderRepository;
  private final ConnectorService connectorService;

  @Inject
  public YamlGitConfigServiceImpl(YamlGitConfigRepository yamlGitConfigRepository,
      YamlGitFolderConfigRepository yamlGitConfigFolderRepository,
      @Named(DEFAULT_CONNECTOR_SERVICE) ConnectorService connectorService) {
    this.yamlGitConfigRepository = yamlGitConfigRepository;
    this.yamlGitConfigFolderRepository = yamlGitConfigFolderRepository;
    this.connectorService = connectorService;
  }

  public YamlGitConfigDTO getByIdentifier(
      String projectId, String organizationId, String accountId, String identifier) {
    Scope scope = getScope(accountId, organizationId, projectId);
    final List<YamlGitFolderConfig> yamlGitFolderConfigs =
        yamlGitConfigFolderRepository
            .findByAccountIdAndOrganizationIdAndProjectIdAndScopeAndYamlGitConfigIdOrderByCreatedAtAsc(
                accountId, organizationId, projectId, scope, identifier);
    return toYamlGitConfigDTOFromFolderConfigWithSameYamlGitConfigId(yamlGitFolderConfigs);
  }

  @Override
  public List<YamlGitConfigDTO> get(String projectIdentifier, String orgIdentifier, String accountId) {
    Scope scope = getScope(accountId, orgIdentifier, projectIdentifier);
    List<YamlGitFolderConfig> yamlGitConfigs =
        yamlGitConfigFolderRepository.findByAccountIdAndOrganizationIdAndProjectIdAndScopeOrderByCreatedAtAsc(
            accountId, orgIdentifier, projectIdentifier, scope);
    return getYamlGitConfigDTOsFromYamlGitFolderConfig(yamlGitConfigs);
  }

  @Override
  public List<YamlGitConfigDTO> orderedGet(String projectIdentifier, String orgIdentifier, String accountId) {
    Scope scope = getScope(accountId, orgIdentifier, projectIdentifier);
    List<YamlGitFolderConfig> yamlGitFolderConfigs =
        yamlGitConfigFolderRepository.findByAccountIdAndOrganizationIdAndProjectIdAndScopeOrderByCreatedAtAsc(
            accountId, orgIdentifier, projectIdentifier, scope);
    List<YamlGitConfigDTO> yamlGitConfigDTO = getYamlGitConfigDTOsFromYamlGitFolderConfig(yamlGitFolderConfigs);
    List<YamlGitConfig> yamlGitConfig = yamlGitConfigRepository.findByAccountIdAndOrganizationIdAndProjectIdAndScope(
        accountId, orgIdentifier, projectIdentifier, scope);
    return orderYamlGitConfigByCreatedAt(yamlGitConfigDTO, yamlGitConfig);
  }

  private List<YamlGitConfigDTO> orderYamlGitConfigByCreatedAt(
      List<YamlGitConfigDTO> yamlGitConfigDTOs, List<YamlGitConfig> yamlGitConfig) {
    List<String> yamlGitConfigsIds = yamlGitConfig.stream().map(YamlGitConfig::getUuid).collect(Collectors.toList());
    return yamlGitConfigDTOs.stream()
        .sorted(new YamlGitConfigDTOComparator(yamlGitConfigsIds))
        .collect(Collectors.toList());
  }

  @Override
  public YamlGitConfigDTO getByFolderIdentifier(
      String projectIdentifier, String orgIdentifier, String accountId, String identifier) {
    // assuming identifier = uuid.
    Optional<YamlGitFolderConfig> yamlGitFolderConfig = yamlGitConfigFolderRepository.findById(identifier);
    if (yamlGitFolderConfig.isPresent()) {
      return toYamlGitConfigDTOFromFolderConfigWithSameYamlGitConfigId(
          Collections.singletonList(yamlGitFolderConfig.get()));
    }
    throw new InvalidRequestException("No git sync with given folder ID");
  }

  @Override
  public YamlGitConfigDTO getByFolderIdentifierAndIsEnabled(
      String projectIdentifier, String orgIdentifier, String accountId, String folderId) {
    // assuming identifier = uuid.
    Optional<YamlGitFolderConfig> yamlGitConfig =
        yamlGitConfigFolderRepository.findByUuidAndAccountIdAndEnabled(folderId, accountId, true);
    return yamlGitConfig
        .map(yamlGitFolderConfig
            -> toYamlGitConfigDTOFromFolderConfigWithSameYamlGitConfigId(
                Collections.singletonList(yamlGitFolderConfig)))
        .orElse(null);
  }

  private String findDefaultIfPresent(YamlGitConfigDTO yamlGitConfigDTO) {
    if (yamlGitConfigDTO.getDefaultRootFolder() != null) {
      return yamlGitConfigDTO.getDefaultRootFolder().getIdentifier();
    }
    return null;
  }

  private Optional<YamlGitFolderConfig> findDefaultIfPresent(
      String projectIdentifier, String orgIdentifier, String accountId) {
    Scope scope = getScope(accountId, orgIdentifier, projectIdentifier);
    Optional<YamlGitFolderConfig> defaultConfig =
        yamlGitConfigFolderRepository.findByAccountIdAndOrganizationIdAndProjectIdAndScopeAndIsDefault(
            accountId, orgIdentifier, projectIdentifier, scope, true);
    // assuming uuid = identifier
    return defaultConfig;
  }

  @Override
  public List<YamlGitConfigDTO> updateDefault(
      String projectIdentifier, String orgIdentifier, String accountId, String identifier, String folderIdentifier) {
    Scope scope = getScope(accountId, orgIdentifier, projectIdentifier);
    Optional<YamlGitFolderConfig> oldDefault =
        yamlGitConfigFolderRepository.findByAccountIdAndOrganizationIdAndProjectIdAndScopeAndIsDefault(
            accountId, orgIdentifier, projectIdentifier, scope, true);

    Optional<YamlGitFolderConfig> defaultToBe = yamlGitConfigFolderRepository.findById(folderIdentifier);
    // TODO(abhinav): add transaction
    if (defaultToBe.isPresent()) {
      defaultToBe.get().setDefault(true);
      yamlGitConfigFolderRepository.save(defaultToBe.get());
      if (oldDefault.isPresent()) {
        oldDefault.get().setDefault(false);
        yamlGitConfigFolderRepository.save(oldDefault.get());
      }
    }

    return get(projectIdentifier, orgIdentifier, accountId);
  }

  @Override
  public Optional<YamlGitConfigDTO.RootFolder> getDefault(
      String projectIdentifier, String orgIdentifier, String accountId) {
    Scope scope = getScope(accountId, orgIdentifier, projectIdentifier);
    Optional<YamlGitFolderConfig> yamlGitFolderConfig =
        yamlGitConfigFolderRepository.findByAccountIdAndOrganizationIdAndProjectIdAndScopeAndIsDefault(
            accountId, orgIdentifier, projectIdentifier, scope, true);
    return yamlGitFolderConfig.map(gitFolderConfig
        -> YamlGitConfigDTO.RootFolder.builder()
               .enabled(gitFolderConfig.isEnabled())
               .identifier(gitFolderConfig.getUuid())
               .rootFolder(gitFolderConfig.getRootFolder())
               .build());
  }

  @NotNull
  public List<YamlGitConfigDTO> getYamlGitConfigDTOsFromYamlGitFolderConfig(List<YamlGitFolderConfig> yamlGitConfigs) {
    Map<String, List<YamlGitFolderConfig>> yamlGitConfigFolderMap =
        yamlGitConfigs.stream().collect(Collectors.groupingBy(YamlGitFolderConfig::getYamlGitConfigId));
    List<YamlGitConfigDTO> yamlGitConfigDTOs = new ArrayList<>();
    yamlGitConfigFolderMap.forEach(
        (yamlGitConfigId, groupedYamlGitConfigFolders)
            -> yamlGitConfigDTOs.add(YamlGitConfigMapper.toYamlGitConfigDTOFromFolderConfigWithSameYamlGitConfigId(
                groupedYamlGitConfigFolders)));
    return yamlGitConfigDTOs;
  }

  @Override
  public YamlGitConfigDTO getByYamlGitConfigIdAndBranchAndRepoAndConnectorId(
      String identifier, String branch, String repo, String connectorId, String accountId) {
    // Assuming identifier == uuid.
    List<YamlGitFolderConfig> yamlGitConfig =
        yamlGitConfigFolderRepository.findByYamlGitConfigIdAndGitConnectorIdAndRepoAndBranchAndAccountId(
            identifier, connectorId, repo, branch, accountId);
    return YamlGitConfigMapper.toYamlGitConfigDTOFromFolderConfigWithSameYamlGitConfigId(yamlGitConfig);
  }

  @Override
  public List<YamlGitConfigDTO> getByConnectorRepoAndBranch(
      String gitConnectorId, String repo, String branchName, String accountId) {
    List<YamlGitFolderConfig> yamlGitConfigs =
        yamlGitConfigFolderRepository.findByGitConnectorIdAndRepoAndBranchAndAccountId(
            gitConnectorId, repo, branchName, accountId);
    return getYamlGitConfigDTOsFromYamlGitFolderConfig(yamlGitConfigs);
  }

  @Override
  public YamlGitConfigDTO save(YamlGitConfigDTO ygs) {
    return save(ygs, true);
  }

  void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  @Override
  public YamlGitConfigDTO update(YamlGitConfigDTO ygs) {
    validatePresenceOfRequiredFields(ygs.getAccountId(), ygs.getIdentifier());
    return save(ygs);
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
    YamlGitConfigDTO yamlGitConfigDTO = saveInternal(ygs);
    // TODO(abhinav): add full sync logic.
    return yamlGitConfigDTO;
  }

  private YamlGitConfigDTO saveInternal(YamlGitConfigDTO ygs) {
    List<YamlGitFolderConfig> yamlGitFolderConfigs = new ArrayList<>();
    String defaultGitConfig = findDefaultIfPresent(ygs);
    Optional<YamlGitFolderConfig> oldDefaultConfig =
        findDefaultIfPresent(ygs.getProjectId(), ygs.getOrganizationId(), ygs.getAccountId());

    YamlGitConfig yamlGitConfig = yamlGitConfigRepository.save(toYamlGitConfig(ygs));
    ygs.setIdentifier(yamlGitConfig.getUuid());
    yamlGitConfigFolderRepository.saveAll(toYamlGitFolderConfig(ygs))
        .iterator()
        .forEachRemaining(yamlGitFolderConfigs::add);

    if (defaultGitConfig != null && oldDefaultConfig.isPresent()
        && !oldDefaultConfig.get().getUuid().equals(defaultGitConfig)) {
      oldDefaultConfig.get().setDefault(false);
      yamlGitConfigFolderRepository.save(oldDefaultConfig.get());
    }

    return toYamlGitConfigDTOFromFolderConfigWithSameYamlGitConfigId(yamlGitFolderConfigs);
  }

  @Override
  public boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    Scope scope = getScope(accountId, orgIdentifier, projectIdentifier);
    return yamlGitConfigRepository.removeByAccountIdAndOrganizationIdAndProjectIdAndScopeAndUuid(
               accountId, orgIdentifier, projectIdentifier, scope, identifier)
        != 0;
  }
}
