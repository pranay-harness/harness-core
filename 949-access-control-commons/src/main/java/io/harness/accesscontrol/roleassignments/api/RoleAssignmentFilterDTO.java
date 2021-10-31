package io.harness.accesscontrol.roleassignments.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel(value = "RoleAssignmentFilter")
@Schema(name = "RoleAssignmentFilter")
@OwnedBy(PL)
public class RoleAssignmentFilterDTO {
  Set<String> resourceGroupFilter;
  Set<String> roleFilter;
  Set<PrincipalType> principalTypeFilter;
  Set<PrincipalDTO> principalFilter;
  Set<Boolean> harnessManagedFilter;
  Set<Boolean> disabledFilter;
}
