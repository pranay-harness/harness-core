/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitPRCreateRequest;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.delegate.task.scm.GitWebhookTaskType;
import io.harness.gitsync.common.beans.InfoForGitPush;
import io.harness.gitsync.common.dtos.CreatePRDTO;
import io.harness.gitsync.common.dtos.GitDiffResultFileListDTO;
import io.harness.gitsync.common.dtos.GitFileChangeDTO;
import io.harness.gitsync.common.dtos.GitFileContent;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.webhook.UpsertWebhookRequestDTO;
import io.harness.product.ci.scm.proto.Commit;
import io.harness.product.ci.scm.proto.CreateFileResponse;
import io.harness.product.ci.scm.proto.CreateWebhookResponse;
import io.harness.product.ci.scm.proto.DeleteFileResponse;
import io.harness.product.ci.scm.proto.UpdateFileResponse;

import java.util.List;
import java.util.Set;

// Don't inject this directly go through ScmClientOrchestrator.
@OwnedBy(DX)
public interface ScmClientFacilitatorService {
  List<String> listBranchesForRepoByConnector(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String connectorIdentifierRef, String repoURL, PageRequest pageRequest, String searchTerm);

  List<String> listBranchesForRepoByGitSyncConfig(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String yamlGitConfigIdentifier, PageRequest pageRequest, String searchTerm);

  GitFileContent getFileContent(String yamlGitConfigIdentifier, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String filePath, String branch, String commitId);

  CreatePRDTO createPullRequest(GitPRCreateRequest gitCreatePRRequest);

  List<GitFileChangeDTO> listFilesOfBranches(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String yamlGitConfigIdentifier, Set<String> foldersList, String branchName);

  // Find content of the files in branchName at the latest commit id of the branch
  List<GitFileChangeDTO> listFilesByFilePaths(
      YamlGitConfigDTO yamlGitConfigDTO, List<String> filePaths, String branchName);

  // Find content of the files at given commitId
  List<GitFileChangeDTO> listFilesByCommitId(
      YamlGitConfigDTO yamlGitConfigDTO, List<String> filePaths, String commitId);

  GitDiffResultFileListDTO listCommitsDiffFiles(
      YamlGitConfigDTO yamlGitConfigDTO, String initialCommitId, String finalCommitId);

  List<String> listCommits(YamlGitConfigDTO yamlGitConfigDTO, String branch);

  Commit getLatestCommit(YamlGitConfigDTO yamlGitConfigDTO, String branch);

  CreateFileResponse createFile(InfoForGitPush infoForPush);

  UpdateFileResponse updateFile(InfoForGitPush infoForPush);

  DeleteFileResponse deleteFile(InfoForGitPush infoForPush);

  Commit findCommitById(YamlGitConfigDTO yamlGitConfigDTO, String commitId);

  CreateWebhookResponse upsertWebhook(
      UpsertWebhookRequestDTO upsertWebhookRequestDTO, String target, GitWebhookTaskType gitWebhookTaskType);
}
