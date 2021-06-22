package io.harness.aggregator.consumers;

import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.accesscontrol.principals.PrincipalType.USER_GROUP;
import static io.harness.aggregator.ACLUtils.buildACL;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.repository.ACLRepository;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(PL)
@Singleton
@Slf4j
public class RoleAssignmentChangeConsumerImpl implements ChangeConsumer<RoleAssignmentDBO> {
  private final ACLRepository aclRepository;
  private final RoleAssignmentRepository roleAssignmentRepository;
  private final RoleService roleService;
  private final UserGroupService userGroupService;
  private final ResourceGroupService resourceGroupService;

  @Inject
  public RoleAssignmentChangeConsumerImpl(ACLRepository aclRepository, RoleService roleService,
      UserGroupService userGroupService, ResourceGroupService resourceGroupService,
      RoleAssignmentRepository roleAssignmentRepository) {
    this.aclRepository = aclRepository;
    this.roleService = roleService;
    this.roleAssignmentRepository = roleAssignmentRepository;
    this.resourceGroupService = resourceGroupService;
    this.userGroupService = userGroupService;
  }

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
    return aclRepository.deleteByRoleAssignmentId(id);
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
    return aclRepository.insertAllIgnoringDuplicates(aclsToCreate);
  }

  @Override
  public long consumeCreateEvent(String id, RoleAssignmentDBO newRoleAssignmentDBO) {
    Optional<RoleAssignmentDBO> roleAssignmentOptional = roleAssignmentRepository.findByIdentifierAndScopeIdentifier(
        newRoleAssignmentDBO.getIdentifier(), newRoleAssignmentDBO.getScopeIdentifier());
    if (!roleAssignmentOptional.isPresent()) {
      log.info("Role assignment has been deleted, not processing role assignment create event for id: {}", id);
      return 0;
    }
    long createdCount = createACLs(newRoleAssignmentDBO);
    log.info("Number of ACLs created: {}", createdCount);
    return createdCount;
  }
}
