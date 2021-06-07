package io.harness.gitsync.common.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitPRCreateRequest;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.common.dtos.GitDiffResultFileListDTO;
import io.harness.gitsync.common.dtos.GitFileChangeDTO;
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

  boolean createPullRequest(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String yamlGitConfigRef, GitPRCreateRequest gitCreatePRRequest);

  List<GitFileChangeDTO> listFilesOfBranches(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String yamlGitConfigIdentifier, List<String> foldersList, String branchName);

  List<GitFileChangeDTO> listFilesByFilePaths(
      YamlGitConfigDTO yamlGitConfigDTO, List<String> filePaths, String branchName);

  GitDiffResultFileListDTO listCommitsDiffFiles(
      YamlGitConfigDTO yamlGitConfigDTO, String initialCommitId, String finalCommitId);
}
