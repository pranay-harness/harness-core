package io.harness.ng.core.api.impl;

import static io.harness.accesscontrol.principals.PrincipalType.USER_GROUP;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.remote.client.NGRestUtils.getResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.resourcegroups.api.ResourceGroupDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentAggregateResponseDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentFilterDTO;
import io.harness.accesscontrol.roles.api.RoleResponseDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.api.AggregateUserGroupService;
import io.harness.ng.core.api.UserGroupService;
import io.harness.ng.core.dto.AggregateACLRequest;
import io.harness.ng.core.dto.RoleAssignmentMetadataDTO;
import io.harness.ng.core.dto.UserGroupAggregateDTO;
import io.harness.ng.core.dto.UserGroupFilterDTO;
import io.harness.ng.core.entities.UserGroup;
import io.harness.ng.core.invites.dto.UserSearchDTO;
import io.harness.ng.core.invites.remote.UserSearchMapper;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.ng.core.utils.UserGroupMapper;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;
import org.springframework.data.domain.Page;

@OwnedBy(PL)
@Singleton
@ValidateOnExecution
public class AggregateUserGroupServiceImpl implements AggregateUserGroupService {
  private final UserGroupService userGroupService;
  private final AccessControlAdminClient accessControlAdminClient;
  private final NgUserService ngUserService;

  @Inject
  public AggregateUserGroupServiceImpl(UserGroupService userGroupService,
      AccessControlAdminClient accessControlAdminClient, NgUserService ngUserService) {
    this.userGroupService = userGroupService;
    this.accessControlAdminClient = accessControlAdminClient;
    this.ngUserService = ngUserService;
  }

  @Override
  public PageResponse<UserGroupAggregateDTO> listAggregateUserGroups(PageRequest pageRequest, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String searchTerm) {
    Page<UserGroup> userGroupPageResponse = userGroupService.list(
        accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, getPageRequest(pageRequest));

    Set<PrincipalDTO> principalDTOSet =
        userGroupPageResponse.stream()
            .map(userGroup -> PrincipalDTO.builder().identifier(userGroup.getIdentifier()).type(USER_GROUP).build())
            .collect(Collectors.toSet());
    RoleAssignmentFilterDTO roleAssignmentFilterDTO =
        RoleAssignmentFilterDTO.builder().principalFilter(principalDTOSet).build();

    Map<String, List<RoleAssignmentMetadataDTO>> userGroupRoleAssignmentsMap =
        getPrincipalRoleAssignmentMap(accountIdentifier, orgIdentifier, projectIdentifier, roleAssignmentFilterDTO);

    Set<String> userIdentifiers =
        userGroupPageResponse.stream().map(UserGroup::getUsers).reduce(new HashSet<>(), (total, subset) -> {
          total.addAll(subset);
          return total;
        });

    Map<String, UserSearchDTO> userSearchDTOMap =
        ngUserService.getUsersByIds(new ArrayList<>(userIdentifiers))
            .stream()
            .map(UserSearchMapper::writeDTO)
            .collect(Collectors.toMap(UserSearchDTO::getUuid, Function.identity()));

    return PageUtils.getNGPageResponse(userGroupPageResponse.map(userGroup -> {
      List<UserSearchDTO> users =
          userGroup.getUsers().stream().map(userSearchDTOMap::get).filter(Objects::nonNull).collect(toList());
      return UserGroupAggregateDTO.builder()
          .userGroupDTO(UserGroupMapper.toDTO(userGroup))
          .roleAssignmentsMetadataDTO(userGroupRoleAssignmentsMap.get(userGroup.getIdentifier()))
          .users(users)
          .build();
    }));
  }

  @Override
  public List<UserGroupAggregateDTO> listAggregateUserGroups(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, AggregateACLRequest aggregateACLRequest) {
    RoleAssignmentFilterDTO roleAssignmentFilterDTO =
        RoleAssignmentFilterDTO.builder()
            .roleFilter(aggregateACLRequest.getRoleFilter())
            .resourceGroupFilter(aggregateACLRequest.getResourceGroupFilter())
            .principalTypeFilter(Collections.singleton(USER_GROUP))
            .build();

    Map<String, List<RoleAssignmentMetadataDTO>> userGroupRoleAssignmentsMap =
        getPrincipalRoleAssignmentMap(accountIdentifier, orgIdentifier, projectIdentifier, roleAssignmentFilterDTO);

    if (userGroupRoleAssignmentsMap.keySet().isEmpty()) {
      return Collections.emptyList();
    }

    UserGroupFilterDTO userGroupFilterDTO = UserGroupFilterDTO.builder()
                                                .accountIdentifier(accountIdentifier)
                                                .orgIdentifier(orgIdentifier)
                                                .projectIdentifier(projectIdentifier)
                                                .identifierFilter(userGroupRoleAssignmentsMap.keySet())
                                                .build();
    List<UserGroup> userGroups = userGroupService.list(userGroupFilterDTO);

    Set<String> userIdentifiers =
        userGroups.stream().map(UserGroup::getUsers).reduce(new HashSet<>(), (total, subset) -> {
          total.addAll(subset);
          return total;
        });

    Map<String, UserSearchDTO> userSearchDTOMap =
        ngUserService.getUsersByIds(new ArrayList<>(userIdentifiers))
            .stream()
            .map(UserSearchMapper::writeDTO)
            .collect(Collectors.toMap(UserSearchDTO::getUuid, Function.identity()));

    return userGroups.stream()
        .map(userGroup -> {
          List<UserSearchDTO> users =
              userGroup.getUsers().stream().map(userSearchDTOMap::get).filter(Objects::nonNull).collect(toList());
          return UserGroupAggregateDTO.builder()
              .userGroupDTO(UserGroupMapper.toDTO(userGroup))
              .roleAssignmentsMetadataDTO(userGroupRoleAssignmentsMap.get(userGroup.getIdentifier()))
              .users(users)
              .build();
        })
        .collect(toList());
  }

  @Override
  public UserGroupAggregateDTO getAggregatedUserGroup(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String userGroupIdentifier) {
    Optional<UserGroup> userGroupOpt =
        userGroupService.get(accountIdentifier, orgIdentifier, projectIdentifier, userGroupIdentifier);
    if (!userGroupOpt.isPresent()) {
      throw new InvalidRequestException(String.format("User Group is not available %s:%s:%s:%s", accountIdentifier,
          orgIdentifier, projectIdentifier, userGroupIdentifier));
    }
    PrincipalDTO principalDTO = PrincipalDTO.builder().identifier(userGroupIdentifier).type(USER_GROUP).build();
    RoleAssignmentFilterDTO roleAssignmentFilterDTO =
        RoleAssignmentFilterDTO.builder().principalFilter(Collections.singleton(principalDTO)).build();
    Map<String, List<RoleAssignmentMetadataDTO>> userGroupRoleAssignmentsMap =
        getPrincipalRoleAssignmentMap(accountIdentifier, orgIdentifier, projectIdentifier, roleAssignmentFilterDTO);

    Set<String> userIdentifiers =
        userGroupOpt.get().getUsers() == null ? new HashSet<>() : userGroupOpt.get().getUsers();

    List<UserSearchDTO> users = ngUserService.getUsersByIds(new ArrayList<>(userIdentifiers))
                                    .stream()
                                    .map(UserSearchMapper::writeDTO)
                                    .collect(toList());

    return UserGroupAggregateDTO.builder()
        .userGroupDTO(UserGroupMapper.toDTO(userGroupOpt.get()))
        .roleAssignmentsMetadataDTO(userGroupRoleAssignmentsMap.get(userGroupIdentifier))
        .users(users)
        .build();
  }

  private Map<String, List<RoleAssignmentMetadataDTO>> getPrincipalRoleAssignmentMap(String accountIdentifier,
      String orgIdentifier, String projectIdentifier, RoleAssignmentFilterDTO roleAssignmentFilterDTO) {
    RoleAssignmentAggregateResponseDTO roleAssignmentAggregateResponseDTO =
        getResponse(accessControlAdminClient.getAggregatedFilteredRoleAssignments(
            accountIdentifier, orgIdentifier, projectIdentifier, roleAssignmentFilterDTO));

    Map<String, RoleResponseDTO> roleMap = roleAssignmentAggregateResponseDTO.getRoles().stream().collect(
        toMap(e -> e.getRole().getIdentifier(), Function.identity()));
    Map<String, ResourceGroupDTO> resourceGroupMap =
        roleAssignmentAggregateResponseDTO.getResourceGroups().stream().collect(
            toMap(ResourceGroupDTO::getIdentifier, Function.identity()));

    return roleAssignmentAggregateResponseDTO.getRoleAssignments()
        .stream()
        .filter(roleAssignmentDTO
            -> roleMap.containsKey(roleAssignmentDTO.getRoleIdentifier())
                && resourceGroupMap.containsKey(roleAssignmentDTO.getResourceGroupIdentifier()))
        .collect(Collectors.groupingBy(roleAssignment
            -> roleAssignment.getPrincipal().getIdentifier(),
            Collectors.mapping(roleAssignment
                -> RoleAssignmentMetadataDTO.builder()
                       .roleIdentifier(roleAssignment.getRoleIdentifier())
                       .resourceGroupIdentifier(roleAssignment.getResourceGroupIdentifier())
                       .roleName(roleMap.get(roleAssignment.getRoleIdentifier()).getRole().getName())
                       .resourceGroupName(resourceGroupMap.get(roleAssignment.getResourceGroupIdentifier()).getName())
                       .managedRole(roleMap.get(roleAssignment.getRoleIdentifier()).isHarnessManaged())
                       .build(),
                toList())));
  }
}
