package io.harness.ng.core.invites.remote;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.dto.RoleDTO;
import io.harness.ng.core.invites.entities.Role;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PL)
public class RoleMapper {
  static Role toRole(RoleDTO roleDTO) {
    return Role.builder().name(roleDTO.getName()).build();
  }

  static RoleDTO writeDTO(Role role) {
    return RoleDTO.builder().name(role.getName()).build();
  }
}
