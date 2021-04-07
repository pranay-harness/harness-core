package io.harness.ng.accesscontrol.user;

import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.remote.client.NGRestUtils.getResponse;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentAggregateResponseDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentFilterDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.accesscontrol.roles.api.RoleResponseDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.accesscontrol.user.remote.ACLAggregateFilter;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.invites.api.InviteService;
import io.harness.ng.core.invites.dto.UserSearchDTO;
import io.harness.ng.core.invites.remote.RoleBinding;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PL)
public class UserServiceImpl implements UserService {
  private static final int DEFAULT_PAGE_SIZE = 1000;
  AccessControlAdminClient accessControlAdminClient;
  NgUserService ngUserService;
  InviteService inviteService;
  ResourceGroupClient resourceGroupClient;

  @Override
  public PageResponse<UserAggregateDTO> getUsers(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, PageRequest pageRequest,
      io.harness.ng.accesscontrol.user.remote.ACLAggregateFilter aclAggregateFilter) {
    validateRequest(searchTerm, aclAggregateFilter);
    if (io.harness.ng.accesscontrol.user.remote.ACLAggregateFilter.isFilterApplied(aclAggregateFilter)) {
      return getFilteredUsers(accountIdentifier, orgIdentifier, projectIdentifier, pageRequest, aclAggregateFilter);
    }
    return getUnfilteredUsersPage(accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, pageRequest);
  }

  private void validateRequest(
      String searchTerm, io.harness.ng.accesscontrol.user.remote.ACLAggregateFilter aclAggregateFilter) {
    if (!isBlank(searchTerm)
        && io.harness.ng.accesscontrol.user.remote.ACLAggregateFilter.isFilterApplied(aclAggregateFilter)) {
      log.error("Search term and filter on role/resourcegroup identifiers can't be applied at the same time");
      throw new InvalidRequestException(
          "Search term and filter on role/resourcegroup identifiers can't be applied at the same time");
    }
  }

  private PageResponse<UserAggregateDTO> getFilteredUsers(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, PageRequest pageRequest,
      io.harness.ng.accesscontrol.user.remote.ACLAggregateFilter aclAggregateFilter) {
    RoleAssignmentAggregateResponseDTO roleAssignmentAggregateResponseDTO =
        getRoleAssignments(accountIdentifier, orgIdentifier, projectIdentifier, aclAggregateFilter);
    Map<String, List<RoleBinding>> userRoleAssignmentsMap =
        getUserRoleAssignmentMap(roleAssignmentAggregateResponseDTO);
    List<UserSearchDTO> users =
        getUsersForFilteredUsersPage(new ArrayList<>(userRoleAssignmentsMap.keySet()), accountIdentifier, pageRequest);
    List<UserAggregateDTO> userAggregateDTOS =
        users.stream()
            .map(user
                -> UserAggregateDTO.builder()
                       .roleBindings(userRoleAssignmentsMap.getOrDefault(user.getUuid(), Collections.emptyList()))
                       .user(user)
                       .build())
            .collect(toList());
    return PageResponse.<UserAggregateDTO>builder()
        .totalPages((int) Math.ceil((double) userRoleAssignmentsMap.size() / pageRequest.getPageSize()))
        .totalItems(userRoleAssignmentsMap.size())
        .pageItemCount(userAggregateDTOS.size())
        .content(userAggregateDTOS)
        .pageSize(pageRequest.getPageSize())
        .pageIndex(pageRequest.getPageIndex())
        .empty(userAggregateDTOS.isEmpty())
        .build();
  }

  private RoleAssignmentAggregateResponseDTO getRoleAssignments(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, ACLAggregateFilter aclAggregateFilter) {
    RoleAssignmentFilterDTO roleAssignmentFilterDTO =
        RoleAssignmentFilterDTO.builder()
            .roleFilter(aclAggregateFilter.getRoleIdentifiers())
            .resourceGroupFilter(aclAggregateFilter.getResourceGroupIdentifiers())
            .build();
    return getResponse(accessControlAdminClient.getAggregatedFilteredRoleAssignments(
        accountIdentifier, orgIdentifier, projectIdentifier, roleAssignmentFilterDTO));
  }

  private List<UserSearchDTO> getUsersForFilteredUsersPage(
      List<String> userIds, String accountIdentifier, PageRequest pageRequest) {
    int lowIdx = pageRequest.getPageIndex() * pageRequest.getPageSize();
    if (lowIdx < 0 || lowIdx >= userIds.size()) {
      return Collections.emptyList();
    }
    int highIdx = Math.min(lowIdx + pageRequest.getPageSize(), userIds.size());
    List<String> userIdPage = userIds.subList(lowIdx, highIdx);
    List<UserInfo> users = ngUserService.getUsersByIds(userIdPage, accountIdentifier);
    return users.stream()
        .map(user -> UserSearchDTO.builder().uuid(user.getUuid()).name(user.getName()).email(user.getEmail()).build())
        .collect(toList());
  }

  private PageResponse<UserAggregateDTO> getUnfilteredUsersPage(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, PageRequest pageRequest) {
    PageResponse<UserSearchDTO> userPage =
        getUsersFromUnfilteredUsersPage(accountIdentifier, orgIdentifier, projectIdentifier, pageRequest, searchTerm);
    Set<PrincipalDTO> principalDTOs =
        userPage.getContent()
            .stream()
            .map(userSearch -> PrincipalDTO.builder().identifier(userSearch.getUuid()).type(USER).build())
            .collect(Collectors.toSet());

    RoleAssignmentFilterDTO roleAssignmentFilterDTO =
        RoleAssignmentFilterDTO.builder().principalFilter(principalDTOs).build();
    RoleAssignmentAggregateResponseDTO roleAssignmentAggregateResponseDTO =
        getResponse(accessControlAdminClient.getAggregatedFilteredRoleAssignments(
            accountIdentifier, orgIdentifier, projectIdentifier, roleAssignmentFilterDTO));
    Map<String, List<RoleBinding>> userRoleAssignmentsMap =
        getUserRoleAssignmentMap(roleAssignmentAggregateResponseDTO);
    List<UserAggregateDTO> userAggregateDTOS =
        userPage.getContent()
            .stream()
            .map(user
                -> UserAggregateDTO.builder()
                       .roleBindings(userRoleAssignmentsMap.getOrDefault(user.getUuid(), Collections.emptyList()))
                       .user(user)
                       .build())
            .collect(toList());
    return PageUtils.getNGPageResponse(userPage, userAggregateDTOS);
  }

  private PageResponse<UserSearchDTO> getUsersFromUnfilteredUsersPage(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, PageRequest pageRequest, String searchTerm) {
    List<String> userIds = ngUserService.listUsersAtScope(accountIdentifier, orgIdentifier, projectIdentifier);
    Page<UserInfo> users = ngUserService.list(
        accountIdentifier, searchTerm, org.springframework.data.domain.PageRequest.of(0, DEFAULT_PAGE_SIZE));
    Set<String> userIdsSet = new HashSet<>(userIds);
    List<UserSearchDTO> allFilteredUsers =
        users.stream()
            .filter(user -> userIdsSet.contains(user.getUuid()))
            .map(user
                -> UserSearchDTO.builder().uuid(user.getUuid()).name(user.getName()).email(user.getEmail()).build())
            .collect(Collectors.toList());
    int lowIdx = pageRequest.getPageIndex() * pageRequest.getPageSize();
    if (lowIdx < 0 || lowIdx >= allFilteredUsers.size()) {
      return PageResponse.<UserSearchDTO>builder()
          .totalPages((int) Math.ceil((double) allFilteredUsers.size() / pageRequest.getPageSize()))
          .totalItems(allFilteredUsers.size())
          .pageItemCount(0)
          .content(Collections.emptyList())
          .pageSize(pageRequest.getPageSize())
          .pageIndex(pageRequest.getPageIndex())
          .empty(true)
          .build();
    }
    int highIdx = Math.min(lowIdx + pageRequest.getPageSize(), allFilteredUsers.size());
    List<UserSearchDTO> usersPage = allFilteredUsers.subList(lowIdx, highIdx);
    return PageResponse.<UserSearchDTO>builder()
        .totalPages((int) Math.ceil((double) allFilteredUsers.size() / pageRequest.getPageSize()))
        .totalItems(allFilteredUsers.size())
        .pageItemCount(usersPage.size())
        .content(usersPage)
        .pageSize(pageRequest.getPageSize())
        .pageIndex(pageRequest.getPageIndex())
        .empty(usersPage.isEmpty())
        .build();
  }

  @Override
  public boolean deleteUser(String accountIdentifier, String orgIdentifier, String projectIdentifier, String userId) {
    List<RoleAssignmentResponseDTO> roleAssignments = getResponse(
        accessControlAdminClient.getFilteredRoleAssignments(accountIdentifier, orgIdentifier, projectIdentifier, 0,
            DEFAULT_PAGE_SIZE,
            RoleAssignmentFilterDTO.builder()
                .principalFilter(Collections.singleton(PrincipalDTO.builder().identifier(userId).type(USER).build()))
                .build()))
                                                          .getContent();
    boolean successfullyDeleted = true;
    for (RoleAssignmentResponseDTO assignment : roleAssignments) {
      RoleAssignmentDTO roleAssignment = assignment.getRoleAssignment();
      RoleAssignmentResponseDTO roleAssignmentResponseDTO = getResponse(accessControlAdminClient.deleteRoleAssignment(
          roleAssignment.getIdentifier(), accountIdentifier, orgIdentifier, projectIdentifier));
      successfullyDeleted = successfullyDeleted && Objects.nonNull(roleAssignmentResponseDTO);
    }
    if (successfullyDeleted) {
      ngUserService.removeUserFromScope(userId, accountIdentifier, orgIdentifier, projectIdentifier);
    }
    return successfullyDeleted;
  }

  private Map<String, List<RoleBinding>> getUserRoleAssignmentMap(
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
                -> RoleBinding.builder()
                       .roleIdentifier(roleAssignment.getRoleIdentifier())
                       .resourceGroupIdentifier(roleAssignment.getResourceGroupIdentifier())
                       .roleName(roleMap.get(roleAssignment.getRoleIdentifier()).getRole().getName())
                       .resourceGroupName(resourceGroupMap.get(roleAssignment.getResourceGroupIdentifier()).getName())
                       .managedRole(roleMap.get(roleAssignment.getRoleIdentifier()).isHarnessManaged())
                       .build(),
                toList())));
  }
}
