package io.harness.accesscontrol.rolebindings.database;

import io.harness.accesscontrol.rolebindings.RoleBindingDTO;

import lombok.experimental.UtilityClass;

@UtilityClass
class RoleBindingMapper {
  public static RoleBinding fromDTO(io.harness.accesscontrol.rolebindings.RoleBindingDTO dto) {
    return RoleBinding.builder()
        .identifier(dto.getIdentifier())
        .parentIdentifier(dto.getParentIdentifier())
        .resourceGroupIdentifier(dto.getResourceGroupIdentifier())
        .principalIdentifier(dto.getPrincipalIdentifier())
        .principalType(dto.getPrincipalType())
        .roleIdentifier(dto.getRoleIdentifier())
        .isDefault(dto.isDefault())
        .isDisabled(dto.isDisabled())
        .version(dto.getVersion())
        .build();
  }

  public static io.harness.accesscontrol.rolebindings.RoleBindingDTO toDTO(RoleBinding object) {
    return RoleBindingDTO.builder()
        .identifier(object.getIdentifier())
        .parentIdentifier(object.getParentIdentifier())
        .resourceGroupIdentifier(object.getResourceGroupIdentifier())
        .principalIdentifier(object.getPrincipalIdentifier())
        .principalType(object.getPrincipalType())
        .roleIdentifier(object.getRoleIdentifier())
        .isDefault(object.isDefault())
        .isDisabled(object.isDisabled())
        .version(object.getVersion())
        .build();
  }
}
