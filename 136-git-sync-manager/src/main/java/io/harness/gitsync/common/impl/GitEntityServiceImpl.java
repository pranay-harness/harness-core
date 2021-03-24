package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.encryption.ScopeHelper.getScope;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.EntityType;
import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.common.EntityReference;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.encryption.Scope;
import io.harness.gitsync.common.beans.GitFileLocation;
import io.harness.gitsync.common.beans.GitFileLocation.GitFileLocationKeys;
import io.harness.gitsync.common.dtos.GitSyncEntityDTO;
import io.harness.gitsync.common.dtos.GitSyncEntityListDTO;
import io.harness.gitsync.common.dtos.GitSyncProductDTO;
import io.harness.gitsync.common.dtos.RepoProviders;
import io.harness.gitsync.common.helper.GitFileLocationHelper;
import io.harness.gitsync.common.service.GitEntityService;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.EntityDetail;
import io.harness.repositories.gitFileLocation.GitFileLocationRepository;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.transport.URIish;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(DX)
public class GitEntityServiceImpl implements GitEntityService {
  private final GitFileLocationRepository gitFileLocationRepository;

  @Override
  public GitSyncProductDTO list(String projectId, String orgId, String accountId, ModuleType moduleType, int size) {
    final List<EntityType> entityTypes = getEntityTypesFromModuleType(moduleType);
    final Scope scope = getScope(accountId, orgId, projectId);
    final List<GitSyncEntityListDTO> gitSyncEntityListDTOs =
        entityTypes.stream()
            .map(entityType -> {
              final List<GitSyncEntityDTO> gitSyncEntityDTOs =
                  listByType(projectId, orgId, accountId, entityType, 0, size);
              final Long totalCount = countByType(projectId, orgId, accountId, scope, entityType);
              return buildGitSyncEntityListDTO(entityType, totalCount, gitSyncEntityDTOs);
            })
            .collect(Collectors.toList());
    return GitSyncProductDTO.builder().gitSyncEntityListDTOList(gitSyncEntityListDTOs).moduleType(moduleType).build();
  }

  private GitSyncEntityListDTO buildGitSyncEntityListDTO(
      EntityType entityType, Long totalCount, List<GitSyncEntityDTO> gitFileLocations) {
    return GitSyncEntityListDTO.builder()
        .entityType(entityType)
        .count(totalCount)
        .gitSyncEntities(gitFileLocations)
        .build();
  }

  private Page<GitFileLocation> getGitFileLocationsForEntityType(
      String projectId, String orgId, String accountId, Scope scope, Pageable pageable, EntityType entityType) {
    final Criteria criteria = getCriteriaWithScopeMatchAndEntityType(projectId, orgId, accountId, scope, entityType);
    return gitFileLocationRepository.getGitFileLocation(criteria, pageable);
  }

  @NotNull
  private Criteria getCriteriaWithScopeMatchAndEntityType(
      String projectId, String orgId, String accountId, Scope scope, EntityType entityTypes) {
    return getCriteriaWithScopeMatch(projectId, orgId, accountId, scope)
        .and(GitFileLocationKeys.entityType)
        .is(entityTypes.getYamlName());
  }

  @NotNull
  private Criteria getCriteriaWithScopeMatch(String projectId, String orgId, String accountId, Scope scope) {
    return Criteria.where(GitFileLocationKeys.accountId)
        .is(accountId)
        .and(GitFileLocationKeys.projectId)
        .is(projectId)
        .and(GitFileLocationKeys.organizationId)
        .is(orgId)
        .and(GitFileLocationKeys.scope)
        .is(scope);
  }

  private GitSyncEntityDTO buildGitSyncEntityDTO(GitFileLocation entity) {
    return GitSyncEntityDTO.builder()
        .branch(entity.getBranch())
        .entityIdentifier(entity.getEntityIdentifier())
        .entityName(entity.getEntityName())
        .entityType(EntityType.getEntityFromYamlType(entity.getEntityType()))
        .gitConnectorId(entity.getGitConnectorId())
        .repo(getDisplayRepositoryUrl(entity.getRepo()))
        .repoProviderType(getGitProvider(entity.getRepo()))
        .filePath(getEntityPath(entity))
        .yamlGitConfigId(entity.getYamlGitConfigId())
        .accountId(entity.getAccountId())
        .build();
  }

  private RepoProviders getGitProvider(String repositoryUrl) {
    try {
      URIish uri = new URIish(repositoryUrl);
      String host = uri.getHost();
      if (null != host) {
        for (RepoProviders repoProvider : RepoProviders.values()) {
          if (StringUtils.containsIgnoreCase(host, repoProvider.name())) {
            return repoProvider;
          }
        }
      }
    } catch (Exception e) {
      log.error("Failed to generate Git Provider Repository Url {}", repositoryUrl, e);
    }
    return RepoProviders.UNKNOWN;
  }

  private String getDisplayRepositoryUrl(String repositoryUrl) {
    try {
      URIish uri = new URIish(repositoryUrl);
      String path = uri.getPath();
      path = StringUtils.removeEnd(path, "/");
      path = StringUtils.removeEnd(path, ".git");
      path = StringUtils.removeStart(path, "/");
      return path;
    } catch (URISyntaxException e) {
      log.error("Failed to generate Display Repository Url {}", repositoryUrl, e);
    }
    return repositoryUrl;
  }

  @NotNull
  private String getEntityPath(GitFileLocation entity) {
    return GitFileLocationHelper.getEntityPath(
        entity.getEntityRootFolderName(), entity.getEntityType(), entity.getEntityIdentifier());
  }

  @Override
  public PageResponse<GitSyncEntityListDTO> getPageByType(
      String projectId, String orgId, String accountId, EntityType entityType, int page, int size) {
    final Scope scope = getScope(accountId, orgId, projectId);
    final Page<GitFileLocation> gitFileLocationsPage =
        gitSyncEntityDTOPageByType(projectId, orgId, accountId, entityType, page, size);
    final List<GitSyncEntityDTO> gitSyncEntityDTOList = buildEntityDtoFromPage(gitFileLocationsPage);
    final Long totalCount = countByType(projectId, orgId, accountId, scope, entityType);
    final GitSyncEntityListDTO gitSyncEntityListDTO =
        buildGitSyncEntityListDTO(entityType, totalCount, gitSyncEntityDTOList);
    return getNGPageResponse(gitFileLocationsPage, Collections.singletonList(gitSyncEntityListDTO));
  }

  private List<GitSyncEntityDTO> buildEntityDtoFromPage(Page<GitFileLocation> gitFileLocationsPage) {
    return gitFileLocationsPage.get().map(this::buildGitSyncEntityDTO).collect(Collectors.toList());
  }

  private List<GitSyncEntityDTO> listByType(
      String projectId, String orgId, String accountId, EntityType entityType, int page, int size) {
    final Page<GitFileLocation> gitFileLocationPage =
        gitSyncEntityDTOPageByType(projectId, orgId, accountId, entityType, page, size);
    return buildEntityDtoFromPage(gitFileLocationPage);
  }

  private long countByType(String projectId, String orgId, String accountId, Scope scope, EntityType entityType) {
    return gitFileLocationRepository.countByProjectIdAndOrganizationIdAndAccountIdAndScopeAndEntityType(
        projectId, orgId, accountId, scope, entityType.getYamlName());
  }

  private Page<GitFileLocation> gitSyncEntityDTOPageByType(
      String projectId, String orgId, String accountId, EntityType entityType, int page, int size) {
    Scope scope = getScope(accountId, orgId, projectId);
    final Pageable pageable = getPageRequest(page, size, Collections.singletonList("DESC"));
    return getGitFileLocationsForEntityType(projectId, orgId, accountId, scope, pageable, entityType);
  }

  @NotNull
  @VisibleForTesting
  public List<EntityType> getEntityTypesFromModuleType(ModuleType moduleType) {
    return new ArrayList<>(EntityType.getEntityTypes(moduleType));
  }

  @Override
  public GitSyncEntityDTO get(EntityReference entityReference, EntityType entityType) {
    final Optional<GitFileLocation> gitFileLocation =
        gitFileLocationRepository.findByEntityIdentifierFQNAndEntityTypeAndAccountId(
            entityReference.getFullyQualifiedName(), entityType.name(), entityReference.getAccountIdentifier());
    return gitFileLocation.map(this::buildGitSyncEntityDTO).orElse(null);
  }

  @Override
  public boolean save(
      String accountId, EntityDetail entityDetail, YamlGitConfigDTO yamlGitConfig, String filePath, String commitId) {
    final Optional<GitFileLocation> gitFileLocation =
        gitFileLocationRepository.findByEntityGitPathAndYamlGitConfigIdAndAccountId(
            filePath, yamlGitConfig.getIdentifier(), accountId);

    final GitFileLocation fileLocation = GitFileLocation.builder()
                                             .accountId(accountId)
                                             .entityIdentifier(entityDetail.getEntityRef().getIdentifier())
                                             .entityType(entityDetail.getType().name())
                                             .entityName(entityDetail.getName())
                                             .organizationId(entityDetail.getEntityRef().getOrgIdentifier())
                                             .projectId(entityDetail.getEntityRef().getProjectIdentifier())
                                             .yamlGitConfigId(yamlGitConfig.getIdentifier())
                                             .entityGitPath(filePath)
                                             .branch(yamlGitConfig.getBranch())
                                             .repo(yamlGitConfig.getRepo())
                                             .gitConnectorId(yamlGitConfig.getGitConnectorId())
                                             .scope(yamlGitConfig.getScope())
                                             .entityIdentifierFQN(entityDetail.getEntityRef().getFullyQualifiedName())
                                             .entityReference(entityDetail.getEntityRef())
                                             .lastCommitId(commitId)
                                             .build();
    gitFileLocation.ifPresent(location -> fileLocation.setUuid(location.getUuid()));
    gitFileLocationRepository.save(fileLocation);
    return true;
  }
}
