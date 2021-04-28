package io.harness.aggregator.consumers;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.services.ACLService;
import io.harness.accesscontrol.roles.persistence.RoleDBO;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
@Singleton
public class RoleChangeConsumerImpl implements ChangeConsumer<RoleDBO> {
  private final ACLService aclService;

  @Inject
  public RoleChangeConsumerImpl(ACLService aclService) {
    this.aclService = aclService;
  }

  @Override
  public long consumeUpdateEvent(String id, RoleDBO updatedEntity) {
    if (Optional.ofNullable(updatedEntity.getPermissions()).filter(x -> !x.isEmpty()).isPresent()) {
      Set<String> currentPermissionsInRole = updatedEntity.getPermissions();
      List<ACL> acls = aclService.getByRole(
          updatedEntity.getScopeIdentifier(), updatedEntity.getIdentifier(), updatedEntity.isManaged());

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

      Map<ACL.RoleAssignmentResourceSelectorPrincipal, List<ACL>> roleAssignmentToACLMapping =
          acls.stream().collect(Collectors.groupingBy(ACL::roleAssignmentResourceSelectorPrincipal));

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
        count = aclService.saveAll(aclsToCreate);
      }
      log.info("{} ACLs created", count);
      return count;
    }
    return 0;
  }

  @Override
  public long consumeDeleteEvent(String id) {
    return 0;
  }

  @Override
  public long consumeCreateEvent(String id, RoleDBO createdEntity) {
    return 0;
  }
}
