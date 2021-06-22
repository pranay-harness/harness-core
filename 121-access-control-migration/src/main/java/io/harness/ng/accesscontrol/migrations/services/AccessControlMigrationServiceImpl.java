package io.harness.ng.accesscontrol.migrations.services;

import static io.harness.NGConstants.DEFAULT_RESOURCE_GROUP_IDENTIFIER;
import static io.harness.NGConstants.DEFAULT_RESOURCE_GROUP_NAME;
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
import io.harness.ng.accesscontrol.migrations.dao.AccessControlMigrationDAO;
import io.harness.ng.accesscontrol.migrations.models.AccessControlMigration;
import io.harness.ng.accesscontrol.mockserver.models.MockRoleAssignment.MockRoleAssignmentKeys;
import io.harness.ng.accesscontrol.mockserver.services.MockRoleAssignmentService;
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
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;
import io.harness.user.remote.UserClient;
import io.harness.utils.CryptoUtils;
import io.harness.utils.ScopeUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
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
  private final AccessControlAdminClient accessControlAdminClient;
  private final UserClient userClient;
  private final ResourceGroupClient resourceGroupClient;
  private final NgUserService ngUserService;
  private final ExecutorService executorService =
      Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);
  private static final String DESCRIPTION_FORMAT = "All the resources in this %s are included in this resource group.";

  @Override
  public AccessControlMigration save(AccessControlMigration accessControlMigration) {
    try {
      return accessControlMigrationDAO.save(accessControlMigration);
    } catch (DuplicateFieldException | DuplicateKeyException duplicateException) {
      return null;
    }
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

  private void upsertUserMembership(
      String userId, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    try {
      ngUserService.addUserToScope(userId,
          Scope.builder()
              .accountIdentifier(accountIdentifier)
              .orgIdentifier(orgIdentifier)
              .projectIdentifier(projectIdentifier)
              .build(),
          false, UserMembershipUpdateSource.SYSTEM);
    } catch (DuplicateKeyException | DuplicateFieldException duplicateException) {
      // ignore
    }
  }

  private Optional<AccessControlMigration> migrateInternal(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, List<String> currentGenUserIds) {
    if (isAlreadyMigrated(accountIdentifier, orgIdentifier, projectIdentifier)) {
      return Optional.empty();
    }

    AccessControlMigration.AccessControlMigrationBuilder accessControlMigrationBuilder =
        AccessControlMigration.builder()
            .accountIdentifier(accountIdentifier)
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier);
    long createdRoleAssignmentsCount = 0;

    log.info("Running migration for account: {}, org: {} and project: {}", accountIdentifier, orgIdentifier,
        projectIdentifier);

    // create managed resource group if it does not exist already
    boolean resourceGroupCreated = createManagedResourceGroup(accountIdentifier, orgIdentifier, projectIdentifier);
    if (resourceGroupCreated) {
      log.info("Default resource group created for account: {}, org: {} and project: {}", accountIdentifier,
          orgIdentifier, projectIdentifier);
    }

    Page<RoleAssignmentResponseDTO> mockRoleAssignments =
        mockRoleAssignmentService.list(Criteria.where(MockRoleAssignmentKeys.accountIdentifier)
                                           .is(accountIdentifier)
                                           .and(MockRoleAssignmentKeys.orgIdentifier)
                                           .is(orgIdentifier)
                                           .and(MockRoleAssignmentKeys.projectIdentifier)
                                           .is(projectIdentifier),
            Pageable.unpaged());

    if (!mockRoleAssignments.getContent().isEmpty()) {
      mockRoleAssignments.getContent().forEach(mockRoleAssignment -> {
        if (mockRoleAssignment.getRoleAssignment().getPrincipal().getType() == PrincipalType.USER) {
          upsertUserMembership(mockRoleAssignment.getRoleAssignment().getPrincipal().getIdentifier(), accountIdentifier,
              orgIdentifier, projectIdentifier);
        }
      });
      createdRoleAssignmentsCount += createRoleAssignmentsFromMockRoleAssignments(
          accountIdentifier, orgIdentifier, projectIdentifier, mockRoleAssignments);
    } else {
      log.info("No mock role assignments in this scope found, trying to create role assignments from user memberships");

      List<String> userMembershipUserIds =
          ngUserService
              .listUserMemberships(Criteria.where(UserMembershipKeys.scopes + ".accountIdentifier")
                                       .is(accountIdentifier)
                                       .and(UserMembershipKeys.scopes + ".orgIdentifier")
                                       .is(orgIdentifier)
                                       .and(UserMembershipKeys.scopes + ".projectIdentifier")
                                       .is(projectIdentifier))
              .stream()
              .map(UserMembership::getUserId)
              .collect(Collectors.toList());

      if (!userMembershipUserIds.isEmpty()) {
        long createdCount = createRoleAssignments(accountIdentifier, orgIdentifier, projectIdentifier, true,
            buildRoleAssignments(
                userMembershipUserIds, getViewerRole(orgIdentifier, projectIdentifier), ALL_RESOURCES));
        log.info("Created managed role assignments for user memberships: {}", createdCount);
        createdRoleAssignmentsCount += createdCount;

        createdCount = createRoleAssignments(accountIdentifier, orgIdentifier, projectIdentifier, false,
            buildRoleAssignments(userMembershipUserIds, getAdminRole(orgIdentifier, projectIdentifier), ALL_RESOURCES));
        log.info("Created non-managed role assignments for user memberships: {}", createdCount);
        createdRoleAssignmentsCount += createdCount;
      } else {
        log.info("No user memberships in this scope found, trying to create role assignments for current gen users");

        currentGenUserIds.forEach(
            userId -> upsertUserMembership(userId, accountIdentifier, orgIdentifier, projectIdentifier));
        long createdCount = createRoleAssignments(accountIdentifier, orgIdentifier, projectIdentifier, true,
            buildRoleAssignments(currentGenUserIds, getViewerRole(orgIdentifier, projectIdentifier), ALL_RESOURCES));
        log.info("Created managed role assignments for current gen users: {}", createdCount);
        createdRoleAssignmentsCount += createdCount;

        createdCount = createRoleAssignments(accountIdentifier, orgIdentifier, projectIdentifier, false,
            buildRoleAssignments(currentGenUserIds, getAdminRole(orgIdentifier, projectIdentifier), ALL_RESOURCES));
        log.info("Created non-managed role assignments for current gen users: {}", createdCount);
        createdRoleAssignmentsCount += createdCount;
      }
    }
    return Optional.ofNullable(
        accessControlMigrationBuilder.createdRoleAssignments(createdRoleAssignmentsCount).build());
  }

  private long createRoleAssignmentsFromMockRoleAssignments(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, Page<RoleAssignmentResponseDTO> mockRoleAssignments) {
    long createdRoleAssignmentsCount = 0;
    List<RoleAssignmentDTO> managedRoleAssignments = new ArrayList<>();
    List<RoleAssignmentDTO> nonManagedRoleAssignments = new ArrayList<>();
    mockRoleAssignments.getContent().forEach(mockRoleAssignment -> {
      if (Boolean.TRUE.equals(mockRoleAssignment.getRoleAssignment().isManaged())) {
        managedRoleAssignments.add(mockRoleAssignment.getRoleAssignment());
      } else {
        nonManagedRoleAssignments.add(mockRoleAssignment.getRoleAssignment());
      }
    });

    long createdCount;

    createdCount =
        createRoleAssignments(accountIdentifier, orgIdentifier, projectIdentifier, true, managedRoleAssignments);
    log.info("Created managed role assignments for mock role assignments: {}", createdCount);
    createdRoleAssignmentsCount += createdCount;

    createdCount =
        createRoleAssignments(accountIdentifier, orgIdentifier, projectIdentifier, false, nonManagedRoleAssignments);
    log.info("Create non-managed role assignments for mock role assignments: {}", createdCount);
    createdRoleAssignmentsCount += createdCount;

    return createdRoleAssignmentsCount;
  }

  private boolean createManagedResourceGroup(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    ResourceGroupDTO resourceGroupDTO =
        ResourceGroupDTO.builder()
            .accountIdentifier(accountIdentifier)
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .name(DEFAULT_RESOURCE_GROUP_NAME)
            .identifier(DEFAULT_RESOURCE_GROUP_IDENTIFIER)
            .description(String.format(DESCRIPTION_FORMAT,
                ScopeUtils.getMostSignificantScope(accountIdentifier, orgIdentifier, projectIdentifier)
                    .toString()
                    .toLowerCase()))
            .resourceSelectors(Collections.emptyList())
            .fullScopeSelected(true)
            .build();
    return resourceGroupClient.createManagedResourceGroup(
               accountIdentifier, orgIdentifier, projectIdentifier, resourceGroupDTO)
        != null;
  }

  private String getViewerRole(String orgIdentifier, String projectIdentifier) {
    if (!StringUtils.isEmpty(projectIdentifier)) {
      return "_project_viewer";
    } else if (!StringUtils.isEmpty(orgIdentifier)) {
      return "_organization_viewer";
    } else {
      return "_account_viewer";
    }
  }

  private String getAdminRole(String orgIdentifier, String projectIdentifier) {
    if (!StringUtils.isEmpty(projectIdentifier)) {
      return "_project_admin";
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
    List<String> currentGenUserIds =
        getUsers(accountIdentifier).stream().map(UserInfo::getUuid).collect(Collectors.toList());

    Optional<AccessControlMigration> accountAccessControlMigrationOptional =
        migrateInternal(accountIdentifier, null, null, currentGenUserIds);

    List<String> organizations =
        organizationService.list(Criteria.where(OrganizationKeys.accountIdentifier).is(accountIdentifier))
            .stream()
            .map(Organization::getIdentifier)
            .collect(Collectors.toList());

    for (String organizationIdentifier : organizations) {
      Optional<AccessControlMigration> orgAccessControlMigrationOptional =
          migrateInternal(accountIdentifier, organizationIdentifier, null, currentGenUserIds);
      List<String> projects = projectService
                                  .list(Criteria.where(ProjectKeys.accountIdentifier)
                                            .is(accountIdentifier)
                                            .and(ProjectKeys.orgIdentifier)
                                            .is(organizationIdentifier))
                                  .stream()
                                  .map(Project::getIdentifier)
                                  .collect(Collectors.toList());

      for (String projectIdentifier : projects) {
        Optional<AccessControlMigration> projectAccessControlMigrationOptional =
            migrateInternal(accountIdentifier, organizationIdentifier, projectIdentifier, currentGenUserIds);
        projectAccessControlMigrationOptional.ifPresent(this::runPostMigrationSteps);
      }
      orgAccessControlMigrationOptional.ifPresent(this::runPostMigrationSteps);
    }
    boolean rbacEnabled =
        NGRestUtils.getResponse(accessControlAdminClient.upsertAccessControlPreference(accountIdentifier, true));
    accountAccessControlMigrationOptional.ifPresent(this::runPostMigrationSteps);

    if (rbacEnabled) {
      log.info("Migration ran successfully and rbac enabled for account: {}", accountIdentifier);
    }
  }

  private void runPostMigrationSteps(AccessControlMigration migration) {
    if (migration.getCreatedRoleAssignments() > 0) {
      save(migration);
    } else {
      log.error(
          "No role assignments were created for account: {}, org: {} and project: {}, migration will run again for this scope",
          migration.getAccountIdentifier(), migration.getOrgIdentifier(), migration.getProjectIdentifier());
    }
  }
}
