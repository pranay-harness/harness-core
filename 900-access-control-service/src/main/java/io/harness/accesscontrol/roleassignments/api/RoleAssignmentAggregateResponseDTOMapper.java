package io.harness.accesscontrol.roleassignments.api;

import io.harness.accesscontrol.resourcegroups.api.ResourceGroupDTO;
import io.harness.accesscontrol.roles.api.RoleResponseDTO;

import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RoleAssignmentAggregateResponseDTOMapper {
  public static RoleAssignmentAggregateResponseDTO toDTO(List<RoleAssignmentDTO> roleAssignments,
      String scopeIdentifier, List<RoleResponseDTO> roles, List<ResourceGroupDTO> resourceGroups) {
    return RoleAssignmentAggregateResponseDTO.builder()
        .roleAssignments(roleAssignments)
        .scope(scopeIdentifier)
        .roles(roles)
        .resourceGroups(resourceGroups)
        .build();
  }
}
