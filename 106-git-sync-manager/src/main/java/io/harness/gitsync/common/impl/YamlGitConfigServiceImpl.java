package io.harness.gitsync.common.impl;

import static io.harness.gitsync.common.ScopeHelper.getScope;
import static io.harness.gitsync.common.remote.YamlGitConfigMapper.toYamlGitConfig;
import static io.harness.gitsync.common.remote.YamlGitConfigMapper.toYamlGitConfigDTOFromFolderConfigWithSameYamlGitConfigId;
import static io.harness.gitsync.common.remote.YamlGitConfigMapper.toYamlGitFolderConfig;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.gitsync.common.EntityScope.Scope;
import io.harness.gitsync.common.beans.YamlGitFolderConfig;
import io.harness.gitsync.common.dao.api.repositories.yamlGitConfig.YamlGitConfigRepository;
import io.harness.gitsync.common.dao.api.repositories.yamlGitFolderConfig.YamlGitFolderConfigRepository;
import io.harness.gitsync.common.dtos.YamlGitConfigDTO;
import io.harness.gitsync.common.remote.YamlGitConfigMapper;
import io.harness.gitsync.common.service.YamlGitConfigService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class YamlGitConfigServiceImpl implements YamlGitConfigService {
  private final YamlGitConfigRepository yamlGitConfigRepository;
  private final YamlGitFolderConfigRepository yamlGitConfigFolderRepository;

  @Override
  public List<YamlGitConfigDTO> get(String projectIdentifier, String orgIdentifier, String accountId) {
    Scope scope = getScope(accountId, orgIdentifier, projectIdentifier);
    List<YamlGitFolderConfig> yamlGitConfigs =
        yamlGitConfigFolderRepository.findByAccountIdAndOrganizationIdAndProjectIdAndScope(
            accountId, orgIdentifier, projectIdentifier, scope);
    return getYamlGitConfigDTOsFromYamlGitFolderConfig(yamlGitConfigs);
  }

  @Override
  public YamlGitConfigDTO getByIdentifier(
      String projectIdentifier, String orgIdentifier, String accountId, String identifier) {
    Scope scope = getScope(accountId, orgIdentifier, projectIdentifier);
    // assuming identifier = uuid.
    List<YamlGitFolderConfig> yamlGitConfigs =
        yamlGitConfigFolderRepository.findByAccountIdAndOrganizationIdAndProjectIdAndScopeAndYamlGitConfigId(
            accountId, orgIdentifier, projectIdentifier, scope, identifier);
    return toYamlGitConfigDTOFromFolderConfigWithSameYamlGitConfigId(yamlGitConfigs);
  }

  private YamlGitFolderConfig findDefaultIfPresent(String projectIdentifier, String orgIdentifier, String accountId) {
    Scope scope = getScope(accountId, orgIdentifier, projectIdentifier);
    YamlGitFolderConfig defaultConfig =
        yamlGitConfigFolderRepository.findByAccountIdAndOrganizationIdAndProjectIdAndScopeAndIsDefault(
            accountId, orgIdentifier, projectIdentifier, scope, true);
    // assuming uuid = identifier
    return defaultConfig;
  }

  private String findDefaultIfPresent(YamlGitConfigDTO yamlGitConfigDTO) {
    if (yamlGitConfigDTO.getDefaultRootFolder() != null) {
      return yamlGitConfigDTO.getDefaultRootFolder().getIdentifier();
    }
    return null;
  }

  @Override
  public List<YamlGitConfigDTO> updateDefault(
      String projectIdentifier, String orgIdentifier, String accountId, String identifier, String folderIdentifier) {
    Scope scope = getScope(accountId, orgIdentifier, projectIdentifier);
    YamlGitFolderConfig oldDefault =
        yamlGitConfigFolderRepository.findByAccountIdAndOrganizationIdAndProjectIdAndScopeAndIsDefault(
            accountId, orgIdentifier, projectIdentifier, scope, true);

    Optional<YamlGitFolderConfig> defaultToBe = yamlGitConfigFolderRepository.findById(folderIdentifier);

    if (defaultToBe.isPresent()) {
      defaultToBe.get().setDefault(true);
      yamlGitConfigFolderRepository.save(defaultToBe.get());
      if (oldDefault != null) {
        oldDefault.setDefault(false);
        yamlGitConfigFolderRepository.save(oldDefault);
      }
    }

    return get(projectIdentifier, orgIdentifier, accountId);
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
  public YamlGitConfigDTO save(YamlGitConfigDTO ygs, boolean performFullSync) {
    YamlGitConfigDTO yamlGitConfigDTO = saveInternal(ygs);
    // TODO(abhinav): add full sync logic.
    return yamlGitConfigDTO;
  }

  private YamlGitConfigDTO saveInternal(YamlGitConfigDTO ygs) {
    List<YamlGitFolderConfig> yamlGitFolderConfigs = new ArrayList<>();
    String defaultGitConfig = findDefaultIfPresent(ygs);
    YamlGitFolderConfig oldDefaultConfig =
        findDefaultIfPresent(ygs.getProjectId(), ygs.getOrganizationId(), ygs.getAccountId());

    yamlGitConfigRepository.save(toYamlGitConfig(ygs));
    yamlGitConfigFolderRepository.saveAll(toYamlGitFolderConfig(ygs))
        .iterator()
        .forEachRemaining(yamlGitFolderConfigs::add);

    if (defaultGitConfig != null && oldDefaultConfig != null && !oldDefaultConfig.getUuid().equals(defaultGitConfig)) {
      oldDefaultConfig.setDefault(false);
      yamlGitConfigFolderRepository.save(oldDefaultConfig);
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
