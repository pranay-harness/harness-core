package io.harness.ng.core.invites.mapper;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.dto.RoleBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class RoleBindingMapper {
  private static final String DEFAULT_RESOURCE_GROUP_NAME = "All Resources";
  private static final String DEFAULT_RESOURCE_GROUP_IDENTIFIER = "_all_resources";

  public static io.harness.audit.beans.custom.user.RoleBinding toAuditRoleBinding(RoleBinding roleBinding) {
    return io.harness.audit.beans.custom.user.RoleBinding.builder()
        .roleIdentifier(roleBinding.getRoleIdentifier())
        .resourceGroupIdentifier(roleBinding.getResourceGroupIdentifier())
        .build();
  }

  public static List<io.harness.audit.beans.custom.user.RoleBinding> toAuditRoleBindings(
      List<RoleBinding> roleBindings) {
    if (roleBindings == null) {
      return null;
    }
    return roleBindings.stream().map(RoleBindingMapper::toAuditRoleBinding).collect(toList());
  }

  public static List<RoleAssignmentDTO> createRoleAssignmentDTOs(List<RoleBinding> roleBindings, String userId) {
    if (isEmpty(roleBindings)) {
      return new ArrayList<>();
    }
    return roleBindings.stream()
        .map(roleBinding -> {
          if (isBlank(roleBinding.getResourceGroupIdentifier())) {
            roleBinding.setResourceGroupIdentifier(DEFAULT_RESOURCE_GROUP_IDENTIFIER);
            roleBinding.setResourceGroupName(DEFAULT_RESOURCE_GROUP_NAME);
          }
          return RoleAssignmentDTO.builder()
              .roleIdentifier(roleBinding.getRoleIdentifier())
              .resourceGroupIdentifier(roleBinding.getResourceGroupIdentifier())
              .principal(PrincipalDTO.builder().type(PrincipalType.USER).identifier(userId).build())
              .disabled(false)
              .build();
        })
        .collect(Collectors.toList());
  }
}
