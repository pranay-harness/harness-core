package io.harness.ng.core.event;

import static io.harness.NGConstants.DEFAULT_ORG_IDENTIFIER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.Collections.emptyMap;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.accesscontrol.principals.PrincipalDTO;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentCreateRequestDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageResponse;
import io.harness.beans.Scope;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.GeneralException;
import io.harness.ng.accesscontrol.migrations.models.AccessControlMigration;
import io.harness.ng.accesscontrol.migrations.services.AccessControlMigrationService;
import io.harness.ng.core.AccountOrgProjectValidator;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.UserMembershipUpdateSource;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.remote.client.NGRestUtils;
import io.harness.remote.client.RestClientUtils;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;
import io.harness.user.remote.UserClient;
import io.harness.utils.CryptoUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;

@OwnedBy(PL)
@Singleton
@Slf4j
public class DefaultOrganizationManager {
  private final OrganizationService organizationService;
  private final AccountOrgProjectValidator accountOrgProjectValidator;
  private final AccessControlAdminClient accessControlAdminClient;
  private final NgUserService ngUserService;
  private final UserClient userClient;
  private final ResourceGroupClient resourceGroupClient;
  private final AccessControlMigrationService accessControlMigrationService;

  @Inject
  public DefaultOrganizationManager(OrganizationService organizationService,
      AccountOrgProjectValidator accountOrgProjectValidator,
      @Named("PRIVILEGED") AccessControlAdminClient accessControlAdminClient, NgUserService ngUserService,
      UserClient userClient, ResourceGroupClient resourceGroupClient,
      AccessControlMigrationService accessControlMigrationService) {
    this.organizationService = organizationService;
    this.accountOrgProjectValidator = accountOrgProjectValidator;
    this.accessControlAdminClient = accessControlAdminClient;
    this.ngUserService = ngUserService;
    this.userClient = userClient;
    this.resourceGroupClient = resourceGroupClient;
    this.accessControlMigrationService = accessControlMigrationService;
  }

  public void createDefaultOrganization(String accountIdentifier) {
    if (!accountOrgProjectValidator.isPresent(accountIdentifier, null, null)) {
      log.info(String.format(
          "Account with accountIdentifier %s not found, skipping creation of Default Organization", accountIdentifier));
      return;
    }

    Organization defaultOrg = ensureDefaultOrg(accountIdentifier);
    setupAccount(defaultOrg.getAccountIdentifier(), defaultOrg.getIdentifier());
  }

  private Organization ensureDefaultOrg(String accountIdentifier) {
    Optional<Organization> organization = organizationService.get(accountIdentifier, DEFAULT_ORG_IDENTIFIER);
    if (organization.isPresent()) {
      log.info(String.format("Default Organization for account %s already present", accountIdentifier));
      return organization.get();
    }
    OrganizationDTO createOrganizationDTO = OrganizationDTO.builder().build();
    createOrganizationDTO.setIdentifier(DEFAULT_ORG_IDENTIFIER);
    createOrganizationDTO.setName("Default");
    createOrganizationDTO.setTags(emptyMap());
    createOrganizationDTO.setDescription("Default Organization");
    createOrganizationDTO.setHarnessManaged(true);
    return organizationService.create(accountIdentifier, createOrganizationDTO);
  }

  private void setupAccount(String accountIdentifier, String orgIdentifier) {
    NGRestUtils.getResponse(resourceGroupClient.createManagedResourceGroup(accountIdentifier, null, null));
    NGRestUtils.getResponse(resourceGroupClient.createManagedResourceGroup(accountIdentifier, orgIdentifier, null));

    Collection<UserInfo> cgUsers = getCGUsers(accountIdentifier);
    Collection<String> cgAdmins =
        cgUsers.stream().filter(UserInfo::isAdmin).map(UserInfo::getUuid).collect(Collectors.toSet());

    Scope accountScope = Scope.of(accountIdentifier, null, null);
    if (!hasAdmin(accountScope)) {
      cgUsers.forEach(user -> upsertUserMembership(accountScope, user.getUuid()));
      assignAdminRoleToUsers(accountScope, cgAdmins, getManagedAdminRole(accountScope));
      if (!hasAdmin(accountScope)) {
        throw new GeneralException(String.format("No Admin could be assigned in scope %s", accountScope));
      }
      accessControlMigrationService.save(AccessControlMigration.builder().accountIdentifier(accountIdentifier).build());
    }

    Scope orgScope = Scope.of(accountIdentifier, orgIdentifier, null);
    if (!hasAdmin(orgScope)) {
      cgAdmins.forEach(user -> upsertUserMembership(orgScope, user));
      assignAdminRoleToUsers(orgScope, cgAdmins, getManagedAdminRole(orgScope));
      if (!hasAdmin(orgScope)) {
        throw new GeneralException(String.format("No Admin could be assigned in scope %s", orgScope));
      }
      accessControlMigrationService.save(
          AccessControlMigration.builder().accountIdentifier(accountIdentifier).orgIdentifier(orgIdentifier).build());
    }
  }

  private boolean hasAdmin(Scope scope) {
    return !isEmpty(ngUserService.listUsersHavingRole(scope, getManagedAdminRole(scope)));
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

  private void assignAdminRoleToUsers(Scope scope, Collection<String> users, String roleIdentifier) {
    createRoleAssignments(scope, buildRoleAssignments(users, roleIdentifier));
  }

  private List<RoleAssignmentDTO> buildRoleAssignments(Collection<String> userIds, String roleIdentifier) {
    return userIds.stream()
        .map(userId
            -> RoleAssignmentDTO.builder()
                   .disabled(false)
                   .identifier("role_assignment_".concat(CryptoUtils.secureRandAlphaNumString(20)))
                   .roleIdentifier(roleIdentifier)
                   .resourceGroupIdentifier("_all_resources")
                   .principal(PrincipalDTO.builder().identifier(userId).type(PrincipalType.USER).build())
                   .build())
        .collect(Collectors.toList());
  }

  private void createRoleAssignments(Scope scope, List<RoleAssignmentDTO> roleAssignments) {
    List<List<RoleAssignmentDTO>> batchedRoleAssignments = Lists.partition(roleAssignments, 2);
    for (List<RoleAssignmentDTO> batchOfRoleAssignment : batchedRoleAssignments) {
      NGRestUtils.getResponse(accessControlAdminClient.createMultiRoleAssignment(scope.getAccountIdentifier(),
          scope.getOrgIdentifier(), scope.getProjectIdentifier(), false,
          RoleAssignmentCreateRequestDTO.builder().roleAssignments(batchOfRoleAssignment).build()));
    }
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

  private Collection<UserInfo> getCGUsers(String accountId) {
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
    return users;
  }
}
