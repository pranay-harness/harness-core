/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.repositories.yamlGitConfig;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.Scope;
import io.harness.gitsync.common.beans.YamlGitConfig;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface YamlGitConfigRepository extends CrudRepository<YamlGitConfig, String> {
  Long deleteByAccountIdAndOrgIdentifierAndProjectIdentifierAndScopeAndIdentifier(
      String accountId, String organizationIdentifier, String projectIdentifier, Scope scope, String identifier);

  Optional<YamlGitConfig> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifier(
      String accountId, String organizationIdentifier, String projectIdentifier, String identifier);

  List<YamlGitConfig> findByGitConnectorRefAndRepoAndBranchAndAccountId(
      String gitConnectorId, String repo, String branchName, String accountId);

  boolean existsByAccountIdAndOrgIdentifierAndProjectIdentifier(
      String accountIdentifier, String organizationIdentifier, String projectIdentifier);

  List<YamlGitConfig> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndScopeOrderByCreatedAtDesc(
      String accountId, String orgIdentifier, String projectIdentifier, Scope scope);

  Boolean existsByRepo(String repo);

  List<YamlGitConfig> findByAccountIdAndRepoOrderByCreatedAtDesc(String accountId, String repo);

  Optional<YamlGitConfig> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndRepo(
      String accountId, String organizationIdentifier, String projectIdentifier, String repo);
}
