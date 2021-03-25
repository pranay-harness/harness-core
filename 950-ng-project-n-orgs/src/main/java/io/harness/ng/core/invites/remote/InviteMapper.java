package io.harness.ng.core.invites.remote;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.invites.dto.CreateInviteListDTO;
import io.harness.ng.core.invites.dto.InviteDTO;
import io.harness.ng.core.invites.entities.Invite;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
        .roleAssignments(invite.getRoleAssignments())
        .inviteType(invite.getInviteType())
        .approved(invite.getApproved())
        .build();
  }

  static Invite toInvite(InviteDTO inviteDTO, NGAccess ngAccess) {
    return Invite.builder()
        .id(inviteDTO.getId())
        .name(inviteDTO.getName())
        .email(inviteDTO.getEmail())
        .roleAssignments(inviteDTO.getRoleAssignments())
        .inviteType(inviteDTO.getInviteType())
        .approved(inviteDTO.getApproved())
        .accountIdentifier(ngAccess.getAccountIdentifier())
        .orgIdentifier(ngAccess.getOrgIdentifier())
        .projectIdentifier(ngAccess.getProjectIdentifier())
        .build();
  }

  static List<Invite> toInviteList(CreateInviteListDTO createInviteListDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier) {
    if (isEmpty(createInviteListDTO.getUsers())) {
      return new ArrayList<>();
    }

    EmailValidator emailValidator = EmailValidator.getInstance();
    List<String> emailIdList = createInviteListDTO.getUsers();
    List<Invite> invites = new ArrayList<>();
    for (String emailId : emailIdList) {
      if (emailValidator.isValid(emailId)) {
        invites.add(Invite.builder()
                        .email(emailId)
                        .roleAssignments(createRoleAssignment(createInviteListDTO.getRoleAssignments(),
                            accountIdentifier, orgIdentifier, projectIdentifier))
                        .inviteType(createInviteListDTO.getInviteType())
                        .approved(Boolean.FALSE)
                        .accountIdentifier(accountIdentifier)
                        .orgIdentifier(orgIdentifier)
                        .projectIdentifier(projectIdentifier)
                        .build());
      } else {
        invites.add(null);
      }
    }
    return invites;
  }

  private static List<RoleAssignmentDTO> createRoleAssignment(List<CreateInviteListDTO.RoleAssignment> roleAssignments,
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return roleAssignments.stream()
        .map(r
            -> RoleAssignmentDTO.builder()
                   .roleIdentifier(r.getRoleIdentifier())
                   .resourceGroupIdentifier(getResourceGroupIdentifier(
                       r.getResourceGroupIdentifier(), accountIdentifier, orgIdentifier, projectIdentifier))
                   .disabled(true)
                   .build())
        .collect(Collectors.toList());
  }

  private static String getResourceGroupIdentifier(
      String resourceGroupIdentifier, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (resourceGroupIdentifier != null) {
      return resourceGroupIdentifier;
    }
    String resourceGroup;
    if (projectIdentifier != null) {
      resourceGroup = projectIdentifier;
    } else if (orgIdentifier != null) {
      resourceGroup = orgIdentifier;
    } else {
      resourceGroup = accountIdentifier;
    }
    return String.format("_%s", resourceGroup);
  }
}
