package io.harness.ng.accesscontrol.user;

import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.remote.client.NGRestUtils.getResponse;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentAggregateResponseDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentFilterDTO;
import io.harness.accesscontrol.roles.api.RoleResponseDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.RoleAssignmentMetadataDTO;
import io.harness.ng.core.invites.dto.UserMetadataDTO;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.remote.dto.UserAggregateDTO;
import io.harness.ng.core.user.remote.dto.UserFilter;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PL)
public class AggregateUserServiceImpl implements AggregateUserService {
  AccessControlAdminClient accessControlAdminClient;
  NgUserService ngUserService;

  @Override
  public PageResponse<UserAggregateDTO> getAggregatedUsers(Scope scope, String searchTerm, PageRequest pageRequest) {
    PageResponse<UserMetadataDTO> userPage =
        ngUserService.listUsers(scope, pageRequest, UserFilter.builder().name(searchTerm).mail(searchTerm).build());
    Set<PrincipalDTO> principalDTOs =
        userPage.getContent()
            .stream()
            .map(user -> PrincipalDTO.builder().identifier(user.getUuid()).type(USER).build())
            .collect(Collectors.toSet());

    RoleAssignmentFilterDTO roleAssignmentFilter =
        RoleAssignmentFilterDTO.builder().principalFilter(principalDTOs).build();
    RoleAssignmentAggregateResponseDTO roleAssignmentResponses =
        getResponse(accessControlAdminClient.getAggregatedFilteredRoleAssignments(scope.getAccountIdentifier(),
            scope.getOrgIdentifier(), scope.getProjectIdentifier(), roleAssignmentFilter));

    Map<String, List<RoleAssignmentMetadataDTO>> userRoleAssignmentsMap =
        getUserRoleAssignmentMap(roleAssignmentResponses);
    List<UserAggregateDTO> userAggregateList =
        userPage.getContent()
            .stream()
            .map(user
                -> UserAggregateDTO.builder()
                       .roleAssignmentMetadata(
                           userRoleAssignmentsMap.getOrDefault(user.getUuid(), Collections.emptyList()))
                       .user(user)
                       .build())
            .collect(toList());
    return PageUtils.getNGPageResponse(userPage, userAggregateList);
  }

  @Override
  public PageResponse<UserAggregateDTO> getAggregatedUsers(
      Scope scope, ACLAggregateFilter aclAggregateFilter, PageRequest pageRequest) {
    RoleAssignmentAggregateResponseDTO roleAssignmentResponses = getRoleAssignments(
        scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier(), aclAggregateFilter);
    Map<String, List<RoleAssignmentMetadataDTO>> userRoleAssignmentsMap =
        getUserRoleAssignmentMap(roleAssignmentResponses);

    Set<String> userIds = userRoleAssignmentsMap.keySet();
    PageResponse<UserMetadataDTO> userPage =
        ngUserService.listUsers(scope, pageRequest, UserFilter.builder().identifiers(userIds).build());

    List<UserAggregateDTO> userAggregateList =
        userPage.getContent()
            .stream()
            .map(user
                -> UserAggregateDTO.builder()
                       .roleAssignmentMetadata(
                           userRoleAssignmentsMap.getOrDefault(user.getUuid(), Collections.emptyList()))
                       .user(user)
                       .build())
            .collect(toList());
    return PageUtils.getNGPageResponse(userPage, userAggregateList);
  }

  @Override
  public UserAggregateDTO getAggregatedUser(Scope scope, String userId) {
    Optional<UserInfo> userInfoOptional = ngUserService.getUserById(userId);
    if (!userInfoOptional.isPresent()) {
      return null;
    }
    UserInfo userInfo = userInfoOptional.get();
    UserMetadataDTO user =
        UserMetadataDTO.builder().uuid(userInfo.getUuid()).name(userInfo.getName()).email(userInfo.getEmail()).build();

    RoleAssignmentFilterDTO roleAssignmentFilterDTO =
        RoleAssignmentFilterDTO.builder()
            .principalFilter(Collections.singleton(PrincipalDTO.builder().identifier(userId).type(USER).build()))
            .build();
    RoleAssignmentAggregateResponseDTO roleAssignmentResponse =
        getResponse(accessControlAdminClient.getAggregatedFilteredRoleAssignments(scope.getAccountIdentifier(),
            scope.getOrgIdentifier(), scope.getProjectIdentifier(), roleAssignmentFilterDTO));

    List<RoleAssignmentMetadataDTO> roleAssignmentMetadata =
        getUserRoleAssignmentMap(roleAssignmentResponse).getOrDefault(userId, Collections.emptyList());
    return UserAggregateDTO.builder().roleAssignmentMetadata(roleAssignmentMetadata).user(user).build();
  }

  private RoleAssignmentAggregateResponseDTO getRoleAssignments(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, ACLAggregateFilter aclAggregateFilter) {
    RoleAssignmentFilterDTO roleAssignmentFilterDTO =
        RoleAssignmentFilterDTO.builder()
            .roleFilter(aclAggregateFilter.getRoleIdentifiers())
            .resourceGroupFilter(aclAggregateFilter.getResourceGroupIdentifiers())
            .principalTypeFilter(Collections.singleton(USER))
            .build();
    return getResponse(accessControlAdminClient.getAggregatedFilteredRoleAssignments(
        accountIdentifier, orgIdentifier, projectIdentifier, roleAssignmentFilterDTO));
  }

  private Map<String, List<RoleAssignmentMetadataDTO>> getUserRoleAssignmentMap(
      RoleAssignmentAggregateResponseDTO roleAssignmentAggregateResponseDTO) {
    Map<String, RoleResponseDTO> roleMap = roleAssignmentAggregateResponseDTO.getRoles().stream().collect(
        toMap(e -> e.getRole().getIdentifier(), Function.identity()));
    Map<String, io.harness.accesscontrol.resourcegroups.api.ResourceGroupDTO> resourceGroupMap =
        roleAssignmentAggregateResponseDTO.getResourceGroups().stream().collect(
            toMap(io.harness.accesscontrol.resourcegroups.api.ResourceGroupDTO::getIdentifier, Function.identity()));
    return roleAssignmentAggregateResponseDTO.getRoleAssignments()
        .stream()
        .filter(roleAssignmentDTO
            -> roleMap.containsKey(roleAssignmentDTO.getRoleIdentifier())
                && resourceGroupMap.containsKey(roleAssignmentDTO.getResourceGroupIdentifier()))
        .collect(Collectors.groupingBy(roleAssignment
            -> roleAssignment.getPrincipal().getIdentifier(),
            mapping(roleAssignment
                -> RoleAssignmentMetadataDTO.builder()
                       .identifier(roleAssignment.getIdentifier())
                       .roleIdentifier(roleAssignment.getRoleIdentifier())
                       .resourceGroupIdentifier(roleAssignment.getResourceGroupIdentifier())
                       .roleName(roleMap.get(roleAssignment.getRoleIdentifier()).getRole().getName())
                       .resourceGroupName(resourceGroupMap.get(roleAssignment.getResourceGroupIdentifier()).getName())
                       .managedRole(roleMap.get(roleAssignment.getRoleIdentifier()).isHarnessManaged())
                       .managedRoleAssignment(roleAssignment.isManaged())
                       .build(),
                toList())));
  }
}
