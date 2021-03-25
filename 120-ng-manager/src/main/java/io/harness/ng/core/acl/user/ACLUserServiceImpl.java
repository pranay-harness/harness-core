package io.harness.ng.core.acl.user;

import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.ng.core.acl.user.ACLUserAggregateDTO.Status.ACTIVE;
import static io.harness.ng.core.acl.user.ACLUserAggregateDTO.Status.INVITED;
import static io.harness.ng.core.acl.user.ACLUserAggregateDTO.Status.REQUESTED;
import static io.harness.remote.client.NGRestUtils.getResponse;

import static java.lang.Boolean.FALSE;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentAggregateResponseDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentFilterDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.accesscontrol.roles.api.RoleResponseDTO;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.acl.user.ACLUserAggregateDTO.Status;
import io.harness.ng.core.acl.user.remote.ACLAggregateFilter;
import io.harness.ng.core.invites.api.InviteService;
import io.harness.ng.core.invites.dto.UserSearchDTO;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.ng.core.invites.entities.Invite.InviteType;
import io.harness.ng.core.invites.remote.RoleBinding;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroupclient.ResourceGroupResponse;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class ACLUserServiceImpl implements ACLUserService {
  private static final int DEFAULT_PAGE_SIZE = 1000;
  AccessControlAdminClient accessControlAdminClient;
  NgUserService ngUserService;
  InviteService inviteService;
  ResourceGroupClient resourceGroupClient;

  @Override
  public PageResponse<ACLUserAggregateDTO> getActiveUsers(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, PageRequest pageRequest, ACLAggregateFilter aclAggregateFilter) {
    if (ACLAggregateFilter.isFilterApplied(aclAggregateFilter)) {
      return getFilteredActiveUsers(
          accountIdentifier, orgIdentifier, projectIdentifier, pageRequest, aclAggregateFilter);
    } else {
      return getUnfilteredActiveUsers(accountIdentifier, orgIdentifier, projectIdentifier, pageRequest);
    }
  }

  private PageResponse<ACLUserAggregateDTO> getFilteredActiveUsers(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, PageRequest pageRequest, ACLAggregateFilter aclAggregateFilter) {
    RoleAssignmentFilterDTO roleAssignmentFilterDTO =
        RoleAssignmentFilterDTO.builder()
            .roleFilter(aclAggregateFilter.getRoleIdentifiers())
            .resourceGroupFilter(aclAggregateFilter.getResourceGroupIdentifiers())
            .build();
    RoleAssignmentAggregateResponseDTO roleAssignmentAggregateResponseDTO =
        getResponse(accessControlAdminClient.getAggregatedFilteredRoleAssignments(
            accountIdentifier, orgIdentifier, projectIdentifier, roleAssignmentFilterDTO));
    Map<String, List<RoleBinding>> userRoleAssignmentsMap =
        getUserRoleAssignmentMap(roleAssignmentAggregateResponseDTO);
    List<UserSearchDTO> users = getUsersForFilteredActiveUsers(userRoleAssignmentsMap, pageRequest);
    List<ACLUserAggregateDTO> aclUserAggregateDTOs =
        users.stream()
            .map(user
                -> ACLUserAggregateDTO.builder()
                       .roleBindings(userRoleAssignmentsMap.getOrDefault(user.getUuid(), Collections.emptyList()))
                       .status(ACTIVE)
                       .user(user)
                       .build())
            .collect(toList());
    return PageResponse.<ACLUserAggregateDTO>builder()
        .totalPages((int) Math.ceil((double) userRoleAssignmentsMap.size() / pageRequest.getPageSize()))
        .totalItems(userRoleAssignmentsMap.size())
        .pageItemCount(aclUserAggregateDTOs.size())
        .content(aclUserAggregateDTOs)
        .pageSize(pageRequest.getPageSize())
        .pageIndex(pageRequest.getPageIndex())
        .empty(aclUserAggregateDTOs.isEmpty())
        .build();
  }

  private List<UserSearchDTO> getUsersForFilteredActiveUsers(
      Map<String, List<RoleBinding>> userRoleAssignmentsMap, PageRequest pageRequest) {
    List<String> userIds = new ArrayList<>(userRoleAssignmentsMap.keySet());
    Collections.sort(userIds);
    int lowIdx = pageRequest.getPageIndex() * pageRequest.getPageSize();
    if (lowIdx < 0 || lowIdx >= userIds.size()) {
      return Collections.emptyList();
    }
    int highIdx = Math.min(lowIdx + pageRequest.getPageSize(), userIds.size());
    List<String> userIdPage = userIds.subList(lowIdx, highIdx);
    List<UserInfo> users = ngUserService.getUsersByIds(userIdPage);
    return users.stream()
        .map(user -> UserSearchDTO.builder().uuid(user.getUuid()).name(user.getName()).email(user.getEmail()).build())
        .collect(toList());
  }

  private PageResponse<ACLUserAggregateDTO> getUnfilteredActiveUsers(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, PageRequest pageRequest) {
    PageResponse<UserSearchDTO> usersPageResponse =
        ngUserService.listUsersAtScope(accountIdentifier, orgIdentifier, projectIdentifier, pageRequest);
    List<UserSearchDTO> users = usersPageResponse.getContent();
    Set<PrincipalDTO> principalDTOs =
        users.stream()
            .map(userSearch -> PrincipalDTO.builder().identifier(userSearch.getUuid()).type(USER).build())
            .collect(Collectors.toSet());

    RoleAssignmentFilterDTO roleAssignmentFilterDTO =
        RoleAssignmentFilterDTO.builder().principalFilter(principalDTOs).build();
    RoleAssignmentAggregateResponseDTO roleAssignmentAggregateResponseDTO =
        getResponse(accessControlAdminClient.getAggregatedFilteredRoleAssignments(
            accountIdentifier, orgIdentifier, projectIdentifier, roleAssignmentFilterDTO));
    Map<String, List<RoleBinding>> userRoleAssignmentsMap =
        getUserRoleAssignmentMap(roleAssignmentAggregateResponseDTO);
    List<ACLUserAggregateDTO> aclUserAggregateDTOs =
        users.stream()
            .map(user
                -> ACLUserAggregateDTO.builder()
                       .roleBindings(userRoleAssignmentsMap.getOrDefault(user.getUuid(), Collections.emptyList()))
                       .status(ACTIVE)
                       .user(user)
                       .build())
            .collect(toList());
    return PageUtils.getNGPageResponse(usersPageResponse, aclUserAggregateDTOs);
  }

  @Override
  public PageResponse<ACLUserAggregateDTO> getPendingUsers(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, PageRequest pageRequest, ACLAggregateFilter aclAggregateFilter) {
    PageResponse<Invite> invitesPage =
        getInvitePage(accountIdentifier, orgIdentifier, projectIdentifier, pageRequest, aclAggregateFilter);
    List<String> userEmails = invitesPage.getContent().stream().map(Invite::getEmail).collect(toList());
    Map<String, UserSearchDTO> userSearchDTOs = getPendingUserMap(userEmails);
    Map<String, RoleResponseDTO> roleMap =
        getRoleMapForInvites(invitesPage.getContent(), aclAggregateFilter.getRoleIdentifiers());
    Map<String, ResourceGroupDTO> resourceGroupMap =
        getResourceGroupMapForInvites(invitesPage.getContent(), aclAggregateFilter.getResourceGroupIdentifiers());
    List<ACLUserAggregateDTO> aclUserAggregateDTOs =
        aggregatePendingUsers(invitesPage.getContent(), userSearchDTOs, roleMap, resourceGroupMap);
    return PageUtils.getNGPageResponse(invitesPage, aclUserAggregateDTOs);
  }

  private PageResponse<Invite> getInvitePage(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      PageRequest pageRequest, ACLAggregateFilter aclAggregateFilter) {
    Criteria criteria = Criteria.where(Invite.InviteKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(Invite.InviteKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(Invite.InviteKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(Invite.InviteKeys.deleted)
                            .is(FALSE);
    if (ACLAggregateFilter.isFilterApplied(aclAggregateFilter)) {
      if (aclAggregateFilter.getRoleIdentifiers() != null && !aclAggregateFilter.getRoleIdentifiers().isEmpty()) {
        criteria.and(Invite.InviteKeys.roleBindings + "." + RoleBinding.RoleBindingKeys.roleIdentifier)
            .in(aclAggregateFilter.getRoleIdentifiers());
      }
      if (aclAggregateFilter.getResourceGroupIdentifiers() != null
          && !aclAggregateFilter.getResourceGroupIdentifiers().isEmpty()) {
        criteria.and(Invite.InviteKeys.roleBindings + "." + RoleBinding.RoleBindingKeys.resourceGroupIdentifier)
            .in(aclAggregateFilter.getResourceGroupIdentifiers());
      }
    }
    return inviteService.getInvites(criteria, pageRequest);
  }

  private List<ACLUserAggregateDTO> aggregatePendingUsers(List<Invite> invites, Map<String, UserSearchDTO> userMap,
      Map<String, RoleResponseDTO> roleMap, Map<String, ResourceGroupDTO> resourceGroupMap) {
    List<ACLUserAggregateDTO> userAggregateDTOs = new ArrayList<>();
    List<Invite> accumulatedInvites = accumulateRoleAssignments(invites);
    for (Invite invite : accumulatedInvites) {
      List<RoleBinding> roleBindings =
          invite.getRoleBindings()
              .stream()
              .filter(roleAssignmentDTO
                  -> roleMap.containsKey(roleAssignmentDTO.getRoleIdentifier())
                      && resourceGroupMap.containsKey(roleAssignmentDTO.getResourceGroupIdentifier()))
              .map(roleAssignmentDTO
                  -> RoleBinding.builder()
                         .roleIdentifier(roleAssignmentDTO.getRoleIdentifier())
                         .roleName(roleMap.get(roleAssignmentDTO.getRoleIdentifier()).getRole().getName())
                         .resourceGroupIdentifier(roleAssignmentDTO.getResourceGroupIdentifier())
                         .resourceGroupName(
                             resourceGroupMap.get(roleAssignmentDTO.getResourceGroupIdentifier()).getName())
                         .managedRole(roleMap.get(roleAssignmentDTO.getRoleIdentifier()).isHarnessManaged())
                         .build())
              .collect(toList());
      userAggregateDTOs.add(ACLUserAggregateDTO.builder()
                                .user(userMap.get(invite.getEmail()))
                                .roleBindings(roleBindings)
                                .status(getPendingStatus(invite.getInviteType()))
                                .build());
    }
    return userAggregateDTOs;
  }

  private Status getPendingStatus(InviteType inviteType) {
    if (inviteType.equals(InviteType.ADMIN_INITIATED_INVITE)) {
      return INVITED;
    } else if (inviteType.equals(InviteType.USER_INITIATED_INVITE)) {
      return REQUESTED;
    }
    return null;
  }

  private List<Invite> accumulateRoleAssignments(List<Invite> invites) {
    Map<String, Invite> inviteMap = new HashMap<>();
    for (Invite invite : invites) {
      String email = invite.getEmail();
      if (!inviteMap.containsKey(email)) {
        inviteMap.put(email, invite);
      } else {
        inviteMap.get(email).getRoleBindings().addAll(invite.getRoleBindings());
      }
    }
    return new ArrayList<>(inviteMap.values());
  }

  private Map<String, ResourceGroupDTO> getResourceGroupMapForInvites(
      List<Invite> invites, Set<String> filterResourceGroupIdentifiers) {
    if (invites.isEmpty()) {
      return Collections.emptyMap();
    }
    Invite anInvite = invites.get(0);
    Set<String> resourceGroupIds = invites.stream()
                                       .map(Invite::getRoleBindings)
                                       .flatMap(Collection::stream)
                                       .map(RoleBinding::getResourceGroupIdentifier)
                                       .collect(Collectors.toSet());
    List<ResourceGroupResponse> resourceGroupResponses =
        getResponse(resourceGroupClient.getResourceGroups(anInvite.getAccountIdentifier(), anInvite.getOrgIdentifier(),
                        anInvite.getProjectIdentifier(), 0, DEFAULT_PAGE_SIZE))
            .getContent();
    Predicate<ResourceGroupResponse> filter;
    if (filterResourceGroupIdentifiers != null && !filterResourceGroupIdentifiers.isEmpty()) {
      filter = resourceGroupResponse
          -> resourceGroupIds.contains(resourceGroupResponse.getResourceGroup().getIdentifier())
          && filterResourceGroupIdentifiers.contains(resourceGroupResponse.getResourceGroup().getIdentifier());
    } else {
      filter =
          resourceGroupResponse -> resourceGroupIds.contains(resourceGroupResponse.getResourceGroup().getIdentifier());
    }
    return resourceGroupResponses.stream()
        .filter(filter::test)
        .map(ResourceGroupResponse::getResourceGroup)
        .collect(toMap(ResourceGroupDTO::getIdentifier, Function.identity()));
  }

  private Map<String, RoleResponseDTO> getRoleMapForInvites(List<Invite> invites, Set<String> filterRoleIdentifiers) {
    if (invites.isEmpty()) {
      return Collections.emptyMap();
    }
    Invite anInvite = invites.get(0);
    Set<String> roleIds = invites.stream()
                              .map(Invite::getRoleBindings)
                              .flatMap(Collection::stream)
                              .map(RoleBinding::getRoleIdentifier)
                              .collect(Collectors.toSet());
    List<RoleResponseDTO> roleResponseDTOs =
        getResponse(accessControlAdminClient.getRoles(anInvite.getAccountIdentifier(), anInvite.getOrgIdentifier(),
                        anInvite.getProjectIdentifier(), 0, DEFAULT_PAGE_SIZE))
            .getContent();
    Predicate<RoleResponseDTO> filter;
    if (filterRoleIdentifiers != null && !filterRoleIdentifiers.isEmpty()) {
      filter = roleResponseDTO
          -> roleIds.contains(roleResponseDTO.getRole().getIdentifier())
          && filterRoleIdentifiers.contains(roleResponseDTO.getRole().getIdentifier());
    } else {
      filter = roleResponseDTO -> roleIds.contains(roleResponseDTO.getRole().getIdentifier());
    }
    return roleResponseDTOs.stream()
        .filter(filter::test)
        .collect(toMap(roleResponseDTO -> roleResponseDTO.getRole().getIdentifier(), Function.identity()));
  }

  private Map<String, UserSearchDTO> getPendingUserMap(List<String> userEmails) {
    List<UserInfo> users = ngUserService.getUsersFromEmail(userEmails);
    Map<String, UserSearchDTO> userSearchMap = new HashMap<>();
    users.forEach(user
        -> userSearchMap.put(user.getEmail(),
            UserSearchDTO.builder().email(user.getEmail()).name(user.getName()).uuid(user.getUuid()).build()));
    for (String email : userEmails) {
      if (!userSearchMap.containsKey(email)) {
        userSearchMap.put(email, UserSearchDTO.builder().email(email).build());
      }
    }
    return userSearchMap;
  }

  @Override
  public boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String userId) {
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
