package io.harness.ng.core.invites.mapper;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.dto.RoleBinding;

import java.util.List;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class RoleBindingMapper {
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
}
