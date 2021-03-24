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
  Long removeByAccountIdAndOrganizationIdAndProjectIdAndScopeAndUuid(
      String accountId, String organizationIdentifier, String projectIdentifier, Scope scope, String uuid);

  List<YamlGitConfig> findByAccountIdAndOrganizationIdAndProjectIdAndScope(
      String accountId, String organizationIdentifier, String projectIdentifier, Scope scope);

  Optional<YamlGitConfig> findByAccountIdAndOrganizationIdAndProjectIdAndUuid(
      String accountId, String organizationIdentifier, String projectIdentifier, String uuid);

  List<YamlGitConfig> findByAccountIdAndOrganizationIdAndProjectIdAndScopeOrderByCreatedAtAsc(
      String accountId, String organizationIdentifier, String projectIdentifier, Scope scope);

  List<YamlGitConfig> findByGitConnectorIdAndRepoAndBranchAndAccountId(
      String gitConnectorId, String repo, String branchName, String accountId);

  List<YamlGitConfig> findByRepoAndBranchAndAccountId(String repo, String branch, String accountId);

  Optional<YamlGitConfig> findByUuidAndAccountId(String uuid, String accountId);
}
