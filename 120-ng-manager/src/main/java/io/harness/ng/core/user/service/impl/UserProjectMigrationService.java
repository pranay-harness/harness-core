package io.harness.ng.core.user.service.impl;

import static io.harness.AuthorizationServiceHeader.NG_MANAGER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.core.user.UserMembershipUpdateSource.SYSTEM;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.ng.core.invites.entities.Role;
import io.harness.ng.core.user.entities.UserProjectMap;
import io.harness.ng.core.user.entities.UserProjectMap.UserProjectMapKeys;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.remote.client.NGRestUtils;
import io.harness.repositories.user.spring.UserProjectMapRepository;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ServicePrincipal;

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
public class UserProjectMigrationService implements Managed {
  private static final String DEFAULT_RESOURCE_GROUP_IDENTIFIER = "_all_resources";
  private final UserProjectMapRepository userProjectMapRepository;
  private final NgUserService ngUserService;
  private final AccessControlAdminClient accessControlAdminClient;
  private final ExecutorService executorService = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("usermembership-migration-worker-thread").build());
  private Future userMembershipMigrationJob;
  Map<String, String> oldToNewRoleMap = new HashMap<>();

  @Inject
  public UserProjectMigrationService(UserProjectMapRepository userProjectMapRepository, NgUserService ngUserService,
      AccessControlAdminClient accessControlAdminClient) {
    this.userProjectMapRepository = userProjectMapRepository;
    this.ngUserService = ngUserService;
    this.accessControlAdminClient = accessControlAdminClient;
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
  public void start() {
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
    log.info("Starting migration of UserProjectMap to UserMembership");
    SecurityContextBuilder.setContext(new ServicePrincipal(NG_MANAGER.getServiceId()));
    try {
      Criteria criteria = Criteria.where(UserProjectMapKeys.moved)
                              .exists(false)
                              .orOperator(Criteria.where(UserProjectMapKeys.tries).exists(false),
                                  Criteria.where(UserProjectMapKeys.tries).lt(3));
      Optional<UserProjectMap> userProjectMapOptional = userProjectMapRepository.findFirstByCriteria(criteria);
      while (userProjectMapOptional.isPresent()) {
        UserProjectMap userProjectMap = userProjectMapOptional.get();
        try {
          handleMigration(userProjectMap);
          userProjectMap.setMoved(true);
        } finally {
          int triesDone = userProjectMap.getTries() != null ? userProjectMap.getTries() : 0;
          userProjectMap.setTries(triesDone + 1);
        }
        userProjectMapRepository.save(userProjectMap);
        userProjectMapOptional = userProjectMapRepository.findFirstByCriteria(criteria);
      }
    } catch (Exception exception) {
      log.error("Exception occurred during migration of UserProjectMap to UserMembership", exception);
    }
    SecurityContextBuilder.unsetCompleteContext();
    log.info("Completed migration of UserProjectMap to UserMembership");
  }

  private void handleMigration(UserProjectMap userProjectMap) {
    Scope scope = Scope.builder()
                      .accountIdentifier(userProjectMap.getAccountIdentifier())
                      .orgIdentifier(userProjectMap.getOrgIdentifier())
                      .projectIdentifier(userProjectMap.getProjectIdentifier())
                      .build();
    ngUserService.addUserToScope(userProjectMap.getUserId(), scope, null, SYSTEM);

    //    Create role assignment for the user
    List<Role> roles = userProjectMap.getRoles();
    for (Role role : roles) {
      if (!oldToNewRoleMap.containsKey(role.getName())) {
        log.error("unidentified rolename {} found while migrating userProjectMap to userMembership", role.getName());
        continue;
      }
      RoleAssignmentDTO roleAssignmentDTO =
          RoleAssignmentDTO.builder()
              .roleIdentifier(oldToNewRoleMap.get(role.getName()))
              .disabled(false)
              .resourceGroupIdentifier(DEFAULT_RESOURCE_GROUP_IDENTIFIER)
              .principal(PrincipalDTO.builder().identifier(userProjectMap.getUserId()).type(PrincipalType.USER).build())
              .build();
      try {
        NGRestUtils.getResponse(accessControlAdminClient.createRoleAssignment(userProjectMap.getAccountIdentifier(),
            userProjectMap.getOrgIdentifier(), userProjectMap.getProjectIdentifier(), roleAssignmentDTO));
      } catch (Exception e) {
        log.error("Couldn't migrate role {} for user {}", role, userProjectMap.getUserId(), e);
      }
    }
  }
}