package io.harness.repositories.gitFileLocation;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.Scope;
import io.harness.gitsync.common.beans.GitFileLocation;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface GitFileLocationRepository
    extends PagingAndSortingRepository<GitFileLocation, String>, GitFileLocationRepositoryCustom {
  Optional<GitFileLocation> findByProjectIdAndOrganizationIdAndAccountIdAndEntityTypeAndEntityIdentifier(
      String projectId, String orgId, String accountId, String entityType, String entityId);

  long countByProjectIdAndOrganizationIdAndAccountIdAndScopeAndEntityType(
      String projectId, String orgId, String accountId, Scope scope, String entityType);

  Optional<GitFileLocation> findByEntityIdentifierFQNAndEntityTypeAndAccountId(
      String fqn, String entityType, String accountId);

  Optional<GitFileLocation> findByEntityGitPathAndGitSyncConfigIdAndAccountId(
      String entityGitPath, String gitSyncConfigId, String accountId);
}
