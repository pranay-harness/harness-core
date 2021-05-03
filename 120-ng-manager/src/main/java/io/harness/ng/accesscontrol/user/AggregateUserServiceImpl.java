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
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentFilterDTO;
import io.harness.accesscontrol.roles.api.RoleResponseDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.invites.api.InviteService;
import io.harness.ng.core.invites.dto.UserMetadataDTO;
import io.harness.ng.core.invites.remote.RoleBinding;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.remote.dto.UserAggregateDTO;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PL)
public class AggregateUserServiceImpl implements AggregateUserService {
  private static final int DEFAULT_PAGE_SIZE = 1000;
  AccessControlAdminClient accessControlAdminClient;
  NgUserService ngUserService;
  InviteService inviteService;
  ResourceGroupClient resourceGroupClient;

  @Override
  public PageResponse<UserAggregateDTO> getAggregatedUsers(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, PageRequest pageRequest, ACLAggregateFilter aclAggregateFilter) {
    validateRequest(searchTerm, aclAggregateFilter);
    if (ACLAggregateFilter.isFilterApplied(aclAggregateFilter)) {
      return getFilteredUsers(accountIdentifier, orgIdentifier, projectIdentifier, pageRequest, aclAggregateFilter);
    }
    return getUnfilteredUsersPage(accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, pageRequest);
  }

  @Override
  public UserAggregateDTO getAggregatedUser(
      String userId, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
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
        getResponse(accessControlAdminClient.getAggregatedFilteredRoleAssignments(
            accountIdentifier, orgIdentifier, projectIdentifier, roleAssignmentFilterDTO));
    List<RoleBinding> roleBindings =
        getUserRoleAssignmentMap(roleAssignmentResponse).getOrDefault(userId, Collections.emptyList());

    return UserAggregateDTO.builder().roleBindings(roleBindings).user(user).build();
  }

  private void validateRequest(String searchTerm, ACLAggregateFilter aclAggregateFilter) {
    if (!isBlank(searchTerm) && ACLAggregateFilter.isFilterApplied(aclAggregateFilter)) {
      log.error("Search term and filter on role/resourcegroup identifiers can't be applied at the same time");
      throw new InvalidRequestException(
          "Search term and filter on role/resourcegroup identifiers can't be applied at the same time");
    }
  }

  private PageResponse<UserAggregateDTO> getFilteredUsers(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, PageRequest pageRequest, ACLAggregateFilter aclAggregateFilter) {
    RoleAssignmentAggregateResponseDTO roleAssignmentAggregateResponseDTO =
        getRoleAssignments(accountIdentifier, orgIdentifier, projectIdentifier, aclAggregateFilter);
    Map<String, List<RoleBinding>> userRoleAssignmentsMap =
        getUserRoleAssignmentMap(roleAssignmentAggregateResponseDTO);
    List<UserMetadataDTO> users =
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

  private List<UserMetadataDTO> getUsersForFilteredUsersPage(
      List<String> userIds, String accountIdentifier, PageRequest pageRequest) {
    int lowIdx = pageRequest.getPageIndex() * pageRequest.getPageSize();
    if (lowIdx < 0 || lowIdx >= userIds.size()) {
      return Collections.emptyList();
    }
    int highIdx = Math.min(lowIdx + pageRequest.getPageSize(), userIds.size());
    List<String> userIdPage = userIds.subList(lowIdx, highIdx);
    List<UserInfo> users = ngUserService.getUsersByIds(userIdPage, accountIdentifier);
    return users.stream()
        .map(user -> UserMetadataDTO.builder().uuid(user.getUuid()).name(user.getName()).email(user.getEmail()).build())
        .collect(toList());
  }

  private PageResponse<UserAggregateDTO> getUnfilteredUsersPage(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, PageRequest pageRequest) {
    PageResponse<UserMetadataDTO> userPage =
        getUsersForUnfilteredUsersPage(accountIdentifier, orgIdentifier, projectIdentifier, pageRequest, searchTerm);
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

  private PageResponse<UserMetadataDTO> getUsersForUnfilteredUsersPage(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, PageRequest pageRequest, String searchTerm) {
    List<String> userIds = ngUserService.listUserIds(Scope.builder()
                                                         .accountIdentifier(accountIdentifier)
                                                         .orgIdentifier(orgIdentifier)
                                                         .projectIdentifier(projectIdentifier)
                                                         .build());
    Page<UserInfo> users = ngUserService.listCurrentGenUsers(
        accountIdentifier, searchTerm, org.springframework.data.domain.PageRequest.of(0, DEFAULT_PAGE_SIZE));
    Set<String> userIdsSet = new HashSet<>(userIds);
    List<UserMetadataDTO> allFilteredUsers =
        users.stream()
            .filter(user -> userIdsSet.contains(user.getUuid()))
            .map(user
                -> UserMetadataDTO.builder().uuid(user.getUuid()).name(user.getName()).email(user.getEmail()).build())
            .collect(Collectors.toList());
    int lowIdx = pageRequest.getPageIndex() * pageRequest.getPageSize();
    if (lowIdx < 0 || lowIdx >= allFilteredUsers.size()) {
      return PageResponse.<UserMetadataDTO>builder()
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
    List<UserMetadataDTO> usersPage = allFilteredUsers.subList(lowIdx, highIdx);
    return PageResponse.<UserMetadataDTO>builder()
        .totalPages((int) Math.ceil((double) allFilteredUsers.size() / pageRequest.getPageSize()))
        .totalItems(allFilteredUsers.size())
        .pageItemCount(usersPage.size())
        .content(usersPage)
        .pageSize(pageRequest.getPageSize())
        .pageIndex(pageRequest.getPageIndex())
        .empty(usersPage.isEmpty())
        .build();
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
                       .identifier(roleAssignment.getIdentifier())
                       .roleIdentifier(roleAssignment.getRoleIdentifier())
                       .resourceGroupIdentifier(roleAssignment.getResourceGroupIdentifier())
                       .roleName(roleMap.get(roleAssignment.getRoleIdentifier()).getRole().getName())
                       .resourceGroupName(resourceGroupMap.get(roleAssignment.getResourceGroupIdentifier()).getName())
                       .managedRole(roleMap.get(roleAssignment.getRoleIdentifier()).isHarnessManaged())
                       .build(),
                toList())));
  }
}
