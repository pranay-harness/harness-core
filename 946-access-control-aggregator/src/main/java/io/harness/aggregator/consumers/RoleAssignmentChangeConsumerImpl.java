package io.harness.aggregator.consumers;

import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.accesscontrol.principals.PrincipalType.USER_GROUP;
import static io.harness.aggregator.ACLUtils.buildACL;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.services.ACLService;
import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.principals.usergroups.UserGroup;
import io.harness.accesscontrol.principals.usergroups.UserGroupService;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroup;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupService;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roleassignments.persistence.repositories.RoleAssignmentRepository;
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
  private final RoleAssignmentRepository roleAssignmentRepository;

  @Override
  public void consumeUpdateEvent(String id, RoleAssignmentDBO updatedRoleAssignmentDBO) {
    if (!StringUtils.isEmpty(updatedRoleAssignmentDBO.getRoleIdentifier())
        || !StringUtils.isEmpty(updatedRoleAssignmentDBO.getResourceGroupIdentifier())
        || !StringUtils.isEmpty(updatedRoleAssignmentDBO.getPrincipalIdentifier())
        || updatedRoleAssignmentDBO.getDisabled() != null) {
      log.info("Number of ACLs deleted: {}", deleteACLs(id));
      Optional<RoleAssignmentDBO> roleAssignment = roleAssignmentRepository.findById(id);
      if (roleAssignment.isPresent()) {
        long createdCount = createACLs(roleAssignment.get());
        log.info("Number of ACLs created: {}", createdCount);
      }
    }
  }

  @Override
  public void consumeDeleteEvent(String id) {
    log.info("Number of ACLs deleted: {}", deleteACLs(id));
  }

  private long deleteACLs(String id) {
    return aclService.deleteByRoleAssignment(id);
  }

  private long createACLs(RoleAssignmentDBO roleAssignment) {
    Optional<Role> role = roleService.get(
        roleAssignment.getRoleIdentifier(), roleAssignment.getScopeIdentifier(), ManagedFilter.NO_FILTER);
    Optional<ResourceGroup> resourceGroup =
        resourceGroupService.get(roleAssignment.getResourceGroupIdentifier(), roleAssignment.getScopeIdentifier());
    if (!role.isPresent() || !resourceGroup.isPresent()) {
      return 0;
    }

    Set<String> principals = new HashSet<>();
    if (USER_GROUP.equals(roleAssignment.getPrincipalType())) {
      Optional<UserGroup> userGroup =
          userGroupService.get(roleAssignment.getPrincipalIdentifier(), roleAssignment.getScopeIdentifier());
      userGroup.ifPresent(group -> principals.addAll(group.getUsers()));
    } else if (USER.equals(roleAssignment.getPrincipalType())) {
      principals.add(roleAssignment.getPrincipalIdentifier());
    }

    Set<String> resourceSelectors = resourceGroup.get().isFullScopeSelected()
        ? Collections.singleton("/*/*")
        : resourceGroup.get().getResourceSelectors();

    List<ACL> aclsToCreate = new ArrayList<>();
    for (String permission : role.get().getPermissions()) {
      for (String principalIdentifier : principals) {
        for (String resourceSelector : resourceSelectors) {
          aclsToCreate.add(
              buildACL(permission, Principal.of(USER, principalIdentifier), roleAssignment, resourceSelector));
        }
      }
    }

    return aclService.saveAll(aclsToCreate);
  }

  @Override
  public long consumeCreateEvent(String id, RoleAssignmentDBO newRoleAssignmentDBO) {
    long createdCount = createACLs(newRoleAssignmentDBO);
    log.info("Number of ACLs created: {}", createdCount);
    return createdCount;
  }
}
