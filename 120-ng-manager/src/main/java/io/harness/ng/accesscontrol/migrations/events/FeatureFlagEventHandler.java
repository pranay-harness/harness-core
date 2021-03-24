package io.harness.ng.accesscontrol.migrations.events;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.accesscontrol.clients.RoleAssignmentClient;
import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentCreateRequestDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.beans.PageResponse;
import io.harness.eventsframework.api.EventHandler;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.featureflag.FeatureFlagChangeDTO;
import io.harness.ng.accesscontrol.migrations.models.Migration;
import io.harness.ng.accesscontrol.migrations.models.Migration.MigrationBuilder;
import io.harness.ng.accesscontrol.migrations.services.MigrationService;
import io.harness.ng.core.dto.OrganizationFilterDTO;
import io.harness.ng.core.dto.ProjectFilterDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.invites.entities.UserProjectMap;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.user.User;
import io.harness.ng.core.user.remote.UserClient;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.remote.client.NGRestUtils;
import io.harness.remote.client.RestClientUtils;
import io.harness.resourcegroup.framework.service.ResourceGroupService;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.utils.CryptoUtils;
import io.harness.utils.RetryUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Pageable;

@Slf4j
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class FeatureFlagEventHandler implements EventHandler {
  private static final String NG_RBAC_ENABLED = "NG_RBAC_ENABLED";
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
  private final MigrationService migrationService;
  private final NgUserService ngUserService;
  private final ResourceGroupService resourceGroupService;
  private final RoleAssignmentClient roleAssignmentClient;
  private final UserClient userClient;

  private static final RetryPolicy<Object> retryPolicy =
      RetryUtils.getRetryPolicy("[Retrying]: Failed to create the role assignments, attempt: {}",
          "[Failed]: Failed to create the role assignments with error: {}", ImmutableList.of(Exception.class),
          Duration.ofSeconds(15), 3, log);

  private List<RoleAssignmentResponseDTO> createRoleAssignments(String account, String org, String project,
      List<User> users, String adminRoleIdentifier, String viewerRoleIdentifier) {
    List<RoleAssignmentDTO> roleAssignments = new ArrayList<>();
    for (User user : users) {
      boolean admin =
          Optional.ofNullable(user.getUserGroups())
              .filter(userGroups
                  -> userGroups.stream().anyMatch(ug -> CG_ACCOUNT_ADMINISTRATOR_USER_GROUP.equals(ug.getName())))
              .isPresent();
      if (admin) {
        roleAssignments.add(getRoleAssignment(user.getUuid(), adminRoleIdentifier));
      } else {
        roleAssignments.add(getRoleAssignment(user.getUuid(), viewerRoleIdentifier));
      }
    }
    return NGRestUtils.getResponse(roleAssignmentClient.createMulti(
        account, org, project, RoleAssignmentCreateRequestDTO.builder().roleAssignments(roleAssignments).build()));
  }

  private void handleNGRBACEnabledFlag(String accountId) {
    MigrationBuilder migrationBuilder = Migration.builder().accountId(accountId).startedAt(new Date());

    // get users along with user groups for account
    PageResponse<User> users = RestClientUtils.getResponse(userClient.list(accountId, "0", "100000", null, true));
    if (isEmpty(users)) {
      migrationService.save(migrationBuilder.endedAt(new Date()).build());
      return;
    }

    upsertResourceGroup(accountId, null, null, ALL_RESOURCES_RESOURCE_GROUP);
    // adding account level roles to users
    List<RoleAssignmentResponseDTO> roleAssignmentsCreated = createRoleAssignments(
        accountId, null, null, users, ACCOUNT_ADMIN_ROLE_IDENTIFIER, ACCOUNT_VIEWER_ROLE_IDENTIFIER);

    // adding org level roles to users
    List<Organization> organizations =
        orgService.list(accountId, Pageable.unpaged(), OrganizationFilterDTO.builder().build()).getContent();
    for (Organization organization : organizations) {
      upsertResourceGroup(accountId, organization.getIdentifier(), null, ALL_RESOURCES_RESOURCE_GROUP);
      roleAssignmentsCreated.addAll(createRoleAssignments(
          accountId, organization.getIdentifier(), null, users, ORG_ADMIN_ROLE_IDENTIFIER, ORG_VIEWER_ROLE_IDENTIFIER));

      // adding project level roles to users
      List<Project> projects = projectService
                                   .list(accountId, Pageable.unpaged(),
                                       ProjectFilterDTO.builder().orgIdentifier(organization.getIdentifier()).build())
                                   .getContent();
      for (Project project : projects) {
        upsertResourceGroup(
            accountId, organization.getIdentifier(), project.getIdentifier(), ALL_RESOURCES_RESOURCE_GROUP);
        roleAssignmentsCreated.addAll(createRoleAssignments(accountId, organization.getIdentifier(),
            project.getIdentifier(), users, PROJECT_ADMIN_ROLE_IDENTIFIER, PROJECT_VIEWER_ROLE_IDENTIFIER));

        // adding user project map
        users.forEach(user
            -> upsertUserProjectMap(accountId, organization.getIdentifier(), project.getIdentifier(), user.getUuid()));
      }
    }
    migrationService.save(migrationBuilder.createdRoleAssignments(roleAssignmentsCreated).build());
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
  public boolean handle(Message message) {
    FeatureFlagChangeDTO featureFlagChangeDTO = FeatureFlagChangeDTO.parseFrom(message.getMessage().getData());
    if (Optional.ofNullable(featureFlagChangeDTO).isPresent()) {
      String accountId = featureFlagChangeDTO.getAccountId();
      String featureName = featureFlagChangeDTO.getFeatureName();
      if (NG_RBAC_ENABLED.equals(featureName)) {
        handleNGRBACEnabledFlag(accountId);
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

  private RoleAssignmentResponseDTO createRoleAssignment(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, RoleAssignmentDTO roleAssignmentDTO) {
    return NGRestUtils.getResponse(
        roleAssignmentClient.create(accountIdentifier, orgIdentifier, projectIdentifier, roleAssignmentDTO));
  }

  private boolean upsertResourceGroup(
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
      resourceGroupService.create(resourceGroupDTO);
    } catch (DuplicateKeyException duplicateKeyException) {
      log.info("Resource group already exists, account: {}, org: {}, project:{}, identifier: {}", accountIdentifier,
          orgIdentifier, projectIdentifier, resourceGroupIdentifier);
    }
    return true;
  }
}
