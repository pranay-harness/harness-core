package io.harness.repositories.invites.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.core.invites.entities.Invite.InviteType;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.repositories.invites.custom.InviteRepositoryCustom;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(PL)
public interface InvitesRepository extends PagingAndSortingRepository<Invite, String>, InviteRepositoryCustom {
  Optional<Invite> findDistinctByIdAndDeleted(String id, Boolean notDeleted);

  List<Invite> findAllByEmailAndApprovedAndDeleted(String email, boolean approved, boolean deleted);

  Optional<Invite> findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndInviteTypeAndDeleted(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String email, InviteType inviteType,
      Boolean deleted);
}
