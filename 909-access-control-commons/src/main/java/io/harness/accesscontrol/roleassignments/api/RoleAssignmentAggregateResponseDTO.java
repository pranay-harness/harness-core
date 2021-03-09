package io.harness.accesscontrol.roleassignments.api;

import io.harness.accesscontrol.resourcegroups.api.ResourceGroupDTO;
import io.harness.accesscontrol.roles.api.RoleResponseDTO;

import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel(value = "RoleAssignmentAggregateResponse")
public class RoleAssignmentAggregateResponseDTO {
  List<RoleAssignmentDTO> roleAssignments;
  String scope;
  List<RoleResponseDTO> roles;
  List<ResourceGroupDTO> resourceGroups;
}
