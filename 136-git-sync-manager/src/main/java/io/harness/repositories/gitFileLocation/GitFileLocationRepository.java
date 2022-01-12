/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.gitFileLocation;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.Scope;
import io.harness.gitsync.common.beans.GitFileLocation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface GitFileLocationRepository
    extends PagingAndSortingRepository<GitFileLocation, String>, GitFileLocationRepositoryCustom {
  long countByProjectIdAndOrganizationIdAndAccountIdAndScopeAndEntityType(
      String projectId, String orgId, String accountId, Scope scope, String entityType);

  Optional<GitFileLocation> findByEntityIdentifierFQNAndEntityTypeAndAccountIdAndBranch(
      String fqn, String entityType, String accountId, String branch);

  Optional<GitFileLocation> findByEntityGitPathAndGitSyncConfigIdAndAccountIdAndBranch(
      String entityGitPath, String gitSyncConfigId, String accountId, String branch);

  List<GitFileLocation> findByAccountIdAndOrganizationIdAndProjectIdAndGitSyncConfigIdAndIsDefault(
      String accountIdentifier, String organizationIdentifier, String projectIdentifier, String yamlGitConfigId,
      boolean b);

  Optional<GitFileLocation> findByAccountIdAndCompleteGitPathAndRepoAndBranch(
      String accountIdentifier, String completeFilePath, String repoUrl, String branch);

  List<GitFileLocation> findByAccountIdAndRepoAndBranch(String accountIdentifier, String repoUrl, String branch);
}
