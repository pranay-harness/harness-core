package io.harness.aggregator.consumers;

import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.models.SourceMetadata;
import io.harness.accesscontrol.acl.services.ACLService;
import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroup;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupService;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roles.Role;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class RoleAssignmentChangeConsumerImpl implements ChangeConsumer<RoleAssignmentDBO> {
  @Inject private ACLService aclService;
  @Inject private RoleService roleService;
  @Inject private ResourceGroupService resourceGroupService;
  @Inject private ScopeService scopeService;
  private static final String DELIMITER = "/";

  private ACL getACL(RoleAssignmentDBO roleAssignmentDBO, String permission, String resourceSelector) {
    String principalType = Optional.ofNullable(roleAssignmentDBO.getPrincipalType()).map(Enum::name).orElse(null);
    return ACL.builder()
        .roleAssignmentId(roleAssignmentDBO.getId())
        .scopeIdentifier(roleAssignmentDBO.getScopeIdentifier())
        .permissionIdentifier(permission)
        .sourceMetadata(SourceMetadata.builder()
                            .roleIdentifier(roleAssignmentDBO.getRoleIdentifier())
                            .roleAssignmentIdentifier(roleAssignmentDBO.getIdentifier())
                            .resourceGroupIdentifier(roleAssignmentDBO.getResourceGroupIdentifier())
                            .userGroupIdentifier(null)
                            .build())
        .resourceSelector(resourceSelector)
        .principalType(principalType)
        .principalIdentifier(roleAssignmentDBO.getPrincipalIdentifier())
        .aclQueryString(ACL.getAclQueryString(roleAssignmentDBO.getScopeIdentifier(), resourceSelector, principalType,
            roleAssignmentDBO.getPrincipalIdentifier(), permission))
        .enabled(!roleAssignmentDBO.isDisabled())
        .build();
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
      log.info("None of the relevant fields have changed for role assignment: {}", id);
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
    Optional<Role> roleOptional = roleService.get(
        roleAssignmentDBO.getRoleIdentifier(), roleAssignmentDBO.getScopeIdentifier(), ManagedFilter.NO_FILTER);
    Optional<ResourceGroup> resourceGroupOptional = resourceGroupService.get(
        roleAssignmentDBO.getResourceGroupIdentifier(), roleAssignmentDBO.getScopeIdentifier());

    if (!roleOptional.isPresent() || !resourceGroupOptional.isPresent()) {
      log.error("Role/Resource group not found, Unable to process creation of role assignment: {}", roleAssignmentDBO);
      return 0;
    }

    Role role = roleOptional.get();
    ResourceGroup resourceGroup = resourceGroupOptional.get();

    List<ACL> acls = new ArrayList<>();
    role.getPermissions().forEach(permission -> {
      if (resourceGroup.isFullScopeSelected()) {
        Scope scope = scopeService.buildScopeFromScopeIdentifier(resourceGroup.getScopeIdentifier());
        acls.add(getACL(roleAssignmentDBO, permission,
            getResourceSelector(scope.getLevel().getResourceType(), scope.getInstanceId())));
      } else {
        resourceGroup.getResourceSelectors().forEach(
            resourceSelector -> acls.add(getACL(roleAssignmentDBO, permission, resourceSelector)));
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
