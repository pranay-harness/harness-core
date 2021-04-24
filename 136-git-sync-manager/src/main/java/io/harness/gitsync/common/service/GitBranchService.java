package io.harness.gitsync.common.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.BranchSyncStatus;
import io.harness.gitsync.common.dtos.GitBranchDTO;
import io.harness.ng.beans.PageResponse;

import java.util.List;

@OwnedBy(DX)
public interface GitBranchService {
  List<String> listBranchesForRepoByConnector(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String repoURL);

  List<String> listBranchesForRepoByGitSyncConfig(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String yamlGitConfigIdentifier);

  PageResponse<GitBranchDTO> listBranchesWithStatus(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String yamlGitConfigIdentifier, int page, int size, String searchTerm);

  Boolean syncNewBranch(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String yamlGitConfigIdentifier, String branchName);

  void updateBranchSyncStatus(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, String branchName, BranchSyncStatus branchSyncStatus);

  void createBranches(String accountId, String organizationIdentifier, String projectIdentifier, String gitConnectorRef,
      String repoUrl, String yamlGitConfigIdentifier);
}
