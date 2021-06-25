package io.harness.ng.accesscontrol.migrations.services;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentCreateRequestDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageResponse;
import io.harness.beans.Scope;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.GeneralException;
import io.harness.ng.accesscontrol.migrations.dao.AccessControlMigrationDAO;
import io.harness.ng.accesscontrol.migrations.models.AccessControlMigration;
import io.harness.ng.accesscontrol.mockserver.MockRoleAssignment.MockRoleAssignmentKeys;
import io.harness.ng.accesscontrol.mockserver.MockRoleAssignmentService;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Organization.OrganizationKeys;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.entities.Project.ProjectKeys;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.UserMembershipUpdateSource;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.ng.core.user.entities.UserMembership.UserMembershipKeys;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.remote.client.NGRestUtils;
import io.harness.remote.client.RestClientUtils;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;
import io.harness.user.remote.UserClient;
import io.harness.utils.CryptoUtils;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.PL)
@Slf4j
public class AccessControlMigrationServiceImpl implements AccessControlMigrationService {
  public static final int BATCH_SIZE = 50;
  public static final String ALL_RESOURCES = "_all_resources";
  private final AccessControlMigrationDAO accessControlMigrationDAO;
  private final ProjectService projectService;
  private final OrganizationService organizationService;
  private final MockRoleAssignmentService mockRoleAssignmentService;
  @Named("PRIVILEGED") private final AccessControlAdminClient accessControlAdminClient;
  private final UserClient userClient;
  private final ResourceGroupClient resourceGroupClient;
  private final NgUserService ngUserService;
  private final ExecutorService executorService =
      Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);

  @Override
  public void save(AccessControlMigration accessControlMigration) {
    try {
      accessControlMigrationDAO.save(accessControlMigration);
    } catch (DuplicateFieldException | DuplicateKeyException duplicateException) {
      // Ignore
    }
  }

  @Override
  public boolean isMigrated(Scope scope) {
    return accessControlMigrationDAO.isMigrated(scope);
  }

  @Override
  public void migrate(String accountIdentifier) {
    migrateInternal(Scope.of(accountIdentifier, null, null));
    for (String orgIdentifier : getOrganizations(accountIdentifier)) {
      migrateInternal(Scope.of(accountIdentifier, orgIdentifier, null));
      for (String projectIdentifier : getProjects(accountIdentifier, orgIdentifier)) {
        migrateInternal(Scope.of(accountIdentifier, orgIdentifier, projectIdentifier));
      }
    }
  }

  private List<UserInfo> getUsers(String accountId) {
    int offset = 0;
    int limit = 500;
    int maxIterations = 50;
    Set<UserInfo> users = new HashSet<>();
    while (maxIterations > 0) {
      PageResponse<UserInfo> usersPage = RestClientUtils.getResponse(
          userClient.list(accountId, String.valueOf(offset), String.valueOf(limit), null, true));
      if (isEmpty(usersPage.getResponse())) {
        break;
      }
      users.addAll(usersPage.getResponse());
      maxIterations--;
      offset += limit;
    }
    return new ArrayList<>(users);
  }

  private long createRoleAssignments(Scope scope, boolean managed, List<RoleAssignmentDTO> roleAssignments) {
    List<List<RoleAssignmentDTO>> batchedRoleAssignments = Lists.partition(roleAssignments, BATCH_SIZE);
    List<Future<List<RoleAssignmentResponseDTO>>> futures = new ArrayList<>();
    batchedRoleAssignments.forEach(batch
        -> futures.add(executorService.submit(
            ()
                -> NGRestUtils.getResponse(accessControlAdminClient.createMultiRoleAssignment(
                    scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier(), managed,
                    RoleAssignmentCreateRequestDTO.builder().roleAssignments(batch).build())))));

    long createdRoleAssignments = 0;
    for (Future<List<RoleAssignmentResponseDTO>> future : futures) {
      try {
        createdRoleAssignments += future.get().size();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new GeneralException(
            String.format("Error occurred while trying to create role assignments for scope : %s", scope), ex);
      } catch (ExecutionException ex) {
        throw new GeneralException(
            String.format("Error occurred while trying to create role assignments for scope : %s", scope),
            ex.getCause());
      }
    }
    return createdRoleAssignments;
  }

  private void migrateInternal(Scope scope) {
    if (isMigrated(scope)) {
      log.info("Scope {} already migrated", scope);
      return;
    }
    log.info("Access control migration started for scope : {}", scope);
    Stopwatch stopwatch = Stopwatch.createStarted();

    try {
      ensureManagedResourceGroup(scope);
      migrateMockRoleAssignments(scope);
      assignViewerRoleToUsers(scope);
      if (!hasAdmin(scope)) {
        assignAdminRoleToUsers(scope);
      }

      if (!hasAdmin(scope)) {
        assignAdminAndViewerRoleToCGUsers(scope);
      }

      long durationInSeconds = stopwatch.elapsed(TimeUnit.SECONDS);
      log.info(
          "Access control migration finished for scope : {} in {} seconds", scope, stopwatch.elapsed(TimeUnit.SECONDS));

      save(AccessControlMigration.builder()
               .accountIdentifier(scope.getAccountIdentifier())
               .orgIdentifier(scope.getOrgIdentifier())
               .projectIdentifier(scope.getProjectIdentifier())
               .durationInSeconds(durationInSeconds)
               .build());
    } catch (Exception ex) {
      log.error(String.format("Access control migration failed for scope : %s", scope.toString()), ex);
    }
  }

  private void assignViewerRoleToUsers(Scope scope) {
    List<String> users = getUsersInScope(scope);
    if (users.isEmpty()) {
      return;
    }

    log.info("Created {} MANAGED role assignments from UserMembership for scope: {}",
        createRoleAssignments(scope, true, buildRoleAssignments(users, getManagedViewerRole(scope))), scope);
  }

  private boolean hasAdmin(Scope scope) {
    return !isEmpty(ngUserService.listUsersHavingRole(scope, getManagedAdminRole(scope)));
  }

  private void ensureManagedResourceGroup(Scope scope) {
    NGRestUtils.getResponse(resourceGroupClient.createManagedResourceGroup(
        scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier()));
  }

  private void migrateMockRoleAssignments(Scope scope) {
    List<RoleAssignmentResponseDTO> mockRoleAssignments = getMockRoleAssignments(scope);
    if (mockRoleAssignments.isEmpty()) {
      return;
    }

    List<RoleAssignmentDTO> managedRoleAssignments = new ArrayList<>();
    List<RoleAssignmentDTO> nonManagedRoleAssignments = new ArrayList<>();
    mockRoleAssignments.forEach(mockRoleAssignment -> {
      upsertUserMembership(scope, mockRoleAssignment.getRoleAssignment().getPrincipal().getIdentifier());

      if (Boolean.TRUE.equals(mockRoleAssignment.getRoleAssignment().isManaged())) {
        managedRoleAssignments.add(mockRoleAssignment.getRoleAssignment());
      } else {
        nonManagedRoleAssignments.add(mockRoleAssignment.getRoleAssignment());
      }
    });

    log.info("Created {} MANAGED role assignments from MockRoleAssignments for scope: {}",
        createRoleAssignments(scope, true, managedRoleAssignments), scope);
    log.info("Created {} NON-MANAGED role assignments from MockRoleAssignments for scope: {}",
        createRoleAssignments(scope, false, nonManagedRoleAssignments), scope);
  }

  private void assignAdminRoleToUsers(Scope scope) {
    List<String> users = getUsersInScope(scope);
    if (users.isEmpty()) {
      return;
    }

    log.info("Created {} NON-MANAGED role assignments from UserMembership for scope: {}",
        createRoleAssignments(scope, false, buildRoleAssignments(users, getManagedAdminRole(scope))), scope);
  }

  private void assignAdminAndViewerRoleToCGUsers(Scope scope) {
    List<String> currentGenUsers =
        getUsers(scope.getAccountIdentifier()).stream().map(UserInfo::getUuid).collect(Collectors.toList());
    if (currentGenUsers.isEmpty()) {
      return;
    }

    currentGenUsers.forEach(userId -> upsertUserMembership(scope, userId));

    log.info("Created {} MANAGED role assignments from CG Users for scope: {}",
        createRoleAssignments(scope, true, buildRoleAssignments(currentGenUsers, getManagedViewerRole(scope))), scope);

    log.info("Created {} NON-MANAGED role assignments from CG Users for scope: {}",
        createRoleAssignments(scope, false, buildRoleAssignments(currentGenUsers, getManagedAdminRole(scope))), scope);
  }

  private void upsertUserMembership(Scope scope, String userId) {
    try {
      ngUserService.addUserToScope(userId,
          Scope.builder()
              .accountIdentifier(scope.getAccountIdentifier())
              .orgIdentifier(scope.getOrgIdentifier())
              .projectIdentifier(scope.getProjectIdentifier())
              .build(),
          false, UserMembershipUpdateSource.SYSTEM);
    } catch (DuplicateKeyException | DuplicateFieldException duplicateException) {
      // ignore
    }
  }

  private static String getManagedViewerRole(Scope scope) {
    if (!StringUtils.isEmpty(scope.getProjectIdentifier())) {
      return "_project_viewer";
    } else if (!StringUtils.isEmpty(scope.getOrgIdentifier())) {
      return "_organization_viewer";
    } else {
      return "_account_viewer";
    }
  }

  private static String getManagedAdminRole(Scope scope) {
    if (!StringUtils.isEmpty(scope.getProjectIdentifier())) {
      return "_project_admin";
    } else if (!StringUtils.isEmpty(scope.getOrgIdentifier())) {
      return "_organization_admin";
    } else {
      return "_account_admin";
    }
  }

  private List<RoleAssignmentDTO> buildRoleAssignments(List<String> userIds, String roleIdentifier) {
    return userIds.stream()
        .map(userId
            -> RoleAssignmentDTO.builder()
                   .disabled(false)
                   .identifier("role_assignment_".concat(CryptoUtils.secureRandAlphaNumString(20)))
                   .roleIdentifier(roleIdentifier)
                   .resourceGroupIdentifier(ALL_RESOURCES)
                   .principal(PrincipalDTO.builder().identifier(userId).type(PrincipalType.USER).build())
                   .build())
        .collect(Collectors.toList());
  }

  private List<String> getOrganizations(String accountIdentifier) {
    return organizationService
        .list(Criteria.where(OrganizationKeys.accountIdentifier)
                  .is(accountIdentifier)
                  .and(OrganizationKeys.deleted)
                  .ne(true))
        .stream()
        .map(Organization::getIdentifier)
        .collect(Collectors.toList());
  }

  private List<String> getProjects(String accountIdentifier, String orgIdentifier) {
    return projectService
        .list(Criteria.where(ProjectKeys.accountIdentifier)
                  .is(accountIdentifier)
                  .and(ProjectKeys.deleted)
                  .ne(true)
                  .and(ProjectKeys.orgIdentifier)
                  .is(orgIdentifier))
        .stream()
        .map(Project::getIdentifier)
        .collect(Collectors.toList());
  }

  private List<RoleAssignmentResponseDTO> getMockRoleAssignments(Scope scope) {
    return mockRoleAssignmentService
        .list(Criteria.where(MockRoleAssignmentKeys.accountIdentifier)
                  .is(scope.getAccountIdentifier())
                  .and(MockRoleAssignmentKeys.orgIdentifier)
                  .is(scope.getOrgIdentifier())
                  .and(MockRoleAssignmentKeys.projectIdentifier)
                  .is(scope.getProjectIdentifier()),
            Pageable.unpaged())
        .getContent();
  }

  private List<String> getUsersInScope(Scope scope) {
    return ngUserService
        .listUserMemberships(Criteria.where(UserMembershipKeys.scopes + ".accountIdentifier")
                                 .is(scope.getAccountIdentifier())
                                 .and(UserMembershipKeys.scopes + ".orgIdentifier")
                                 .is(scope.getOrgIdentifier())
                                 .and(UserMembershipKeys.scopes + ".projectIdentifier")
                                 .is(scope.getProjectIdentifier()))
        .stream()
        .map(UserMembership::getUserId)
        .collect(Collectors.toList());
  }
}
