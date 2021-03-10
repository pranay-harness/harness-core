package io.harness.accesscontrol.roleassignments.api;

import io.harness.accesscontrol.principals.Principal;
import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.accesscontrol.roleassignments.RoleAssignmentFilter;

import java.util.HashSet;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RoleAssignmentDTOMapper {
  public static RoleAssignmentResponseDTO toDTO(RoleAssignment object) {
    return RoleAssignmentResponseDTO.builder()
        .roleAssignment(RoleAssignmentDTO.builder()
                            .identifier(object.getIdentifier())
                            .principal(PrincipalDTO.builder()
                                           .identifier(object.getPrincipalIdentifier())
                                           .type(object.getPrincipalType())
                                           .build())
                            .resourceGroupIdentifier(object.getResourceGroupIdentifier())
                            .roleIdentifier(object.getRoleIdentifier())
                            .harnessManaged(object.isManaged())
                            .disabled(object.isDisabled())
                            .build())
        .scope(object.getScopeIdentifier())
        .createdAt(object.getCreatedAt())
        .lastModifiedAt(object.getLastModifiedAt())
        .build();
  }

  public static RoleAssignment fromDTO(String scopeIdentifier, RoleAssignmentDTO object) {
    return RoleAssignment.builder()
        .identifier(object.getIdentifier())
        .scopeIdentifier(scopeIdentifier)
        .principalIdentifier(object.getPrincipal().getIdentifier())
        .principalType(object.getPrincipal().getType())
        .resourceGroupIdentifier(object.getResourceGroupIdentifier())
        .roleIdentifier(object.getRoleIdentifier())
        .managed(object.isHarnessManaged())
        .disabled(object.isDisabled())
        .build();
  }

  public static RoleAssignmentFilter fromDTO(RoleAssignmentFilterDTO object) {
    return RoleAssignmentFilter.builder()
        .roleFilter(object.getRoleFilter() == null ? new HashSet<>() : object.getRoleFilter())
        .resourceGroupFilter(
            object.getResourceGroupFilter() == null ? new HashSet<>() : object.getResourceGroupFilter())
        .principalFilter(object.getPrincipalFilter() == null
                ? new HashSet<>()
                : object.getPrincipalFilter()
                      .stream()
                      .map(principalDTO
                          -> Principal.builder()
                                 .principalType(principalDTO.getType())
                                 .principalIdentifier(principalDTO.getIdentifier())
                                 .build())
                      .collect(Collectors.toSet()))
        .principalTypeFilter(
            object.getPrincipalTypeFilter() == null ? new HashSet<>() : object.getPrincipalTypeFilter())
        .managedFilter(object.getHarnessManagedFilter() == null ? new HashSet<>() : object.getHarnessManagedFilter())
        .disabledFilter(object.getDisabledFilter() == null ? new HashSet<>() : object.getDisabledFilter())
        .build();
  }
}
