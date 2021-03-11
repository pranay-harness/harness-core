package io.harness.repositories.invites.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.entities.UserMembership;
import io.harness.repositories.invites.custom.UserMembershipRepositoryCustom;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(PL)
public interface UserMembershipRepository
    extends PagingAndSortingRepository<UserMembership, String>, UserMembershipRepositoryCustom {
  Optional<UserMembership> findDistinctByUserId(String userId);

  Long deleteUserMembershipByUserId(String userId);
}
