package io.harness.ng.core.remote;

import io.harness.ng.core.dto.RoleDTO;
import io.harness.ng.core.models.Role;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RoleMapper {
  static Role toRole(RoleDTO roleDTO) {
    return Role.builder().name(roleDTO.getName()).build();
  }

  static RoleDTO writeDTO(Role role) {
    return RoleDTO.builder().name(role.getName()).build();
  }
}
