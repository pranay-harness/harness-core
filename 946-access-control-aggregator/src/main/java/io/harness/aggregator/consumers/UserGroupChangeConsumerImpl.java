package io.harness.aggregator.consumers;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.services.ACLService;
import io.harness.accesscontrol.principals.usergroups.persistence.UserGroupDBO;
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
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class UserGroupChangeConsumerImpl implements ChangeConsumer<UserGroupDBO> {
  private final ACLService aclService;

  @Override
  public long consumeUpdateEvent(String id, UserGroupDBO updatedEntity) {
    if (Optional.ofNullable(updatedEntity.getUsers()).isPresent()) {
      Set<String> currentUsersInUserGroup = updatedEntity.getUsers();
      List<ACL> acls = aclService.getByUserGroup(updatedEntity.getScopeIdentifier(), updatedEntity.getIdentifier());

      Set<String> oldUsersInUserGroup = acls.stream().map(ACL::getPrincipalIdentifier).collect(Collectors.toSet());

      Set<String> usersToDelete = Sets.difference(oldUsersInUserGroup, currentUsersInUserGroup);
      Set<String> usersToAdd = Sets.difference(currentUsersInUserGroup, oldUsersInUserGroup);

      List<ACL> aclsToDelete =
          acls.stream().filter(x -> usersToDelete.contains(x.getPrincipalIdentifier())).collect(Collectors.toList());
      if (!aclsToDelete.isEmpty()) {
        log.info("Deleting {} ACLs", aclsToDelete.size());
        aclService.deleteAll(aclsToDelete);
      }

      Map<ACL.RoleAssignmentResourceSelectorPermission, List<ACL>> roleAssignmentToACLMapping =
          acls.stream().collect(Collectors.groupingBy(ACL::roleAssignmentResourceSelectorPermission));

      // add new ACLs for new permissions
      List<ACL> aclsToCreate = new ArrayList<>();
      roleAssignmentToACLMapping.forEach((roleAssignmentId, aclList) -> usersToAdd.forEach(userToAdd -> {
        ACL aclToCreate = ACL.copyOf(aclList.get(0));
        aclToCreate.setPrincipalIdentifier(userToAdd);
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
  public long consumeCreateEvent(String id, UserGroupDBO createdEntity) {
    return 0;
  }
}
