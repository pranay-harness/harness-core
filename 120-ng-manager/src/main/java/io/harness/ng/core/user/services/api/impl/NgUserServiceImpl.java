package io.harness.ng.core.user.services.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageResponse;
import io.harness.exception.DuplicateFieldException;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.ng.core.invites.entities.Role;
import io.harness.ng.core.invites.entities.UserProjectMap;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.remote.UserClient;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.remote.client.NGRestUtils;
import io.harness.remote.client.RestClientUtils;
import io.harness.repositories.invites.spring.UserProjectMapRepository;
import io.harness.resourcegroup.framework.service.ResourceGroupService;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@Slf4j
@OwnedBy(PL)
public class NgUserServiceImpl implements NgUserService {
  private final UserClient userClient;
  private final UserProjectMapRepository userProjectMapRepository;
  public static final String ALL_RESOURCES_RESOURCE_GROUP = "_all_resources";
  private final AccessControlAdminClient accessControlAdminClient;
  private final Map<String, String> inviteRoleToRoleIdentifierMapping;
  private final ResourceGroupService resourceGroupService;

  @Inject
  public NgUserServiceImpl(UserClient userClient, UserProjectMapRepository userProjectMapRepository,
      AccessControlAdminClient accessControlAdminClient, ResourceGroupService resourceGroupService) {
    this.userClient = userClient;
    this.userProjectMapRepository = userProjectMapRepository;
    this.accessControlAdminClient = accessControlAdminClient;
    this.resourceGroupService = resourceGroupService;
    this.inviteRoleToRoleIdentifierMapping = new HashMap<>();
    inviteRoleToRoleIdentifierMapping.put("Project Viewer", "_project_viewer");
    inviteRoleToRoleIdentifierMapping.put("Project Member", "_project_viewer");
    inviteRoleToRoleIdentifierMapping.put("Project Admin", "_project_admin");
    inviteRoleToRoleIdentifierMapping.put("Organization Viewer", "_organization_viewer");
    inviteRoleToRoleIdentifierMapping.put("Organization Member", "_organization_viewer");
    inviteRoleToRoleIdentifierMapping.put("Organization Admin", "_organization_admin");
    inviteRoleToRoleIdentifierMapping.put("Account Viewer", "_account_viewer");
    inviteRoleToRoleIdentifierMapping.put("Account Member", "_account_viewer");
    inviteRoleToRoleIdentifierMapping.put("Account Admin", "_account_admin");
  }

  @Override
  public Page<UserInfo> list(String accountIdentifier, String searchString, Pageable pageable) {
    //  @Ankush remove the offset and limit from the following statement because it is redundant pagination
    PageResponse<UserInfo> userPageResponse = RestClientUtils.getResponse(userClient.list(accountIdentifier,
        String.valueOf(pageable.getOffset()), String.valueOf(pageable.getPageSize()), searchString, false));
    List<UserInfo> users = userPageResponse.getResponse();
    return new PageImpl<>(users, pageable, users.size());
  }

  @Override
  public List<UserProjectMap> listUserProjectMap(Criteria criteria) {
    return userProjectMapRepository.findAll(criteria);
  }

  // isSignedUp flag: Users in current gen exists even if they are in invited state and not signed up yet.
  public Optional<UserInfo> getUserFromEmail(String email) {
    return RestClientUtils.getResponse(userClient.getUserFromEmail(email));
  }

  public List<String> getUsernameFromEmail(String accountIdentifier, List<String> emailList) {
    return RestClientUtils.getResponse(userClient.getUsernameFromEmail(accountIdentifier, emailList));
  }

  @Override
  public Optional<UserProjectMap> getUserProjectMap(
      String uuid, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return userProjectMapRepository.findByUserIdAndAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
        uuid, accountIdentifier, orgIdentifier, projectIdentifier);
  }

  private void upsertResourceGroup(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String resourceGroupIdentifier) {
    ResourceGroupDTO resourceGroupDTO = ResourceGroupDTO.builder()
                                            .accountIdentifier(accountIdentifier)
                                            .orgIdentifier(orgIdentifier)
                                            .projectIdentifier(projectIdentifier)
                                            .identifier(resourceGroupIdentifier)
                                            .fullScopeSelected(true)
                                            .resourceSelectors(new ArrayList<>())
                                            .name("All Resources")
                                            .description("Resource Group containing all resources")
                                            .color("#0061fc")
                                            .tags(ImmutableMap.of("predefined", "true"))
                                            .build();
    try {
      resourceGroupService.create(resourceGroupDTO);
    } catch (DuplicateFieldException | DuplicateKeyException duplicateException) {
      // ignore if already exists
    }
  }

  @Override
  public boolean createUserProjectMap(Invite invite, UserInfo user) {
    Optional<UserProjectMap> userProjectMapOptional =
        userProjectMapRepository.findByUserIdAndAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
            user.getUuid(), invite.getAccountIdentifier(), invite.getOrgIdentifier(), invite.getProjectIdentifier());

    UserProjectMap userProjectMap = userProjectMapOptional
                                        .map(e -> {
                                          e.getRoles().add(invite.getRole());
                                          return e;
                                        })
                                        .orElseGet(()
                                                       -> UserProjectMap.builder()
                                                              .userId(user.getUuid())
                                                              .accountIdentifier(invite.getAccountIdentifier())
                                                              .orgIdentifier(invite.getOrgIdentifier())
                                                              .projectIdentifier(invite.getProjectIdentifier())
                                                              .roles(ImmutableList.of(invite.getRole()))
                                                              .build());
    userProjectMapRepository.save(userProjectMap);
    Role role = invite.getRole();
    Optional.ofNullable(role.getName()).ifPresent(name -> role.setName(name.trim()));

    if (inviteRoleToRoleIdentifierMapping.containsKey(role.getName())) {
      // create _all_resources resource group if it doesn't exist already
      upsertResourceGroup(userProjectMap.getAccountIdentifier(), userProjectMap.getOrgIdentifier(),
          userProjectMap.getProjectIdentifier(), ALL_RESOURCES_RESOURCE_GROUP);

      RoleAssignmentDTO roleAssignmentDTO =
          RoleAssignmentDTO.builder()
              .disabled(false)
              .roleIdentifier(inviteRoleToRoleIdentifierMapping.get(role.getName()))
              .principal(PrincipalDTO.builder().type(PrincipalType.USER).identifier(userProjectMap.getUserId()).build())
              .resourceGroupIdentifier(ALL_RESOURCES_RESOURCE_GROUP)
              .build();

      try {
        RoleAssignmentResponseDTO roleAssignmentResponseDTO =
            NGRestUtils.getResponse(accessControlAdminClient.createRoleAssignment(userProjectMap.getAccountIdentifier(),
                userProjectMap.getOrgIdentifier(), userProjectMap.getProjectIdentifier(), roleAssignmentDTO));
        log.info("Created role assignment for invite: {}", roleAssignmentResponseDTO);
      } catch (Exception exception) {
        // this might fail if a user has already been assigned this role before, e.g. by migration, ignore error
        log.error("Error while creating role assignment for invite, user might already have this role, please verify.",
            exception);
      }
    }

    return postCreation(invite, user);
  }

  private boolean postCreation(Invite invite, UserInfo user) {
    String accountId = invite.getAccountIdentifier();
    String userId = user.getUuid();
    return RestClientUtils.getResponse(userClient.addUserToAccount(userId, accountId));
  }

  @Override
  public List<UserInfo> getUsersByIds(List<String> userIds) {
    return RestClientUtils.getResponse(userClient.getUsersByIds(userIds));
  }

  @Override
  public UserProjectMap createUserProjectMap(UserProjectMap userProjectMap) {
    return userProjectMapRepository.save(userProjectMap);
  }

  @Override
  public boolean isUserInAccount(String accountId, String userId) {
    return Boolean.TRUE.equals(RestClientUtils.getResponse(userClient.isUserInAccount(accountId, userId)));
  }

  @Override
  public void removeUserFromScope(
      String userId, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Optional<UserProjectMap> userProjectMapOptional =
        userProjectMapRepository.findByUserIdAndAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
            userId, accountIdentifier, orgIdentifier, projectIdentifier);
    if (!userProjectMapOptional.isPresent()) {
      return;
    }
    userProjectMapRepository.delete(userProjectMapOptional.get());
    userProjectMapOptional = userProjectMapRepository.findFirstByUserIdAndAccountIdentifier(userId, accountIdentifier);
    if (!userProjectMapOptional.isPresent()) {
      //      User has been totally removed from the account -> notify cg
      RestClientUtils.getResponse(userClient.safeDeleteUser(userId, accountIdentifier));
    }
  }
}
