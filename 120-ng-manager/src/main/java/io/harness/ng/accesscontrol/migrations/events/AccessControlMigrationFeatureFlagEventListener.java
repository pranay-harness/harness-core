package io.harness.ng.accesscontrol.migrations.events;

import static io.harness.beans.FeatureName.NG_ACCESS_CONTROL_MIGRATION;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentCreateRequestDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.beans.PageResponse;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.featureflag.FeatureFlagChangeDTO;
import io.harness.exception.DuplicateFieldException;
import io.harness.ng.accesscontrol.migrations.models.AccessControlMigration;
import io.harness.ng.accesscontrol.migrations.models.AccessControlMigration.AccessControlMigrationBuilder;
import io.harness.ng.accesscontrol.migrations.models.AccessControlMigrationPrincipal;
import io.harness.ng.accesscontrol.migrations.models.RoleAssignmentMetadata;
import io.harness.ng.accesscontrol.migrations.services.AccessControlMigrationService;
import io.harness.ng.core.dto.OrganizationFilterDTO;
import io.harness.ng.core.dto.ProjectFilterDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.event.MessageListener;
import io.harness.ng.core.invites.entities.UserProjectMap;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.remote.UserClient;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.remote.client.NGRestUtils;
import io.harness.remote.client.RestClientUtils;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroup.remote.dto.ResourceGroupRequest;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;
import io.harness.utils.CryptoUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Pageable;

@Slf4j
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.PL)
public class AccessControlMigrationFeatureFlagEventListener implements MessageListener {
  private static final String CG_ACCOUNT_ADMINISTRATOR_USER_GROUP = "Account Administrator";
  private static final String ACCOUNT_ADMIN_ROLE_IDENTIFIER = "_account_admin";
  private static final String ORG_ADMIN_ROLE_IDENTIFIER = "_organization_admin";
  private static final String PROJECT_ADMIN_ROLE_IDENTIFIER = "_project_admin";
  private static final String ACCOUNT_VIEWER_ROLE_IDENTIFIER = "_account_viewer";
  private static final String ORG_VIEWER_ROLE_IDENTIFIER = "_organization_viewer";
  private static final String PROJECT_VIEWER_ROLE_IDENTIFIER = "_project_viewer";
  private static final String ALL_RESOURCES_RESOURCE_GROUP = "_all_resources";
  private final ProjectService projectService;
  private final OrganizationService orgService;
  private final AccessControlMigrationService accessControlMigrationService;
  private final NgUserService ngUserService;
  private final ResourceGroupClient resourceGroupClient;
  private final AccessControlAdminClient accessControlAdminClient;
  private final UserClient userClient;

  private RoleAssignmentMetadata createRoleAssignments(String account, String org, String project, List<UserInfo> users,
      String adminRoleIdentifier, String viewerRoleIdentifier) {
    List<RoleAssignmentDTO> roleAssignmentRequests = new ArrayList<>();
    for (UserInfo user : users) {
      boolean admin =
          Optional.ofNullable(user.getUserGroups())
              .filter(userGroups
                  -> userGroups.stream().anyMatch(ug -> CG_ACCOUNT_ADMINISTRATOR_USER_GROUP.equals(ug.getName())))
              .isPresent();
      if (admin) {
        roleAssignmentRequests.add(getRoleAssignment(user.getUuid(), adminRoleIdentifier));
      } else {
        roleAssignmentRequests.add(getRoleAssignment(user.getUuid(), viewerRoleIdentifier));
      }
    }

    List<RoleAssignmentResponseDTO> createdRoleAssignments =
        NGRestUtils.getResponse(accessControlAdminClient.createMulti(account, org, project,
            RoleAssignmentCreateRequestDTO.builder().roleAssignments(roleAssignmentRequests).build()));

    return RoleAssignmentMetadata.builder()
        .createdRoleAssignments(createdRoleAssignments)
        .roleAssignmentRequests(roleAssignmentRequests)
        .status(RoleAssignmentMetadata.getStatus(createdRoleAssignments, roleAssignmentRequests))
        .build();
  }

  private boolean handleNGRBACEnabledFlag(String accountId) {
    log.info("Running ng access control migration for account: {}", accountId);
    // get users along with user groups for account
    PageResponse<UserInfo> users = RestClientUtils.getResponse(userClient.list(accountId, "0", "100000", null, true));
    AccessControlMigrationBuilder migrationBuilder =
        AccessControlMigration.builder()
            .accountId(accountId)
            .startedAt(new Date())
            .principals(users.stream()
                            .map(user
                                -> AccessControlMigrationPrincipal.builder()
                                       .email(user.getEmail())
                                       .identifier(user.getUuid())
                                       .name(user.getName())
                                       .build())
                            .collect(Collectors.toList()));

    if (isEmpty(users)) {
      accessControlMigrationService.save(migrationBuilder.endedAt(new Date()).build());
      return true;
    }

    upsertResourceGroup(accountId, null, null, ALL_RESOURCES_RESOURCE_GROUP);

    // adding account level roles to users
    List<RoleAssignmentMetadata> roleAssignmentMetadataList = new ArrayList<>();
    roleAssignmentMetadataList.add(createRoleAssignments(
        accountId, null, null, users, ACCOUNT_ADMIN_ROLE_IDENTIFIER, ACCOUNT_VIEWER_ROLE_IDENTIFIER));

    // adding org level roles to users
    List<Organization> organizations =
        orgService.list(accountId, Pageable.unpaged(), OrganizationFilterDTO.builder().build()).getContent();
    for (Organization organization : organizations) {
      upsertResourceGroup(accountId, organization.getIdentifier(), null, ALL_RESOURCES_RESOURCE_GROUP);
      roleAssignmentMetadataList.add(createRoleAssignments(
          accountId, organization.getIdentifier(), null, users, ORG_ADMIN_ROLE_IDENTIFIER, ORG_VIEWER_ROLE_IDENTIFIER));

      // adding project level roles to users
      List<Project> projects = projectService
                                   .list(accountId, Pageable.unpaged(),
                                       ProjectFilterDTO.builder().orgIdentifier(organization.getIdentifier()).build())
                                   .getContent();
      for (Project project : projects) {
        upsertResourceGroup(
            accountId, organization.getIdentifier(), project.getIdentifier(), ALL_RESOURCES_RESOURCE_GROUP);
        roleAssignmentMetadataList.add(createRoleAssignments(accountId, organization.getIdentifier(),
            project.getIdentifier(), users, PROJECT_ADMIN_ROLE_IDENTIFIER, PROJECT_VIEWER_ROLE_IDENTIFIER));

        // adding user project map
        users.forEach(user
            -> upsertUserProjectMap(accountId, organization.getIdentifier(), project.getIdentifier(), user.getUuid()));
      }
    }
    accessControlMigrationService.save(migrationBuilder.metadata(roleAssignmentMetadataList).build());
    return true;
  }

  private void upsertUserProjectMap(
      String accountId, String orgIdentifier, String projectIdentifier, String principalIdentifier) {
    try {
      ngUserService.createUserProjectMap(UserProjectMap.builder()
                                             .accountIdentifier(accountId)
                                             .projectIdentifier(projectIdentifier)
                                             .orgIdentifier(orgIdentifier)
                                             .userId(principalIdentifier)
                                             .roles(new ArrayList<>())
                                             .build());
    } catch (DuplicateKeyException duplicateKeyException) {
      log.info("User project map already exists account: {}, org: {}, project: {}, principal: {}", accountId,
          orgIdentifier, projectIdentifier, principalIdentifier);
    }
  }

  @SneakyThrows
  @Override
  public boolean handleMessage(Message message) {
    if (message != null && message.hasMessage()) {
      FeatureFlagChangeDTO featureFlagChangeDTO;
      try {
        featureFlagChangeDTO = FeatureFlagChangeDTO.parseFrom(message.getMessage().getData());
      } catch (InvalidProtocolBufferException e) {
        // no point in reading the message again
        return true;
      }
      if (NG_ACCESS_CONTROL_MIGRATION.equals(FeatureName.valueOf(featureFlagChangeDTO.getFeatureName()))
          && featureFlagChangeDTO.getEnable()) {
        try {
          return handleNGRBACEnabledFlag(featureFlagChangeDTO.getAccountId());
        } catch (Exception ex) {
          log.error(
              "Error while processing {} feature flag for access control migration", NG_ACCESS_CONTROL_MIGRATION, ex);
          return false;
        }
      }
    }
    return true;
  }

  private RoleAssignmentDTO getRoleAssignment(String principalIdentifier, String roleIdentifier) {
    return RoleAssignmentDTO.builder()
        .disabled(false)
        .identifier("role_assignment_".concat(CryptoUtils.secureRandAlphaNumString(20)))
        .roleIdentifier(roleIdentifier)
        .resourceGroupIdentifier(ALL_RESOURCES_RESOURCE_GROUP)
        .principal(PrincipalDTO.builder().identifier(principalIdentifier).type(PrincipalType.USER).build())
        .build();
  }

  private void upsertResourceGroup(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String resourceGroupIdentifier) {
    ResourceGroupDTO resourceGroupDTO = ResourceGroupDTO.builder()
                                            .accountIdentifier(accountIdentifier)
                                            .orgIdentifier(orgIdentifier)
                                            .projectIdentifier(projectIdentifier)
                                            .identifier(ALL_RESOURCES_RESOURCE_GROUP)
                                            .fullScopeSelected(true)
                                            .resourceSelectors(new ArrayList<>())
                                            .name("All Resources")
                                            .description("Resource Group containing all resources")
                                            .color("#0061fc")
                                            .tags(ImmutableMap.of("predefined", "true"))
                                            .build();
    try {
      resourceGroupClient.create(accountIdentifier, orgIdentifier, projectIdentifier,
          ResourceGroupRequest.builder().resourceGroup(resourceGroupDTO).build());
    } catch (DuplicateFieldException duplicateFieldException) {
      log.info("Resource group already exists, account: {}, org: {}, project:{}, identifier: {}", accountIdentifier,
          orgIdentifier, projectIdentifier, resourceGroupIdentifier);
    }
  }
}
