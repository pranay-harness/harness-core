package io.harness.aggregator.consumers;

import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.accesscontrol.principals.PrincipalType.USER_GROUP;
import static io.harness.aggregator.ACLUtils.getACL;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.services.ACLService;
import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.principals.usergroups.UserGroup;
import io.harness.accesscontrol.principals.usergroups.UserGroupService;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroup;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupService;
import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roles.Role;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(PL)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class RoleAssignmentChangeConsumerImpl implements ChangeConsumer<RoleAssignmentDBO> {
  private final ACLService aclService;
  private final RoleService roleService;
  private final UserGroupService userGroupService;
  private final ResourceGroupService resourceGroupService;
  private final RoleAssignmentService roleAssignmentService;

  @Override
  public long consumeUpdateEvent(String id, RoleAssignmentDBO updatedEntity) {
    if (!StringUtils.isEmpty(updatedEntity.getRoleIdentifier())
        || !StringUtils.isEmpty(updatedEntity.getResourceGroupIdentifier())
        || !StringUtils.isEmpty(updatedEntity.getPrincipalIdentifier()) || updatedEntity.getDisabled() != null) {
      consumeDeleteEvent(id);
      return consumeCreateEvent(id, updatedEntity);
    }
    return 0;
  }

  @Override
  public long consumeDeleteEvent(String id) {
    long count = aclService.deleteByRoleAssignment(id);
    log.info("ACLs deleted: {}", count);
    return count;
  }

  private long createACLs(RoleAssignmentDBO roleAssignment, Role role, ResourceGroup resourceGroup) {
    Set<String> principals = new HashSet<>();
    if (USER_GROUP.equals(roleAssignment.getPrincipalType())) {
      Optional<UserGroup> userGroup =
          userGroupService.get(roleAssignment.getPrincipalIdentifier(), roleAssignment.getScopeIdentifier());
      userGroup.ifPresent(group -> principals.addAll(group.getUsers()));
    } else if (USER.equals(roleAssignment.getPrincipalType())) {
      principals.add(roleAssignment.getPrincipalIdentifier());
    }

    List<String> resourceSelectors = resourceGroup.isFullScopeSelected()
        ? Collections.singletonList("/*/*")
        : new ArrayList<>(resourceGroup.getResourceSelectors());

    List<ACL> aclsToCreate = new ArrayList<>();
    long createdCount = 0;

    role.getPermissions().forEach(permission
        -> principals.forEach(principalIdentifier
            -> resourceSelectors.forEach(resourceSelector
                -> aclsToCreate.add(
                    getACL(permission, Principal.of(USER, principalIdentifier), roleAssignment, resourceSelector)))));

    if (!aclsToCreate.isEmpty()) {
      createdCount += aclService.saveAll(aclsToCreate);
    }
    return createdCount;
  }

  @Override
  public long consumeCreateEvent(String id, RoleAssignmentDBO roleAssignmentDBO) {
    Optional<RoleAssignment> roleAssignmentOptional =
        roleAssignmentService.get(roleAssignmentDBO.getIdentifier(), roleAssignmentDBO.getScopeIdentifier());
    if (!roleAssignmentOptional.isPresent()) {
      log.info("Role assignment has been deleted, not processing role assignment create event for id: {}", id);
      return 0;
    }
    Role role =
        roleService
            .get(roleAssignmentDBO.getRoleIdentifier(), roleAssignmentDBO.getScopeIdentifier(), ManagedFilter.NO_FILTER)
            .orElseThrow(
                ()
                    -> new IllegalArgumentException("No such role found: " + roleAssignmentDBO.getRoleIdentifier()
                        + " in scope " + roleAssignmentDBO.getScopeIdentifier()));
    ResourceGroup resourceGroup =
        resourceGroupService.get(roleAssignmentDBO.getResourceGroupIdentifier(), roleAssignmentDBO.getScopeIdentifier())
            .orElseThrow(()
                             -> new IllegalArgumentException(
                                 "No such resource group found: " + roleAssignmentDBO.getResourceGroupIdentifier()
                                 + " in scope " + roleAssignmentDBO.getScopeIdentifier()));

    long createdCount = createACLs(roleAssignmentDBO, role, resourceGroup);
    log.info("ACLs created: {}", createdCount);
    return createdCount;
  }
}
