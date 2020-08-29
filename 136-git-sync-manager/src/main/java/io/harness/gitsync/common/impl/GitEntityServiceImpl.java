package io.harness.gitsync.common.impl;

import static io.harness.encryption.ScopeHelper.getScope;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.NGPageResponse;
import io.harness.encryption.Scope;
import io.harness.gitsync.common.GitFileLocationHelper;
import io.harness.gitsync.common.beans.GitFileLocation;
import io.harness.gitsync.common.beans.GitFileLocation.GitFileLocationKeys;
import io.harness.gitsync.common.dtos.GitSyncEntityDTO;
import io.harness.gitsync.common.dtos.GitSyncEntityListDTO;
import io.harness.gitsync.common.dtos.GitSyncProductDTO;
import io.harness.gitsync.common.service.GitEntityService;
import io.harness.gitsync.core.EntityType;
import io.harness.gitsync.core.Product;
import io.harness.gitsync.core.dao.api.repositories.GitFileLocation.GitFileLocationRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class GitEntityServiceImpl implements GitEntityService {
  private GitFileLocationRepository gitFileLocationRepository;

  @Override
  public GitSyncProductDTO list(String projectId, String orgId, String accountId, Product product, int size) {
    final List<EntityType> entityTypes = getEntityTypesFromProduct(product);
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
    return GitSyncProductDTO.builder().gitSyncEntityListDTOList(gitSyncEntityListDTOs).productName(product).build();
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
        .is(entityTypes.getEntityName());
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
        .entityType(EntityType.getEntityName(entity.getEntityType()))
        .gitConnectorId(entity.getGitConnectorId())
        .repo(entity.getRepo())
        .filePath(getEntityPath(entity))
        .build();
  }

  @NotNull
  private String getEntityPath(GitFileLocation entity) {
    return GitFileLocationHelper.getEntityPath(
        entity.getEntityRootFolderName(), entity.getEntityType(), entity.getEntityIdentifier());
  }

  @Override
  public NGPageResponse<GitSyncEntityListDTO> getPageByType(
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
    return gitFileLocationsPage.get().map(this ::buildGitSyncEntityDTO).collect(Collectors.toList());
  }

  private List<GitSyncEntityDTO> listByType(
      String projectId, String orgId, String accountId, EntityType entityType, int page, int size) {
    final Page<GitFileLocation> gitFileLocationPage =
        gitSyncEntityDTOPageByType(projectId, orgId, accountId, entityType, page, size);
    return buildEntityDtoFromPage(gitFileLocationPage);
  }

  private long countByType(String projectId, String orgId, String accountId, Scope scope, EntityType entityType) {
    return gitFileLocationRepository.countByProjectIdAndOrganizationIdAndAccountIdAndScopeAndEntityType(
        projectId, orgId, accountId, scope, entityType.getEntityName());
  }

  private Page<GitFileLocation> gitSyncEntityDTOPageByType(
      String projectId, String orgId, String accountId, EntityType entityType, int page, int size) {
    Scope scope = getScope(accountId, orgId, projectId);
    final Pageable pageable = getPageRequest(page, size, Collections.singletonList("DESC"));
    return getGitFileLocationsForEntityType(projectId, orgId, accountId, scope, pageable, entityType);
  }

  @NotNull
  @VisibleForTesting
  public List<EntityType> getEntityTypesFromProduct(Product product) {
    return new ArrayList<>(EntityType.getEntityTypes(product));
  }
}
