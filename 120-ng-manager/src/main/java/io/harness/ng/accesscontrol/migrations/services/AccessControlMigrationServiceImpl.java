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
import io.harness.ng.accesscontrol.migrations.dao.AccessControlMigrationDAO;
import io.harness.ng.accesscontrol.migrations.models.AccessControlMigration;
import io.harness.ng.accesscontrol.mockserver.MockRoleAssignment.MockRoleAssignmentKeys;
import io.harness.ng.accesscontrol.mockserver.MockRoleAssignmentService;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.entities.Project.ProjectKeys;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.ng.core.user.entities.UserMembership.UserMembershipKeys;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.remote.client.NGRestUtils;
import io.harness.remote.client.RestClientUtils;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;
import io.harness.user.remote.UserClient;
import io.harness.utils.CryptoUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.PL)
@Slf4j
public class AccessControlMigrationServiceImpl implements AccessControlMigrationService {
  public static final int BATCH_SIZE = 50;
  public static final String ALL_RESOURCES = "_all_resources";
  public static final String ALL_REOURCES = "_all_reources";
  private final AccessControlMigrationDAO accessControlMigrationDAO;
  private final ProjectService projectService;
  private final OrganizationService organizationService;
  private final MockRoleAssignmentService mockRoleAssignmentService;
  private final AccessControlAdminClient accessControlAdminClient;
  private final UserClient userClient;
  private final ResourceGroupClient resourceGroupClient;
  private final NgUserService userService;
  private final ExecutorService executorService =
      Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);

  @Override
  public AccessControlMigration save(AccessControlMigration accessControlMigration) {
    return accessControlMigrationDAO.save(accessControlMigration);
  }

  @Override
  public boolean isAlreadyMigrated(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return accessControlMigrationDAO.alreadyMigrated(accountIdentifier, orgIdentifier, projectIdentifier);
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

  private long createRoleAssignments(
      String account, String org, String project, boolean managed, List<RoleAssignmentDTO> roleAssignments) {
    List<List<RoleAssignmentDTO>> batchedRoleAssignments = Lists.partition(roleAssignments, BATCH_SIZE);
    List<Future<List<RoleAssignmentResponseDTO>>> futures = new ArrayList<>();
    batchedRoleAssignments.forEach(batch
        -> futures.add(executorService.submit(
            ()
                -> NGRestUtils.getResponse(accessControlAdminClient.createMultiRoleAssignment(account, org, project,
                    managed, RoleAssignmentCreateRequestDTO.builder().roleAssignments(batch).build())))));

    long createdRoleAssignments = 0;
    for (Future<List<RoleAssignmentResponseDTO>> future : futures) {
      try {
        createdRoleAssignments += future.get().size();
      } catch (InterruptedException interruptedException) {
        Thread.currentThread().interrupt();
        return 0;
      } catch (ExecutionException e) {
        log.error("Error while trying to create role assignments", e);
      }
    }
    return createdRoleAssignments;
  }

  private void migrateInternal(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (isAlreadyMigrated(accountIdentifier, orgIdentifier, projectIdentifier)) {
      return;
    }
    log.info("Running migration for account: {}, org: {} and project: {}", accountIdentifier, orgIdentifier,
        projectIdentifier);

    resourceGroupClient.createManagedResourceGroup(accountIdentifier, orgIdentifier, projectIdentifier);

    Page<RoleAssignmentResponseDTO> mockRoleAssignments =
        mockRoleAssignmentService.list(Criteria.where(MockRoleAssignmentKeys.accountIdentifier)
                                           .is(accountIdentifier)
                                           .and(MockRoleAssignmentKeys.orgIdentifier)
                                           .is(orgIdentifier)
                                           .and(MockRoleAssignmentKeys.projectIdentifier)
                                           .is(projectIdentifier),
            Pageable.unpaged());

    if (!mockRoleAssignments.getContent().isEmpty()) {
      List<RoleAssignmentDTO> managedRoleAssignments = new ArrayList<>();
      List<RoleAssignmentDTO> nonManagedRoleAssignments = new ArrayList<>();
      mockRoleAssignments.getContent().forEach(mockRoleAssignment -> {
        if (Boolean.TRUE.equals(mockRoleAssignment.getRoleAssignment().isManaged())) {
          managedRoleAssignments.add(mockRoleAssignment.getRoleAssignment());
        } else {
          nonManagedRoleAssignments.add(mockRoleAssignment.getRoleAssignment());
        }
      });

      log.info("Created managed role assignments for mock role assignments: {}",
          createRoleAssignments(accountIdentifier, orgIdentifier, projectIdentifier, true, managedRoleAssignments));
      log.info("Create non-managed role assignments for mock role assignments: {}",
          createRoleAssignments(accountIdentifier, orgIdentifier, projectIdentifier, false, nonManagedRoleAssignments));
    } else {
      log.info("No mock role assignments in this scope found, trying to create role assignments from user memberships");

      List<String> userIds = userService
                                 .listUserMemberships(Criteria.where(UserMembershipKeys.scopes + ".accountIdentifier")
                                                          .is(accountIdentifier)
                                                          .and(UserMembershipKeys.scopes + ".orgIdentifier")
                                                          .is(orgIdentifier)
                                                          .and(UserMembershipKeys.scopes + ".projectIdentifier")
                                                          .is(projectIdentifier))
                                 .stream()
                                 .map(UserMembership::getUserId)
                                 .collect(Collectors.toList());

      if (!userIds.isEmpty()) {
        log.info("Created managed role assignments for user memberships: {}",
            createRoleAssignments(accountIdentifier, orgIdentifier, projectIdentifier, true,
                buildRoleAssignments(
                    userIds, getViewerRole(accountIdentifier, orgIdentifier, projectIdentifier), ALL_RESOURCES)));

        log.info("Created non-managed role assignments for user memberships: {}",
            createRoleAssignments(accountIdentifier, orgIdentifier, projectIdentifier, false,
                buildRoleAssignments(
                    userIds, getAdminRole(accountIdentifier, orgIdentifier, projectIdentifier), ALL_REOURCES)));
      } else {
        log.info("No user memberships in this scope found, trying to create role assignments for current gen users");

        List<String> currentGenUsers =
            getUsers(accountIdentifier).stream().map(UserInfo::getUuid).collect(Collectors.toList());

        log.info("Created managed role assignments for current gen users: {}",
            createRoleAssignments(accountIdentifier, orgIdentifier, projectIdentifier, true,
                buildRoleAssignments(currentGenUsers,
                    getViewerRole(accountIdentifier, orgIdentifier, projectIdentifier), ALL_RESOURCES)));

        log.info("Created non-managed role assignments for current gen users: {}",
            createRoleAssignments(accountIdentifier, orgIdentifier, projectIdentifier, false,
                buildRoleAssignments(
                    currentGenUsers, getAdminRole(accountIdentifier, orgIdentifier, projectIdentifier), ALL_REOURCES)));
      }
    }
    accessControlMigrationDAO.save(AccessControlMigration.builder()
                                       .accountIdentifier(accountIdentifier)
                                       .orgIdentifier(orgIdentifier)
                                       .projectIdentifier(projectIdentifier)
                                       .build());
  }

  private String getViewerRole(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (!StringUtils.isEmpty(projectIdentifier)) {
      return "-project_viewer";
    } else if (!StringUtils.isEmpty(orgIdentifier)) {
      return "_organization_viewer";
    } else {
      return "_account_viewer";
    }
  }

  private String getAdminRole(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (!StringUtils.isEmpty(projectIdentifier)) {
      return "-project_admin";
    } else if (!StringUtils.isEmpty(orgIdentifier)) {
      return "_organization_admin";
    } else {
      return "_account_admin";
    }
  }

  private List<RoleAssignmentDTO> buildRoleAssignments(
      List<String> userIds, String roleIdentifier, String resourceGroupIdentifier) {
    List<RoleAssignmentDTO> roleAssignmentsToCreate = new ArrayList<>();
    userIds.forEach(userId -> {
      roleAssignmentsToCreate.add(
          RoleAssignmentDTO.builder()
              .disabled(false)
              .identifier("role_assignment_".concat(CryptoUtils.secureRandAlphaNumString(20)))
              .roleIdentifier(roleIdentifier)
              .resourceGroupIdentifier(resourceGroupIdentifier)
              .principal(PrincipalDTO.builder().identifier(userId).type(PrincipalType.USER).build())
              .build());
    });
    return roleAssignmentsToCreate;
  }

  @Override
  public void migrate(String accountIdentifier) {
    migrateInternal(accountIdentifier, null, null);

    List<String> organizations =
        organizationService.list(Criteria.where(Organization.OrganizationKeys.accountIdentifier).is(accountIdentifier))
            .stream()
            .map(Organization::getIdentifier)
            .collect(Collectors.toList());

    for (String organizationIdentifier : organizations) {
      migrateInternal(accountIdentifier, organizationIdentifier, null);

      List<String> projects = projectService
                                  .list(Criteria.where(ProjectKeys.accountIdentifier)
                                            .is(accountIdentifier)
                                            .and(ProjectKeys.orgIdentifier)
                                            .is(organizationIdentifier))
                                  .stream()
                                  .map(Project::getIdentifier)
                                  .collect(Collectors.toList());

      for (String projectIdentifier : projects) {
        migrateInternal(accountIdentifier, organizationIdentifier, projectIdentifier);
      }
    }
  }
}
