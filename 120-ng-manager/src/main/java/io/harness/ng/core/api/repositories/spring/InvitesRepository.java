package io.harness.ng.core.api.repositories.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.api.repositories.custom.InviteRepositoryCustom;
import io.harness.ng.core.models.Invite;
import io.harness.ng.core.models.Role;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@HarnessRepo
public interface InvitesRepository extends PagingAndSortingRepository<Invite, String>, InviteRepositoryCustom {
  Optional<Invite> findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndRole(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String email, Role role);
  Optional<Invite> deleteByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmail(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String email);
  Optional<Invite> deleteByUuid(String uuid);
}
