package io.harness.ng.core.invites.remote;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.core.invites.remote.RoleMapper.toRole;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.invites.dto.CreateInviteListDTO;
import io.harness.ng.core.invites.dto.InviteDTO;
import io.harness.ng.core.invites.entities.Invite;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.apache.commons.validator.routines.EmailValidator;

@UtilityClass
@OwnedBy(HarnessTeam.PL)
public class InviteMapper {
  static InviteDTO writeDTO(Invite invite) {
    return InviteDTO.builder()
        .id(invite.getId())
        .name(invite.getName())
        .email(invite.getEmail())
        .role(RoleMapper.writeDTO(invite.getRole()))
        .inviteType(invite.getInviteType())
        .approved(invite.getApproved())
        .build();
  }

  static Invite toInvite(InviteDTO inviteDTO, NGAccess ngAccess) {
    return Invite.builder()
        .id(inviteDTO.getId())
        .name(inviteDTO.getName())
        .email(inviteDTO.getEmail())
        .role(toRole(inviteDTO.getRole()))
        .inviteType(inviteDTO.getInviteType())
        .approved(inviteDTO.getApproved())
        .accountIdentifier(ngAccess.getAccountIdentifier())
        .orgIdentifier(ngAccess.getOrgIdentifier())
        .projectIdentifier(ngAccess.getProjectIdentifier())
        .build();
  }

  static List<Invite> toInviteList(CreateInviteListDTO createInviteListDTO, List<String> usernames, NGAccess ngAccess) {
    if (isEmpty(createInviteListDTO.getUsers())) {
      return new ArrayList<>();
    }

    EmailValidator emailValidator = EmailValidator.getInstance();
    List<String> emailIdList = createInviteListDTO.getUsers();

    List<Invite> invites = new ArrayList<>();
    Invite invite = null;

    for (int i = 0; i < emailIdList.size(); i++) {
      String emailId = emailIdList.get(i);
      if (emailValidator.isValid(emailId)) {
        invite = Invite.builder()
                     .email(emailId)
                     .name(usernames.get(i))
                     .role(toRole(createInviteListDTO.getRole()))
                     .inviteType(createInviteListDTO.getInviteType())
                     .approved(Boolean.FALSE)
                     .accountIdentifier(ngAccess.getAccountIdentifier())
                     .orgIdentifier(ngAccess.getOrgIdentifier())
                     .projectIdentifier(ngAccess.getProjectIdentifier())
                     .build();
        invites.add(invite);
      } else {
        invites.add(null);
      }
    }
    return invites;
  }
}
