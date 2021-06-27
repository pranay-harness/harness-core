package io.harness.gitsync.common.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.common.EntityReference;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.common.beans.GitFileLocation;
import io.harness.gitsync.common.dtos.GitSyncEntityDTO;
import io.harness.gitsync.common.dtos.GitSyncEntityListDTO;
import io.harness.gitsync.common.dtos.GitSyncRepoFilesListDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.EntityDetail;

import java.util.List;

@OwnedBy(DX)
public interface GitEntityService {
  PageResponse<GitSyncEntityListDTO> getPageByType(String projectIdentifier, String organizationIdentifier,
      String accountIdentifier, String gitSyncConfigIdentifier, String branch, EntityType entityType, int page,
      int size);

  GitSyncRepoFilesListDTO listSummary(String projectIdentifier, String organizationIdentifier, String accountIdentifier,
      ModuleType moduleType, String searchTerm, List<String> gitSyncConfigIdentifierList,
      List<EntityType> entityTypeList, int size);

  GitSyncEntityDTO get(EntityReference entityReference, EntityType entityType, String branch);

  boolean save(String accountId, EntityDetail entityDetail, YamlGitConfigDTO yamlGitConfig, String folderPath,
      String filePath, String commitId, String branchName);

  List<GitSyncEntityListDTO> listSummaryByRepoAndBranch(String projectIdentifier, String organizationIdentifier,
      String accountIdentifier, ModuleType moduleType, String searchTerm, String gitSyncConfigIdentifier, String branch,
      List<EntityType> entityTypeList, int size);

  List<GitFileLocation> getDefaultEntities(
      String accountIdentifier, String organizationIdentifier, String projectIdentifier, String yamlGitConfigId);

  GitSyncEntityDTO get(String accountIdentifier, String completeFilePath, String repoUrl, String branch);
}