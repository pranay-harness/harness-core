package io.harness.ng.core.api.impl;

import static io.harness.exception.WingsException.USER_SRE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.DuplicateFieldException;
import io.harness.ng.core.api.InvitesService;
import io.harness.ng.core.api.repositories.spring.InvitesRepository;
import io.harness.ng.core.models.Invite;
import io.harness.ng.core.models.Role;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import software.wings.service.impl.InviteOperationResponse;

import java.util.Optional;
import javax.validation.constraints.NotNull;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class InvitesServiceImpl implements InvitesService {
  private final InvitesRepository invitesRepository;

  /**
   *  // @Ankush Actually map User to the project addUserToProject();
   * @param invite
   * @return
   */
  @Override
  public InviteOperationResponse create(Invite invite) {
    try {
      Optional<Invite> existingInviteOpt =
          invitesRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndRole(
              invite.getAccountIdentifier(), invite.getOrgIdentifier(), invite.getProjectIdentifier(),
              invite.getEmail(), invite.getRole());

      if (!existingInviteOpt.isPresent()) {
        invitesRepository.save(invite);
        return InviteOperationResponse.USER_INVITED_SUCCESSFULLY;
      }

      Invite existingInvite = existingInviteOpt.get();

      if (!existingInvite.getRole().equals(invite.getRole())) {
        invitesRepository.save(invite);
        return InviteOperationResponse.USER_INVITED_SUCCESSFULLY;
      } else {
        if (existingInvite.getInviteType() == invite.getInviteType()) {
          return InviteOperationResponse.USER_ALREADY_INVITED;
        } else {
          Optional<Invite> fullfilledInvite = delete(existingInvite.getUuid());
          return fullfilledInvite.isPresent() ? InviteOperationResponse.USER_ALREADY_ADDED
                                              : InviteOperationResponse.FAIL;
        }
      }

    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format("Invite [%s] under account [%s], organizatino [%s] and project [%s] already exists",
              invite.getUuid(), invite.getAccountIdentifier(), invite.getOrgIdentifier(),
              invite.getProjectIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public Optional<Invite> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String email, Role role) {
    return invitesRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndRole(
        accountIdentifier, orgIdentifier, projectIdentifier, email, role);
  }

  @Override
  public Page<Invite> list(@NotNull Criteria criteria, Pageable pageable) {
    return invitesRepository.findAll(criteria, pageable);
  }

  @Override
  public Optional<Invite> delete(String inviteId) {
    return invitesRepository.deleteByUuid(inviteId);
  }
}
