package io.harness.aggregator.consumers;

import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.services.ACLService;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupService;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.accesscontrol.roles.persistence.RoleDBO;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class RoleChangeConsumerImpl implements ChangeConsumer<RoleDBO> {
  @Inject private ACLService aclService;
  @Inject private RoleService roleService;
  @Inject private ResourceGroupService resourceGroupService;

  @Override
  public long consumeUpdateEvent(String id, RoleDBO roleDBO) {
    if (Optional.ofNullable(roleDBO.getPermissions()).filter(x -> !x.isEmpty()).isPresent()) {
      Set<String> currentPermissionsInRole = roleDBO.getPermissions();
      List<ACL> acls = aclService.getByRole(roleDBO.getScopeIdentifier(), roleDBO.getIdentifier(), roleDBO.isManaged());

      Set<String> oldPermissionsInACLs = acls.stream().map(ACL::getPermissionIdentifier).collect(Collectors.toSet());

      Set<String> permissionsToDelete = Sets.difference(oldPermissionsInACLs, currentPermissionsInRole);
      Set<String> permissionsToAdd = Sets.difference(currentPermissionsInRole, oldPermissionsInACLs);

      // delete ACLs which contain permission contained in permissionsToDelete
      List<ACL> aclsToDelete = acls.stream()
                                   .filter(x -> permissionsToDelete.contains(x.getPermissionIdentifier()))
                                   .collect(Collectors.toList());
      if (!aclsToDelete.isEmpty()) {
        log.info("Deleting {} ACLs", aclsToDelete.size());
        aclService.deleteAll(aclsToDelete);
      }

      Map<ACL.RoleAssignmentResourceSelector, List<ACL>> roleAssignmentToACLMapping =
          acls.stream().collect(Collectors.groupingBy(ACL::roleAssignmentResourceSelector));

      // add new ACLs for new permissions
      List<ACL> aclsToCreate = new ArrayList<>();
      roleAssignmentToACLMapping.forEach((roleAssignmentId, aclList) -> permissionsToAdd.forEach(permissionToAdd -> {
        ACL aclToCreate = ACL.copyOf(aclList.get(0));
        aclToCreate.setPermissionIdentifier(permissionToAdd);
        aclToCreate.setAclQueryString(ACL.getAclQueryString(aclToCreate));
        aclsToCreate.add(aclToCreate);
      }));

      long count = 0;
      if (!aclsToCreate.isEmpty()) {
        count = aclService.insertAllIgnoringDuplicates(aclsToCreate);
      }
      log.info("{} ACLs created", count);
      return count;
    } else {
      log.info("None of the relevant fields have changed for role: {}", id);
    }
    return 0;
  }

  @Override
  public long consumeDeleteEvent(String id) {
    log.info("Role deleted with id: {}", id);
    return 0;
  }

  @Override
  public long consumeCreateEvent(String id, RoleDBO roleDBO) {
    log.info("New role created with id: {}", id);
    return 0;
  }
}
