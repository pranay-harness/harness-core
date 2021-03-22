package io.harness.accesscontrol.migrations;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.harness.accesscontrol.commons.events.EventHandler;
import io.harness.accesscontrol.principals.PrincipalType;
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
import io.harness.resourcegroup.model.DynamicResourceSelector;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroup.remote.dto.ResourceTypeDTO;
import io.harness.resourcegroupclient.ResourceGroupResponse;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;
import io.harness.utils.CryptoUtils;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class FeatureFlagEventHandler implements EventHandler {
  private static final String NG_RBAC_ENABLED = "NG_RBAC_ENABLED";
  private static final String CG_ACCOUNT_ADMINISTRATOR_USER_GROUP = "Account Administrator";
  private static final String ACCOUNT_ADMIN_ROLE_IDENTIFIER = "_account_admin";
  private static final String ORG_ADMIN_ROLE_IDENTIFIER = "_organization_admin";
  private static final String PROJECT_ADMIN_ROLE_IDENTIFIER = "_project_admin";
  private static final String ACCOUNT_VIEWER_ROLE_IDENTIFIER = "_account_viewer";
  private static final String ORG_VIEWER_ROLE_IDENTIFIER = "_org_viewer";
  private static final String PROJECT_VIEWER_ROLE_IDENTIFIER = "_project_viewer";
  private static final String ALL_RESOURCES_RESOURCE_GROUP = "_all_resources";
  private final UserClient userClient;
  private final ProjectManagerClient projectClient;
  private final OrganizationManagerClient orgClient;
  private final RoleAssignmentService roleAssignmentService;
  private final ResourceGroupClient resourceGroupClient;
  private final ScopeService scopeService;

  private void handleNGRBACEnabledFlag(String accountId) throws IOException, InterruptedException {
    PageResponse<User> users = RestClientUtils.getResponse(userClient.list(accountId, "0", "100000", null, true));
    if (users == null) {
      log.info("No user present in account: {}", accountId);
      return;
    }
    for (User user : users) {
      if (user.getUserGroups() == null) {
        user.setUserGroups(new ArrayList<>());
      }
      String principalIdentifier = user.getUuid();
      boolean admin =
          user.getUserGroups().stream().anyMatch(x -> CG_ACCOUNT_ADMINISTRATOR_USER_GROUP.equals(x.getName()));
      if (admin) {
        boolean accountResourceGroupCreationStatus =
            upsertResourceGroup(accountId, null, null, ALL_RESOURCES_RESOURCE_GROUP);

        List<RoleAssignment> roleAssignments = new ArrayList<>();
        if (accountResourceGroupCreationStatus) {
          roleAssignments.add(getRoleAssignment(accountId, null, null, user.getUuid(), ACCOUNT_ADMIN_ROLE_IDENTIFIER));
          List<OrganizationResponse> orgs = orgClient.listOrganization(accountId, "", 0, 100000, new ArrayList<>())
                                                .execute()
                                                .body()
                                                .getData()
                                                .getContent();
          for (OrganizationResponse org : orgs) {
            upsertResourceGroup(accountId, org.getOrganization().getIdentifier(), null, ALL_RESOURCES_RESOURCE_GROUP);
            roleAssignments.add(getRoleAssignment(accountId, org.getOrganization().getIdentifier(), null,
                principalIdentifier, ORG_ADMIN_ROLE_IDENTIFIER));
            List<ProjectResponse> projects = projectClient
                                                 .listProject(accountId, org.getOrganization().getIdentifier(), false,
                                                     null, "", 0, 10000, new ArrayList<>())
                                                 .execute()
                                                 .body()
                                                 .getData()
                                                 .getContent();
            for (ProjectResponse project : projects) {
              upsertResourceGroup(accountId, org.getOrganization().getIdentifier(),
                  project.getProject().getIdentifier(), ALL_RESOURCES_RESOURCE_GROUP);
              roleAssignments.add(getRoleAssignment(accountId, org.getOrganization().getIdentifier(),
                  project.getProject().getIdentifier(), principalIdentifier, ORG_ADMIN_ROLE_IDENTIFIER));
            }
          }
        }
        Thread.sleep(3000);
        createRoleAssignments(roleAssignments);
      }
    }
  }

  private void createRoleAssignments(List<RoleAssignment> roleAssignments) {
    roleAssignments.forEach(roleAssignmentService::create);
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
    ResourceTypeDTO resourceTypeDTO = NGRestUtils.getResponse(
        resourceGroupClient.getResourceTypes(accountIdentifier, orgIdentifier, projectIdentifier));
    ResourceGroupDTO resourceGroupDTO =
        ResourceGroupDTO.builder()
            .accountIdentifier(accountIdentifier)
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .identifier(ALL_RESOURCES_RESOURCE_GROUP)
            .fullScopeSelected(false)
            .resourceSelectors(resourceTypeDTO.getResourceTypes()
                                   .stream()
                                   .map(x -> DynamicResourceSelector.builder().resourceType(x.getName()).build())
                                   .collect(Collectors.toList()))
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
      return true;
    } catch (Exception exception) {
      log.error("Error while creating default resource group: {}", resourceGroupDTO, exception);
      return false;
    }
  }
}
