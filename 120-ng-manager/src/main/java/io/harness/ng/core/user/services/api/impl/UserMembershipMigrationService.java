package io.harness.ng.core.user.services.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.entities.Role;
import io.harness.ng.core.invites.entities.UserMembership;
import io.harness.ng.core.invites.entities.UserProjectMap;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.remote.client.NGRestUtils;
import io.harness.repositories.invites.spring.UserProjectMapRepository;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

// Migrate UserProjectMap collection to UserMembership Collection
@Slf4j
@OwnedBy(PL)
public class UserMembershipMigrationService implements Managed {
  private final UserProjectMapRepository userProjectMapRepository;
  private final NgUserService ngUserService;
  private final AccessControlAdminClient accessControlAdminClient;
  private final ResourceGroupClient resourceGroupClient;
  private final ExecutorService executorService = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("usermembership-migration-worker-thread").build());
  private Future userMembershipMigrationJob;
  Map<String, String> oldToNewRoleMap = new HashMap<>();

  @Inject
  public UserMembershipMigrationService(UserProjectMapRepository userProjectMapRepository, NgUserService ngUserService,
      AccessControlAdminClient accessControlAdminClient, ResourceGroupClient resourceGroupClient) {
    this.userProjectMapRepository = userProjectMapRepository;
    this.ngUserService = ngUserService;
    this.accessControlAdminClient = accessControlAdminClient;
    this.resourceGroupClient = resourceGroupClient;
    oldToNewRoleMap.put("Project Viewer", "_project_viewer");
    oldToNewRoleMap.put("Project Member", "_project_viewer");
    oldToNewRoleMap.put("Project Admin", "_project_admin");
    oldToNewRoleMap.put("Organization Viewer", "_organization_viewer");
    oldToNewRoleMap.put("Organization Member", "_organization_viewer");
    oldToNewRoleMap.put("Organization Admin", "_organization_admin");
    oldToNewRoleMap.put("Account Viewer", "_account_viewer");
    oldToNewRoleMap.put("Account Member", "_account_viewer");
    oldToNewRoleMap.put("Account Admin", "_account_admin");
  }

  @Override
  public void start() throws Exception {
    userMembershipMigrationJob = executorService.submit(this::userMembershipMigrationJob);
  }

  @Override
  public void stop() throws Exception {
    if (userMembershipMigrationJob != null) {
      userMembershipMigrationJob.cancel(true);
    }
    executorService.shutdown();
    executorService.awaitTermination(5, TimeUnit.SECONDS);
  }

  private void userMembershipMigrationJob() {
    Criteria criteria = Criteria.where(UserProjectMap.UserProjectMapKeys.migrated).exists(false);
    Optional<UserProjectMap> userProjectMapOptional = userProjectMapRepository.findFirstByCriteria(criteria);
    while (userProjectMapOptional.isPresent()) {
      UserProjectMap userProjectMap = userProjectMapOptional.get();
      handleMigration(userProjectMap);
      userProjectMap.setMigrated(true);
      userProjectMapRepository.save(userProjectMap);
      userProjectMapOptional = userProjectMapRepository.findFirstByCriteria(criteria);
    }
  }

  private void handleMigration(UserProjectMap userProjectMap) {
    UserMembership.Scope scope = UserMembership.Scope.builder()
                                     .accountIdentifier(userProjectMap.getAccountIdentifier())
                                     .orgIdentifier(userProjectMap.getOrgIdentifier())
                                     .projectIdentifier(userProjectMap.getProjectIdentifier())
                                     .build();
    ngUserService.addUserToScope(userProjectMap.getUserId(), scope);

    //    Create role assignment for the user
    List<Role> roles = userProjectMap.getRoles();
    roles.forEach(role -> {
      try {
        Preconditions.checkState(oldToNewRoleMap.containsKey(role.getName()),
            "unidentified role name found while migrating userProjectMap to userMembership");
        NGRestUtils.getResponse(resourceGroupClient.ensureDefaultResourceGroup(userProjectMap.getAccountIdentifier(),
            userProjectMap.getOrgIdentifier(), userProjectMap.getProjectIdentifier()));
        RoleAssignmentDTO roleAssignmentDTO =
            RoleAssignmentDTO.builder()
                .roleIdentifier(oldToNewRoleMap.get(role.getName()))
                .disabled(false)
                .resourceGroupIdentifier(getDefaultResourceGroupIdentifier(userProjectMap.getAccountIdentifier(),
                    userProjectMap.getOrgIdentifier(), userProjectMap.getProjectIdentifier()))
                .principal(
                    PrincipalDTO.builder().identifier(userProjectMap.getUserId()).type(PrincipalType.USER).build())
                .build();
        NGRestUtils.getResponse(accessControlAdminClient.createRoleAssignment(userProjectMap.getAccountIdentifier(),
            userProjectMap.getOrgIdentifier(), userProjectMap.getProjectIdentifier(), roleAssignmentDTO));
      } catch (Exception e) {
        log.error("Couldn't migrate role {} for user {}", role, userProjectMap.getUserId(), e);
      }
    });
  }

  private String getDefaultResourceGroupIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    String defaultResourceGroupName;
    if (!isBlank(projectIdentifier)) {
      defaultResourceGroupName = projectIdentifier;
    } else if (!isBlank(orgIdentifier)) {
      defaultResourceGroupName = orgIdentifier;
    } else {
      defaultResourceGroupName = accountIdentifier;
    }
    return String.format("_%s", defaultResourceGroupName);
  }
}
