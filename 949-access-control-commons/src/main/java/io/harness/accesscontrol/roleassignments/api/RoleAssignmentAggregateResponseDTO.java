package io.harness.accesscontrol.roleassignments.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.resourcegroups.api.ResourceGroupDTO;
import io.harness.accesscontrol.roles.api.RoleResponseDTO;
import io.harness.accesscontrol.scopes.ScopeDTO;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
@ApiModel(value = "RoleAssignmentAggregateResponse")
@Schema(name = "RoleAssignmentAggregateResponse")
public class RoleAssignmentAggregateResponseDTO {
  List<RoleAssignmentDTO> roleAssignments;
  ScopeDTO scope;
  List<RoleResponseDTO> roles;
  List<ResourceGroupDTO> resourceGroups;
}
