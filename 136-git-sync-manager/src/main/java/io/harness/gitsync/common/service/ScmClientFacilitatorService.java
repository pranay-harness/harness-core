package io.harness.gitsync.common.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitPRCreateRequest;
import io.harness.gitsync.common.dtos.GitFileContent;
import io.harness.ng.beans.PageRequest;

import java.util.List;

// Don't inject this directly go through ScmClientOrchestrator.
@OwnedBy(DX)
public interface ScmClientFacilitatorService {
  List<String> listBranchesForRepoByConnector(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String connectorIdentifierRef, String repoURL, PageRequest pageRequest, String searchTerm);

  List<String> listBranchesForRepoByGitSyncConfig(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String yamlGitConfigIdentifier, PageRequest pageRequest, String searchTerm);

  GitFileContent getFileContent(String yamlGitConfigIdentifier, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String filePath, String branch, String commitId);

  Boolean createPullRequest(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String yamlGitConfigRef, GitPRCreateRequest gitCreatePRRequest);
}
