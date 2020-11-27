package io.harness.repositories.invites.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.core.invites.entities.Invite.InviteType;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.ng.core.invites.entities.Role;
import io.harness.repositories.invites.custom.InviteRepositoryCustom;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(PL)
public interface InvitesRepository extends PagingAndSortingRepository<Invite, String>, InviteRepositoryCustom {
  Optional<Invite> findFirstByIdAndDeletedNot(String id, Boolean notDeleted);

  Optional<Invite>
  findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndRoleAndInviteTypeAndDeletedNot(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String email, Role role,
      InviteType inviteType, Boolean notDeleted);

  List<Invite> findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndDeletedNot(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String email, Boolean notDeleted);

  Optional<Invite> deleteDistinctById(String inviteId);
}
