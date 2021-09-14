/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.common.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.BranchSyncStatus;
import io.harness.gitsync.common.beans.GitBranch;
import io.harness.gitsync.common.dtos.GitBranchListDTO;
import io.harness.ng.beans.PageRequest;

import com.mongodb.client.result.DeleteResult;

@OwnedBy(DX)
public interface GitBranchService {
  GitBranchListDTO listBranchesWithStatus(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String yamlGitConfigIdentifier, PageRequest pageRequest, String searchTerm, BranchSyncStatus branchSyncStatus);

  Boolean syncNewBranch(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String yamlGitConfigIdentifier, String branchName);

  void updateBranchSyncStatus(
      String accountIdentifier, String repoURL, String branchName, BranchSyncStatus branchSyncStatus);

  void createBranches(String accountId, String organizationIdentifier, String projectIdentifier, String gitConnectorRef,
      String repoUrl, String yamlGitConfigIdentifier);

  void save(GitBranch gitBranch);

  GitBranch get(String accountIdentifier, String repoURL, String branchName);

  void checkBranchIsNotAlreadyShortlisted(String repoURL, String accountId, String branch);

  boolean isBranchExists(String accountIdentifier, String repoURL, String branch, BranchSyncStatus branchSyncStatus);

  DeleteResult delete(String repoUrl, String branchName, String accountIdentifier);
}
