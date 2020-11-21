package io.harness.gitsync.core.dao.api.repositories.GitFileLocation;

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
  Optional<GitFileLocation> findByProjectIdAndOrganizationIdAndAccountIdAndEntityTypeAndEntityIdentifier(
      String projectId, String orgId, String accountId, String entityType, String entityId);

  List<GitFileLocation> findByProjectIdAndOrganizationIdAndAccountIdAndScope(
      String projectId, String orgId, String accountId, Scope scope);

  long countByProjectIdAndOrganizationIdAndAccountIdAndScopeAndEntityType(
      String projectId, String orgId, String accountId, Scope scope, String entityType);
}
