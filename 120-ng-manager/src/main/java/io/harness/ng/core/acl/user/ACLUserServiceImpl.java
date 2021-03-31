package io.harness.ng.core.acl.user;

import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.ng.core.acl.user.ACLUserAggregateDTO.Status.ACTIVE;
import static io.harness.ng.core.acl.user.ACLUserAggregateDTO.Status.INVITED;
import static io.harness.ng.core.acl.user.ACLUserAggregateDTO.Status.REQUESTED;
import static io.harness.remote.client.NGRestUtils.getResponse;

import static java.lang.Boolean.FALSE;
import static java.util.regex.Pattern.quote;
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
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.acl.user.ACLUserAggregateDTO.Status;
import io.harness.ng.core.acl.user.remote.ACLAggregateFilter;
import io.harness.ng.core.invites.api.InviteService;
import io.harness.ng.core.invites.dto.UserSearchDTO;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.ng.core.invites.entities.Invite.InviteKeys;
import io.harness.ng.core.invites.entities.Invite.InviteType;
import io.harness.ng.core.invites.remote.RoleBinding;
import io.harness.ng.core.invites.remote.RoleBinding.RoleBindingKeys;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PL)
public class ACLUserServiceImpl implements ACLUserService {
  private static final int DEFAULT_PAGE_SIZE = 1000;
  AccessControlAdminClient accessControlAdminClient;
  NgUserService ngUserService;
  InviteService inviteService;
  ResourceGroupClient resourceGroupClient;

  @Override
  public PageResponse<ACLUserAggregateDTO> getActiveUsers(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, PageRequest pageRequest, ACLAggregateFilter aclAggregateFilter) {
    validateRequest(searchTerm, aclAggregateFilter);
    if (ACLAggregateFilter.isFilterApplied(aclAggregateFilter)) {
      return getFilteredActiveUsers(
          accountIdentifier, orgIdentifier, projectIdentifier, pageRequest, aclAggregateFilter);
    }
    return getUnfilteredActiveUsersPage(accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, pageRequest);
  }

  @Override
  public PageResponse<ACLUserAggregateDTO> getPendingUsers(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, PageRequest pageRequest, ACLAggregateFilter aclAggregateFilter) {
    validateRequest(searchTerm, aclAggregateFilter);
    PageResponse<Invite> invitesPage =
        getInvitePage(accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, pageRequest, aclAggregateFilter);
    List<String> userEmails = invitesPage.getContent().stream().map(Invite::getEmail).collect(toList());
    Map<String, UserSearchDTO> userSearchDTOs = getPendingUserMap(userEmails, accountIdentifier);
    Map<String, RoleResponseDTO> roleMap = getRoleMapForInvites(invitesPage.getContent());
    Map<String, ResourceGroupDTO> resourceGroupMap = getResourceGroupMapForInvites(invitesPage.getContent());
    List<ACLUserAggregateDTO> aclUserAggregateDTOs =
        aggregatePendingUsers(invitesPage.getContent(), userSearchDTOs, roleMap, resourceGroupMap);
    return PageUtils.getNGPageResponse(invitesPage, aclUserAggregateDTOs);
  }

  private void validateRequest(String searchTerm, ACLAggregateFilter aclAggregateFilter) {
    if (!isBlank(searchTerm) && ACLAggregateFilter.isFilterApplied(aclAggregateFilter)) {
      log.error("Search term and filter on role/resourcegroup identifiers can't be applied at the same time");
      throw new InvalidRequestException(
          "Search term and filter on role/resourcegroup identifiers can't be applied at the same time");
    }
  }

  private PageResponse<ACLUserAggregateDTO> getFilteredActiveUsers(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, PageRequest pageRequest, ACLAggregateFilter aclAggregateFilter) {
    RoleAssignmentAggregateResponseDTO roleAssignmentAggregateResponseDTO =
        getRoleAssignments(accountIdentifier, orgIdentifier, projectIdentifier, aclAggregateFilter);
    Map<String, List<RoleBinding>> userRoleAssignmentsMap =
        getUserRoleAssignmentMap(roleAssignmentAggregateResponseDTO);
    List<UserSearchDTO> users = getUsersForFilteredActiveUsersPage(
        new ArrayList<>(userRoleAssignmentsMap.keySet()), accountIdentifier, pageRequest);
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

  private List<UserSearchDTO> getUsersForFilteredActiveUsersPage(
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

  private PageResponse<ACLUserAggregateDTO> getUnfilteredActiveUsersPage(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, PageRequest pageRequest) {
    PageResponse<UserSearchDTO> userPage = getUsersFromUnfilteredActiveUsersPage(
        accountIdentifier, orgIdentifier, projectIdentifier, pageRequest, searchTerm);
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
    List<ACLUserAggregateDTO> aclUserAggregateDTOs =
        userPage.getContent()
            .stream()
            .map(user
                -> ACLUserAggregateDTO.builder()
                       .roleBindings(userRoleAssignmentsMap.getOrDefault(user.getUuid(), Collections.emptyList()))
                       .status(ACTIVE)
                       .user(user)
                       .build())
            .collect(toList());
    return PageUtils.getNGPageResponse(userPage, aclUserAggregateDTOs);
  }

  private PageResponse<UserSearchDTO> getUsersFromUnfilteredActiveUsersPage(String accountIdentifier,
      String orgIdentifier, String projectIdentifier, PageRequest pageRequest, String searchTerm) {
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

  private PageResponse<Invite> getInvitePage(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String searchTerm, PageRequest pageRequest, ACLAggregateFilter aclAggregateFilter) {
    Criteria criteria = Criteria.where(InviteKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(InviteKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(InviteKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(InviteKeys.deleted)
                            .is(FALSE);
    if (ACLAggregateFilter.isFilterApplied(aclAggregateFilter)) {
      if (isNotEmpty(aclAggregateFilter.getRoleIdentifiers())) {
        criteria.and(InviteKeys.roleBindings + "." + RoleBindingKeys.roleIdentifier)
            .in(aclAggregateFilter.getRoleIdentifiers());
      }
      if (isNotEmpty(aclAggregateFilter.getResourceGroupIdentifiers())) {
        criteria.and(InviteKeys.roleBindings + "." + RoleBindingKeys.resourceGroupIdentifier)
            .in(aclAggregateFilter.getResourceGroupIdentifiers());
      }
    }
    if (!isBlank(searchTerm)) {
      Page<UserInfo> userInfos = ngUserService.list(
          accountIdentifier, searchTerm, org.springframework.data.domain.PageRequest.of(0, DEFAULT_PAGE_SIZE));
      List<String> userIds = userInfos.stream().map(UserInfo::getEmail).collect(toList());
      Criteria searchTermCriteria = new Criteria();
      searchTermCriteria.orOperator(
          Criteria.where(InviteKeys.email).regex(quote(searchTerm)), Criteria.where(InviteKeys.email).in(userIds));
      criteria = new Criteria().andOperator(criteria, searchTermCriteria);
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

  private Map<String, ResourceGroupDTO> getResourceGroupMapForInvites(List<Invite> invites) {
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
    filter =
        resourceGroupResponse -> resourceGroupIds.contains(resourceGroupResponse.getResourceGroup().getIdentifier());
    return resourceGroupResponses.stream()
        .filter(filter::test)
        .map(ResourceGroupResponse::getResourceGroup)
        .collect(toMap(ResourceGroupDTO::getIdentifier, Function.identity()));
  }

  private Map<String, RoleResponseDTO> getRoleMapForInvites(List<Invite> invites) {
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
    filter = roleResponseDTO -> roleIds.contains(roleResponseDTO.getRole().getIdentifier());
    return roleResponseDTOs.stream()
        .filter(filter::test)
        .collect(toMap(roleResponseDTO -> roleResponseDTO.getRole().getIdentifier(), Function.identity()));
  }

  private Map<String, UserSearchDTO> getPendingUserMap(List<String> userEmails, String accountIdentifier) {
    List<UserInfo> users = ngUserService.getUsersFromEmail(userEmails, accountIdentifier);
    Map<String, UserSearchDTO> userSearchMap = new HashMap<>();
    users.forEach(user
        -> userSearchMap.put(user.getEmail(),
            UserSearchDTO.builder().email(user.getEmail()).name(user.getName()).uuid(user.getUuid()).build()));
    for (String email : userEmails) {
      userSearchMap.computeIfAbsent(email, email1 -> UserSearchDTO.builder().email(email1).build());
    }
    return userSearchMap;
  }

  @Override
  public boolean deleteActiveUser(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String userId) {
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

  @Override
  public boolean deletePendingUser(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String emailId) {
    return inviteService.deleteInvite(accountIdentifier, orgIdentifier, projectIdentifier, emailId);
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
