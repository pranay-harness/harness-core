package io.harness.repositories.invites.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.entities.UserProjectMap;
import io.harness.repositories.invites.custom.UserProjectMapRepositoryCustom;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(PL)
public interface UserProjectMapRepository
    extends PagingAndSortingRepository<UserProjectMap, String>, UserProjectMapRepositoryCustom {
  Optional<UserProjectMap> findByUserIdAndAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
      String userId, String accountIdentifier, String orgIdentifier, String projectIdentifier);

  Optional<UserProjectMap> findFirstByUserIdAndAccountIdentifier(String userId, String accountIdentifier);
}
