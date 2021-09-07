package io.harness.accesscontrol.principals.usergroups.persistence;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(PL)
@HarnessRepo
public interface UserGroupRepository extends PagingAndSortingRepository<UserGroupDBO, String> {
  Optional<UserGroupDBO> findByIdentifierAndScopeIdentifier(String identifier, String scopeIdentifier);

  Page<UserGroupDBO> findByScopeIdentifier(String scopeIdentifier, Pageable pageable);

  List<UserGroupDBO> deleteByIdentifierAndScopeIdentifier(String identifier, String scopeIdentifier);
}
