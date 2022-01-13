/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.scm.ScmGitUtils.createFilePath;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.EntityType;
import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.common.EntityReference;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.encryption.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.common.beans.GitFileLocation;
import io.harness.gitsync.common.beans.GitFileLocation.GitFileLocationKeys;
import io.harness.gitsync.common.dtos.GitSyncEntityDTO;
import io.harness.gitsync.common.dtos.GitSyncEntityListDTO;
import io.harness.gitsync.common.dtos.GitSyncRepoFilesDTO;
import io.harness.gitsync.common.dtos.GitSyncRepoFilesListDTO;
import io.harness.gitsync.common.dtos.RepoProviders;
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
import org.springframework.dao.DuplicateKeyException;
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
  public GitSyncRepoFilesListDTO listSummary(String projectIdentifier, String organizationIdentifier,
      String accountIdentifier, ModuleType moduleType, String searchTerm, List<String> gitSyncConfigIdentifierList,
      List<EntityType> entityTypeList, int size) {
    List<GitSyncRepoFilesDTO> gitSyncRepoFilesDTOList =
        gitFileLocationRepository
            .getByProjectIdAndOrganizationIdAndAccountIdAndGitSyncConfigIdentifierListAndEntityTypeList(
                projectIdentifier, organizationIdentifier, accountIdentifier, gitSyncConfigIdentifierList,
                entityTypeList, searchTerm, size);
    return GitSyncRepoFilesListDTO.builder()
        .moduleType(moduleType)
        .gitSyncRepoFilesList(gitSyncRepoFilesDTOList)
        .build();
  }

  @Override
  public PageResponse<GitSyncEntityListDTO> getPageByType(String projectIdentifier, String organizationIdentifier,
      String accountIdentifier, String gitSyncConfigIdentifier, String branch, EntityType entityType, int page,
      int size) {
    final Page<GitFileLocation> gitFileLocationsPage = gitSyncEntityDTOPageByType(projectIdentifier,
        organizationIdentifier, accountIdentifier, gitSyncConfigIdentifier, branch, entityType, page, size);
    final List<GitSyncEntityDTO> gitSyncEntityDTOList = buildEntityDtoFromPage(gitFileLocationsPage);
    final GitSyncEntityListDTO gitSyncEntityListDTO =
        buildGitSyncEntityListDTO(entityType, (long) gitSyncEntityDTOList.size(), gitSyncEntityDTOList);
    return getNGPageResponse(gitFileLocationsPage, Collections.singletonList(gitSyncEntityListDTO));
  }

  @Override
  public List<GitSyncEntityListDTO> listSummaryByRepoAndBranch(String projectIdentifier, String organizationIdentifier,
      String accountIdentifier, ModuleType moduleType, String searchTerm, String gitSyncConfigIdentifier, String branch,
      List<EntityType> entityTypeList, int size) {
    return gitFileLocationRepository
        .getByProjectIdAndOrganizationIdAndAccountIdAndGitSyncConfigIdentifierAndEntityTypeListAndBranch(
            projectIdentifier, organizationIdentifier, accountIdentifier, gitSyncConfigIdentifier, branch,
            entityTypeList, searchTerm, size);
  }

  @Override
  public List<GitFileLocation> getDefaultEntities(
      String accountIdentifier, String organizationIdentifier, String projectIdentifier, String yamlGitConfigId) {
    return gitFileLocationRepository.findByAccountIdAndOrganizationIdAndProjectIdAndGitSyncConfigIdAndIsDefault(
        accountIdentifier, organizationIdentifier, projectIdentifier, yamlGitConfigId, true);
  }

  @Override
  public Optional<GitSyncEntityDTO> get(
      String accountIdentifier, String completeFilePath, String repoUrl, String branch) {
    final Optional<GitFileLocation> entityDetails =
        gitFileLocationRepository.findByAccountIdAndCompleteGitPathAndRepoAndBranch(
            accountIdentifier, completeFilePath, repoUrl, branch);
    if (entityDetails.isPresent()) {
      return Optional.ofNullable(buildGitSyncEntityDTO(entityDetails.get()));
    }
    // todo @deepak; Temprory fix, will add migration later
    List<GitFileLocation> gitFileLocations =
        gitFileLocationRepository.findByAccountIdAndRepoAndBranch(accountIdentifier, repoUrl, branch);
    for (GitFileLocation gitFileLocation : gitFileLocations) {
      if (completeFilePath.equals(
              createFilePath(gitFileLocation.getFolderPath(), gitFileLocation.getEntityGitPath()))) {
        return Optional.ofNullable(buildGitSyncEntityDTO(gitFileLocation));
      }
    }
    return Optional.empty();
  }

  private GitSyncEntityListDTO buildGitSyncEntityListDTO(
      EntityType entityType, Long totalCount, List<GitSyncEntityDTO> gitFileLocations) {
    return GitSyncEntityListDTO.builder()
        .entityType(entityType)
        .count(totalCount)
        .gitSyncEntities(gitFileLocations)
        .build();
  }

  private Page<GitFileLocation> getGitFileLocationsForEntityType(String projectIdentifier,
      String organizationIdentifier, String accountIdentifier, Pageable pageable, String gitSyncConfigIdentifier,
      String branch, EntityType entityType) {
    final Criteria criteria = getCriteriaWithScopeMatchAndEntityType(
        projectIdentifier, organizationIdentifier, accountIdentifier, gitSyncConfigIdentifier, branch, entityType);
    return gitFileLocationRepository.getGitFileLocation(criteria, pageable);
  }

  @NotNull
  private Criteria getCriteriaWithScopeMatchAndEntityType(String projectIdentifier, String organizationIdentifier,
      String accountIdentifier, String gitSyncConfigIdentifier, String branch, EntityType entityType) {
    return getCriteriaWithScopeMatch(
        projectIdentifier, organizationIdentifier, accountIdentifier, gitSyncConfigIdentifier, branch)
        .and(GitFileLocationKeys.entityType)
        .is(entityType.name());
  }

  @NotNull
  private Criteria getCriteriaWithScopeMatch(String projectIdentifier, String organizationIdentifier,
      String accountIdentifier, String gitSyncConfigIdentifier, String branch) {
    return Criteria.where(GitFileLocationKeys.accountId)
        .is(accountIdentifier)
        .and(GitFileLocationKeys.projectId)
        .is(projectIdentifier)
        .and(GitFileLocationKeys.organizationId)
        .is(organizationIdentifier)
        .and(GitFileLocationKeys.gitSyncConfigId)
        .is(gitSyncConfigIdentifier)
        .and(GitFileLocationKeys.branch)
        .is(branch);
  }

  private GitSyncEntityDTO buildGitSyncEntityDTO(GitFileLocation entity) {
    return GitSyncEntityDTO.builder()
        .branch(entity.getBranch())
        .entityIdentifier(entity.getEntityIdentifier())
        .entityName(entity.getEntityName())
        .entityType(EntityType.valueOf(entity.getEntityType()))
        .gitConnectorId(entity.getGitConnectorId())
        .repo(entity.getRepo())
        .repoProviderType(getGitProvider(entity.getRepo()))
        .folderPath(entity.getFolderPath())
        .entityGitPath(entity.getEntityGitPath())
        .accountId(entity.getAccountId())
        .entityReference(entity.getEntityReference())
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

  private List<GitSyncEntityDTO> buildEntityDtoFromPage(Page<GitFileLocation> gitFileLocationsPage) {
    return gitFileLocationsPage.get().map(this::buildGitSyncEntityDTO).collect(Collectors.toList());
  }

  private long countByType(String projectIdentifier, String organizationIdentifier, String accountIdentifier,
      Scope scope, EntityType entityType) {
    return gitFileLocationRepository.countByProjectIdAndOrganizationIdAndAccountIdAndScopeAndEntityType(
        projectIdentifier, organizationIdentifier, accountIdentifier, scope, entityType.getYamlName());
  }

  private Page<GitFileLocation> gitSyncEntityDTOPageByType(String projectIdentifier, String organizationIdentifier,
      String accountIdentifier, String gitSyncConfigIdentifier, String branch, EntityType entityType, int page,
      int size) {
    final Pageable pageable = getPageRequest(page, size, Collections.singletonList("DESC"));
    return getGitFileLocationsForEntityType(projectIdentifier, organizationIdentifier, accountIdentifier, pageable,
        gitSyncConfigIdentifier, branch, entityType);
  }

  @NotNull
  @VisibleForTesting
  public List<EntityType> getEntityTypesFromModuleType(ModuleType moduleType) {
    return new ArrayList<>(EntityType.getEntityTypes(moduleType));
  }

  private List<GitSyncEntityDTO> buildEntityDTOListFromFileLocation(
      List<GitFileLocation> gitFileLocationList, EntityType entityType) {
    List<GitSyncEntityDTO> gitSyncEntityDTOList = new ArrayList<>();
    for (GitFileLocation gitFileLocation : gitFileLocationList) {
      gitSyncEntityDTOList.add(buildGitSyncEntityDTO(gitFileLocation));
    }
    return gitSyncEntityDTOList;
  }

  @Override
  public GitSyncEntityDTO get(EntityReference entityReference, EntityType entityType, String branch) {
    Optional<GitFileLocation> gitFileLocation;
    try {
      gitFileLocation = gitFileLocationRepository.findByEntityIdentifierFQNAndEntityTypeAndAccountIdAndBranch(
          entityReference.getFullyQualifiedName(), entityType.name(), entityReference.getAccountIdentifier(), branch);
    } catch (DuplicateKeyException ex) {
      log.error("Error encountered while getting the git entity for {} in the branch {} in account {}",
          entityReference.getFullyQualifiedName(), branch, entityReference.getAccountIdentifier(), ex);
      throw new InvalidRequestException(
          String.format("Multiple git entity records exists for the %s with the identifier %s",
              entityType.getYamlName(), entityReference.getIdentifier()));
    }
    return gitFileLocation.map(this::buildGitSyncEntityDTO).orElse(null);
  }

  @Override
  public boolean save(String accountId, EntityDetail entityDetail, YamlGitConfigDTO yamlGitConfig, String folderPath,
      String filePath, String commitId, String branchName) {
    final Optional<GitFileLocation> gitFileLocation =
        gitFileLocationRepository.findByEntityGitPathAndGitSyncConfigIdAndAccountIdAndBranch(
            filePath, yamlGitConfig.getIdentifier(), accountId, branchName);
    String completeFilePath = createFilePath(folderPath, filePath);
    // todo(abhinav): changeisDefault to value which comes when
    final GitFileLocation fileLocation = GitFileLocation.builder()
                                             .accountId(accountId)
                                             .entityIdentifier(entityDetail.getEntityRef().getIdentifier())
                                             .entityType(entityDetail.getType().name())
                                             .entityName(entityDetail.getName())
                                             .organizationId(entityDetail.getEntityRef().getOrgIdentifier())
                                             .projectId(entityDetail.getEntityRef().getProjectIdentifier())
                                             .completeGitPath(completeFilePath)
                                             .folderPath(folderPath)
                                             .entityGitPath(filePath)
                                             .branch(branchName)
                                             .repo(yamlGitConfig.getRepo())
                                             .gitConnectorId(yamlGitConfig.getGitConnectorRef())
                                             .scope(yamlGitConfig.getScope())
                                             .entityIdentifierFQN(entityDetail.getEntityRef().getFullyQualifiedName())
                                             .entityReference(entityDetail.getEntityRef())
                                             .lastCommitId(commitId)
                                             .gitSyncConfigId(yamlGitConfig.getIdentifier())
                                             .isDefault(branchName.equals(yamlGitConfig.getBranch()))
                                             .build();
    gitFileLocation.ifPresent(location -> fileLocation.setUuid(location.getUuid()));
    gitFileLocationRepository.save(fileLocation);
    return true;
  }
}
