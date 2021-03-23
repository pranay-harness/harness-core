package io.harness.accesscontrol.migrations.events;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.accesscontrol.commons.events.EventHandler;
import io.harness.accesscontrol.migrations.models.Migration;
import io.harness.accesscontrol.migrations.models.Migration.MigrationBuilder;
import io.harness.accesscontrol.migrations.models.MigrationStatus;
import io.harness.accesscontrol.migrations.models.UserMigration;
import io.harness.accesscontrol.migrations.services.MigrationService;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.resources.resourcegroups.HarnessResourceGroupService;
import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.beans.PageResponse;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.featureflag.FeatureFlagChangeDTO;
import io.harness.ng.core.dto.OrganizationResponse;
import io.harness.ng.core.dto.ProjectResponse;
import io.harness.ng.core.user.User;
import io.harness.ng.core.user.remote.UserClient;
import io.harness.organizationmanagerclient.remote.OrganizationManagerClient;
import io.harness.projectmanagerclient.remote.ProjectManagerClient;
import io.harness.remote.client.NGRestUtils;
import io.harness.remote.client.RestClientUtils;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroupclient.ResourceGroupResponse;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;
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
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.lang3.StringUtils;

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
  private final UserClient userClient;
  private final ProjectManagerClient projectClient;
  private final OrganizationManagerClient orgClient;
  private final RoleAssignmentService roleAssignmentService;
  private final ResourceGroupClient resourceGroupClient;
  private final ScopeService scopeService;
  private final MigrationService migrationService;
  private final HarnessResourceGroupService harnessResourceGroupService;
  private static final RetryPolicy<Object> retryPolicy =
      RetryUtils.getRetryPolicy("[Retrying]: Failed to create the role assignments, attempt: {}",
          "[Failed]: Failed to create the role assignments with error: {}", ImmutableList.of(Exception.class),
          Duration.ofSeconds(15), 3, log);

  private void handleNGRBACEnabledFlag(String accountId) {
    MigrationBuilder migrationBuilder = Migration.builder().accountId(accountId).startedAt(new Date());
    PageResponse<User> users = RestClientUtils.getResponse(userClient.list(accountId, "0", "100000", null, true));
    if (isEmpty(users)) {
      migrationService.save(migrationBuilder.endedAt(new Date()).userMigrations(new ArrayList<>()).build());
      return;
    }
    List<UserMigration> userMigrations = new ArrayList<>();
    for (User user : users) {
      if (user.getUserGroups() == null) {
        user.setUserGroups(new ArrayList<>());
      }
      String principalIdentifier = user.getUuid();
      boolean admin =
          user.getUserGroups().stream().anyMatch(x -> CG_ACCOUNT_ADMINISTRATOR_USER_GROUP.equals(x.getName()));
      if (admin) {
        userMigrations.add(createRoleAssignments(accountId, principalIdentifier, ACCOUNT_ADMIN_ROLE_IDENTIFIER,
            ORG_ADMIN_ROLE_IDENTIFIER, PROJECT_ADMIN_ROLE_IDENTIFIER, ALL_RESOURCES_RESOURCE_GROUP));
      } else {
        userMigrations.add(createRoleAssignments(accountId, principalIdentifier, ACCOUNT_VIEWER_ROLE_IDENTIFIER,
            ORG_VIEWER_ROLE_IDENTIFIER, PROJECT_VIEWER_ROLE_IDENTIFIER, ALL_RESOURCES_RESOURCE_GROUP));
      }
    }
    migrationBuilder.userMigrations(userMigrations).endedAt(new Date());
    migrationService.save(migrationBuilder.build());
  }

  private UserMigration createRoleAssignments(String accountId, String principalIdentifier,
      String accountRoleIdentifier, String orgRoleIdentifier, String projectRoleIdentifier,
      String resourceGroupIdentifier) {
    UserMigration.UserMigrationBuilder migrationBuilder =
        UserMigration.builder().principalIdentifier(principalIdentifier).principalType("USER");
    boolean resourceGroupUpsertionStatus = upsertResourceGroup(accountId, null, null, resourceGroupIdentifier);

    List<RoleAssignment> roleAssignments = new ArrayList<>();
    roleAssignments.add(getRoleAssignment(accountId, null, null, principalIdentifier, accountRoleIdentifier));
    List<OrganizationResponse> orgs =
        NGRestUtils.getResponse(orgClient.listOrganization(accountId, "", 0, 100000, new ArrayList<>())).getContent();
    for (OrganizationResponse org : orgs) {
      resourceGroupUpsertionStatus = resourceGroupUpsertionStatus
          && upsertResourceGroup(accountId, org.getOrganization().getIdentifier(), null, resourceGroupIdentifier);
      roleAssignments.add(getRoleAssignment(
          accountId, org.getOrganization().getIdentifier(), null, principalIdentifier, orgRoleIdentifier));
      List<ProjectResponse> projects =
          NGRestUtils
              .getResponse(projectClient.listProject(
                  accountId, org.getOrganization().getIdentifier(), false, null, "", 0, 10000, new ArrayList<>()))
              .getContent();
      for (ProjectResponse project : projects) {
        resourceGroupUpsertionStatus = resourceGroupUpsertionStatus
            && upsertResourceGroup(accountId, org.getOrganization().getIdentifier(),
                project.getProject().getIdentifier(), resourceGroupIdentifier);
        roleAssignments.add(getRoleAssignment(accountId, org.getOrganization().getIdentifier(),
            project.getProject().getIdentifier(), principalIdentifier, projectRoleIdentifier));
      }
    }

    if (!resourceGroupUpsertionStatus) {
      migrationBuilder.migrationStatus(MigrationStatus.FAILED_AT_RESOURCE_GROUP_UPSERTION_AND_SYNC);
    } else {
      List<RoleAssignment> createdRoleAssignments;
      try {
        createdRoleAssignments =
            Failsafe.with(retryPolicy).get(() -> roleAssignmentService.createMulti(roleAssignments));
        migrationBuilder.createdRoleAssignments(createdRoleAssignments);
      } catch (Exception ex) {
        migrationBuilder.migrationStatus(MigrationStatus.FAILED_AT_ROLE_ASSIGNMENT_CREATION);
        migrationBuilder.failedRoleAssignments(roleAssignments);
      }
    }
    return migrationBuilder.build();
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

  private RoleAssignment getRoleAssignment(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String principalIdentifier, String roleIdentifier) {
    HarnessScopeParams.HarnessScopeParamsBuilder scopeParamsBuilder =
        HarnessScopeParams.builder().accountIdentifier(accountIdentifier);
    if (!StringUtils.isEmpty(orgIdentifier)) {
      scopeParamsBuilder.orgIdentifier(orgIdentifier);
    }
    if (!StringUtils.isEmpty(projectIdentifier)) {
      scopeParamsBuilder.projectIdentifier(projectIdentifier);
    }
    Scope scope = scopeService.buildScopeFromParams(scopeParamsBuilder.build());
    return RoleAssignment.builder()
        .disabled(false)
        .identifier("role_assignment_".concat(CryptoUtils.secureRandAlphaNumString(20)))
        .managed(false)
        .roleIdentifier(roleIdentifier)
        .principalType(PrincipalType.USER)
        .principalIdentifier(principalIdentifier)
        .resourceGroupIdentifier(ALL_RESOURCES_RESOURCE_GROUP)
        .scopeIdentifier(scope.toString())
        .build();
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
      ResourceGroupResponse resourceGroupResponse = NGRestUtils.getResponse(
          resourceGroupClient.upsert(accountIdentifier, orgIdentifier, projectIdentifier, resourceGroupDTO));
      if (resourceGroupResponse == null) {
        log.info("Resource group already exists: {}", resourceGroupDTO);
      }
      harnessResourceGroupService.sync(resourceGroupIdentifier,
          scopeService.buildScopeFromParams(HarnessScopeParams.builder()
                                                .accountIdentifier(accountIdentifier)
                                                .orgIdentifier(orgIdentifier)
                                                .projectIdentifier(projectIdentifier)
                                                .build()));
      return true;
    } catch (Exception exception) {
      log.error("Error while creating default resource group: {}", resourceGroupDTO, exception);
      return false;
    }
  }
}
