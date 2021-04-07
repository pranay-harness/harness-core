package io.harness.accesscontrol.roleassignments.api;

import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.principals.PrincipalType;

import io.swagger.annotations.ApiModel;
import java.util.Set;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel(value = "RoleAssignmentFilter")
public class RoleAssignmentFilterDTO {
  Set<String> resourceGroupFilter;
  Set<String> roleFilter;
  Set<PrincipalType> principalTypeFilter;
  @Size(max = 100) Set<PrincipalDTO> principalFilter;
  Set<Boolean> harnessManagedFilter;
  Set<Boolean> disabledFilter;
}
