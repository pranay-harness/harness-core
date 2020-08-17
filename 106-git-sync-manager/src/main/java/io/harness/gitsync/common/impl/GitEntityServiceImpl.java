package io.harness.gitsync.common.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.gitsync.common.ScopeHelper.getScope;
import static io.harness.gitsync.common.YamlConstants.EXTENSION_SEPARATOR;
import static io.harness.gitsync.common.YamlConstants.PATH_DELIMITER;
import static io.harness.gitsync.common.YamlConstants.YAML_EXTENSION;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.beans.git.EntityScope.Scope;
import io.harness.gitsync.common.beans.GitFileLocation;
import io.harness.gitsync.common.dtos.GitSyncEntityDTO;
import io.harness.gitsync.common.dtos.GitSyncEntityListDTO;
import io.harness.gitsync.common.service.GitEntityService;
import io.harness.gitsync.core.dao.api.repositories.GitFileLocation.GitFileLocationRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class GitEntityServiceImpl implements GitEntityService {
  private GitFileLocationRepository gitFileLocationRepository;

  @Override
  public List<GitSyncEntityListDTO> list(String projectId, String orgId, String accountId) {
    Scope scope = getScope(accountId, orgId, projectId);
    List<GitFileLocation> gitSyncEntities =
        gitFileLocationRepository.findByProjectIdAndOrganizationIdAndAccountIdAndScope(
            projectId, orgId, accountId, scope);
    if (isEmpty(gitSyncEntities)) {
      return null;
    }
    Map<String, List<GitFileLocation>> entityTypeEntitiesMap =
        gitSyncEntities.stream().collect(Collectors.groupingBy(GitFileLocation::getEntityType));
    return entityTypeEntitiesMap.entrySet()
        .stream()
        .map(entities
            -> GitSyncEntityListDTO.builder()
                   .entityType(entities.getKey())
                   .gitSyncEntities(buildGitSyncEntityDTO(entities.getValue()))
                   .build())
        .collect(Collectors.toList());
  }

  private List<GitSyncEntityDTO> buildGitSyncEntityDTO(List<GitFileLocation> entities) {
    return entities.stream()
        .map(entity
            -> GitSyncEntityDTO.builder()
                   .branch(entity.getBranch())
                   .entityIdentifier(entity.getEntityIdentifier())
                   .entityName(entity.getEntityName())
                   .entityType(entity.getEntityType())
                   .gitConnectorId(entity.getGitConnectorId())
                   .repo(entity.getRepo())
                   .yamlPath(getYamlPath(entity))
                   .rootPath(entity.getEntityRootFolderName())
                   .build())
        .collect(Collectors.toList());
  }

  @NotNull
  private String getYamlPath(GitFileLocation entity) {
    return entity.getEntityType() + PATH_DELIMITER + entity.getEntityIdentifier() + EXTENSION_SEPARATOR
        + YAML_EXTENSION;
  }
}
