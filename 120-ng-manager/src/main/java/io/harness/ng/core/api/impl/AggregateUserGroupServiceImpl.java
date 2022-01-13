/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.api.impl;

import static io.harness.accesscontrol.principals.PrincipalType.USER_GROUP;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
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
import io.harness.ng.core.user.entities.UserGroup;
import io.harness.ng.core.user.remote.dto.UserMetadataDTO;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.ng.core.utils.UserGroupMapper;
import io.harness.utils.PageUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
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
      String orgIdentifier, String projectIdentifier, String searchTerm, int userSize) {
    Page<UserGroup> userGroupPageResponse = userGroupService.list(
        accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, getPageRequest(pageRequest));

    List<String> userIdentifiers = userGroupPageResponse.stream()
                                       .map(ug -> getLastNElementsReversed(ug.getUsers(), userSize))
                                       .flatMap(List::stream)
                                       .filter(Objects::nonNull)
                                       .distinct()
                                       .collect(Collectors.toList());
    Map<String, UserMetadataDTO> userMetadataMap =
        ngUserService.getUserMetadata(userIdentifiers)
            .stream()
            .collect(Collectors.toMap(UserMetadataDTO::getUuid, Function.identity()));

    Set<PrincipalDTO> principalDTOSet =
        userGroupPageResponse.stream()
            .map(userGroup -> PrincipalDTO.builder().identifier(userGroup.getIdentifier()).type(USER_GROUP).build())
            .collect(Collectors.toSet());
    RoleAssignmentFilterDTO roleAssignmentFilterDTO =
        RoleAssignmentFilterDTO.builder().principalFilter(principalDTOSet).build();
    Map<String, List<RoleAssignmentMetadataDTO>> userGroupRoleAssignmentsMap =
        getPrincipalRoleAssignmentMap(accountIdentifier, orgIdentifier, projectIdentifier, roleAssignmentFilterDTO);

    return PageUtils.getNGPageResponse(userGroupPageResponse.map(userGroup -> {
      List<UserMetadataDTO> users = getLastNElementsReversed(userGroup.getUsers(), userSize)
                                        .stream()
                                        .map(userMetadataMap::get)
                                        .filter(Objects::nonNull)
                                        .collect(toList());
      return UserGroupAggregateDTO.builder()
          .userGroupDTO(UserGroupMapper.toDTO(userGroup))
          .roleAssignmentsMetadataDTO(userGroupRoleAssignmentsMap.get(userGroup.getIdentifier()))
          .users(users)
          .lastModifiedAt(userGroup.getLastModifiedAt())
          .build();
    }));
  }

  public static <T> List<T> getLastNElementsReversed(List<T> list, int n) {
    List<T> result = list.subList(Math.max(list.size() - n, 0), list.size());
    return Lists.reverse(result);
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

    List<String> userIdentifiers = userGroups.stream()
                                       .map(UserGroup::getUsers)
                                       .flatMap(List::stream)
                                       .filter(Objects::nonNull)
                                       .distinct()
                                       .collect(toList());

    Map<String, UserMetadataDTO> userMetadataMap =
        ngUserService.getUserMetadata(userIdentifiers)
            .stream()
            .collect(Collectors.toMap(UserMetadataDTO::getUuid, Function.identity()));

    return userGroups.stream()
        .map(userGroup -> {
          List<UserMetadataDTO> users =
              userGroup.getUsers().stream().map(userMetadataMap::get).filter(Objects::nonNull).collect(toList());
          return UserGroupAggregateDTO.builder()
              .userGroupDTO(UserGroupMapper.toDTO(userGroup))
              .roleAssignmentsMetadataDTO(userGroupRoleAssignmentsMap.get(userGroup.getIdentifier()))
              .users(users)
              .lastModifiedAt(userGroup.getLastModifiedAt())
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

    List<UserMetadataDTO> users = isEmpty(userGroupOpt.get().getUsers())
        ? Collections.emptyList()
        : ngUserService.getUserMetadata(userGroupOpt.get().getUsers());

    return UserGroupAggregateDTO.builder()
        .userGroupDTO(UserGroupMapper.toDTO(userGroupOpt.get()))
        .roleAssignmentsMetadataDTO(userGroupRoleAssignmentsMap.get(userGroupIdentifier))
        .users(users)
        .lastModifiedAt(userGroupOpt.get().getLastModifiedAt())
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
