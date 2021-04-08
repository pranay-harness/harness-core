package io.harness.aggregator.consumers;

import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.accesscontrol.principals.PrincipalType.USER_GROUP;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.models.SourceMetadata;
import io.harness.accesscontrol.acl.services.ACLService;
import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.principals.usergroups.UserGroup;
import io.harness.accesscontrol.principals.usergroups.UserGroupService;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroup;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupService;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roles.Role;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class RoleAssignmentChangeConsumerImpl implements ChangeConsumer<RoleAssignmentDBO> {
  @Inject private ACLService aclService;
  @Inject private RoleService roleService;
  @Inject private UserGroupService userGroupService;
  @Inject private ResourceGroupService resourceGroupService;
  @Inject private ScopeService scopeService;
  private static final String DELIMITER = "/";

  private List<ACL> getACLs(
      RoleAssignmentDBO roleAssignmentDBO, String permission, String resourceSelector, String scopeIdentifier) {
    String userGroupIdentifier = null;
    Set<String> principals = new HashSet<>();
    if (USER_GROUP.equals(roleAssignmentDBO.getPrincipalType())) {
      userGroupIdentifier = roleAssignmentDBO.getPrincipalIdentifier();
      Optional<UserGroup> userGroup =
          userGroupService.get(roleAssignmentDBO.getPrincipalIdentifier(), roleAssignmentDBO.getScopeIdentifier());
      userGroup.ifPresent(group -> principals.addAll(group.getUsers()));
    } else if (USER.equals(roleAssignmentDBO.getPrincipalType())) {
      principals.add(roleAssignmentDBO.getPrincipalIdentifier());
    }
    String principalType = USER.name();
    String finalUserGroupIdentifier = userGroupIdentifier;
    return principals.stream()
        .map(principal
            -> ACL.builder()
                   .roleAssignmentId(roleAssignmentDBO.getId())
                   .scopeIdentifier(roleAssignmentDBO.getScopeIdentifier())
                   .permissionIdentifier(permission)
                   .sourceMetadata(SourceMetadata.builder()
                                       .roleIdentifier(roleAssignmentDBO.getRoleIdentifier())
                                       .roleAssignmentIdentifier(roleAssignmentDBO.getIdentifier())
                                       .resourceGroupIdentifier(roleAssignmentDBO.getResourceGroupIdentifier())
                                       .userGroupIdentifier(finalUserGroupIdentifier)
                                       .build())
                   .resourceSelector(resourceSelector)
                   .principalType(principalType)
                   .principalIdentifier(principal)
                   .aclQueryString(
                       ACL.getAclQueryString(scopeIdentifier, resourceSelector, principalType, principal, permission))
                   .enabled(!roleAssignmentDBO.isDisabled())
                   .build())
        .collect(Collectors.toList());
  }

  @Override
  public long consumeUpdateEvent(String id, RoleAssignmentDBO roleAssignmentDBO) {
    if (roleAssignmentDBO.getDisabled() != null) {
      List<ACL> acls = aclService.getByRoleAssignmentId(id);
      acls.forEach(acl -> acl.setEnabled(!roleAssignmentDBO.getDisabled()));
      long count = aclService.saveAll(acls);
      log.info("Updated {} ACLs", count);
      return count;
    } else {
      log.debug("None of the relevant fields have changed for role assignment: {}", id);
    }
    return 0;
  }

  @Override
  public long consumeDeleteEvent(String id) {
    long count = aclService.deleteByRoleAssignmentId(id);
    log.info("{} ACLs deleted", count);
    return count;
  }

  private String getResourceSelector(String resourceType, String resourceIdentifier) {
    return DELIMITER.concat(resourceType).concat(DELIMITER).concat(resourceIdentifier);
  }

  @Override
  public long consumeCreateEvent(String id, RoleAssignmentDBO roleAssignmentDBO) {
    assert aclService != null && roleService != null && resourceGroupService != null && scopeService != null
        && userGroupService != null;
    Optional<Role> roleOptional = roleService.get(
        roleAssignmentDBO.getRoleIdentifier(), roleAssignmentDBO.getScopeIdentifier(), ManagedFilter.NO_FILTER);
    Optional<ResourceGroup> resourceGroupOptional = resourceGroupService.get(
        roleAssignmentDBO.getResourceGroupIdentifier(), roleAssignmentDBO.getScopeIdentifier());

    if (!roleOptional.isPresent() || !resourceGroupOptional.isPresent()) {
      log.error("Role/Resource group not found, Unable to process creation of role assignment: {}", roleAssignmentDBO);
      return 0;
    }

    if (!roleAssignmentDBO.getPrincipalType().equals(USER_GROUP)
        && !roleAssignmentDBO.getPrincipalType().equals(USER)) {
      log.error("We only have support for USER_GROUP and USER as role assignment principal: {}", roleAssignmentDBO);
      return 0;
    }

    Role role = roleOptional.get();
    ResourceGroup resourceGroup = resourceGroupOptional.get();

    List<ACL> acls = new ArrayList<>();
    role.getPermissions().forEach(permission -> {
      if (resourceGroup.isFullScopeSelected()) {
        Scope scope = scopeService.buildScopeFromScopeIdentifier(resourceGroup.getScopeIdentifier());
        acls.addAll(getACLs(roleAssignmentDBO, permission,
            getResourceSelector(scope.getLevel().getResourceType(), scope.getInstanceId()),
            Optional.ofNullable(scope.getParentScope()).map(Scope::toString).orElse("")));
      } else {
        resourceGroup.getResourceSelectors().forEach(resourceSelector
            -> acls.addAll(
                getACLs(roleAssignmentDBO, permission, resourceSelector, roleAssignmentDBO.getScopeIdentifier())));
      }
    });

    long count = 0;
    if (!acls.isEmpty()) {
      count = aclService.insertAllIgnoringDuplicates(acls);
    }
    log.info("{} ACLs created", count);
    return count;
  }
}
