package io.harness.accesscontrol.roleassignments.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.resourcegroups.api.ResourceGroupDTO;
import io.harness.accesscontrol.roles.api.RoleResponseDTO;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.harness.ScopeMapper;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class RoleAssignmentAggregateResponseDTOMapper {
  public static RoleAssignmentAggregateResponseDTO toDTO(List<RoleAssignmentDTO> roleAssignments, Scope scope,
      List<RoleResponseDTO> roles, List<ResourceGroupDTO> resourceGroups) {
    return RoleAssignmentAggregateResponseDTO.builder()
        .roleAssignments(roleAssignments)
        .scope(ScopeMapper.toDTO(scope))
        .roles(roles)
        .resourceGroups(resourceGroups)
        .build();
  }
}
