package io.harness.aggregator.consumers;

import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.services.ACLService;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupService;
import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDBO;
import io.harness.accesscontrol.roles.RoleService;

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
public class ResourceGroupChangeConsumerImpl implements ChangeConsumer<ResourceGroupDBO> {
  @Inject private ACLService aclService;
  @Inject private RoleService roleService;
  @Inject private ResourceGroupService resourceGroupService;

  @Override
  public long consumeUpdateEvent(String id, ResourceGroupDBO resourceGroupDBO) {
    if (Optional.ofNullable(resourceGroupDBO.getResourceSelectors()).filter(x -> !x.isEmpty()).isPresent()) {
      List<ACL> aclsWithThisResourceGroup = aclService.getByResourceGroup(resourceGroupDBO.getScopeIdentifier(),
          resourceGroupDBO.getIdentifier(), Boolean.TRUE.equals(resourceGroupDBO.getManaged()));

      Set<String> currentResourceSelectors = resourceGroupDBO.getResourceSelectors();
      Set<String> resourceSelectorsInAcls =
          aclsWithThisResourceGroup.stream().map(ACL::getResourceSelector).collect(Collectors.toSet());

      Set<String> resourcesSelectorsToDelete = Sets.difference(resourceSelectorsInAcls, currentResourceSelectors);
      Set<String> resourceSelectorsToAdd = Sets.difference(currentResourceSelectors, resourceSelectorsInAcls);

      // delete ACLs which contain old resource Selectors
      List<ACL> aclsToDelete = aclsWithThisResourceGroup.stream()
                                   .filter(x -> resourcesSelectorsToDelete.contains(x.getResourceSelector()))
                                   .collect(Collectors.toList());
      if (!aclsToDelete.isEmpty()) {
        log.info("Deleting: {} ACls", aclsToDelete.size());
        aclService.deleteAll(aclsToDelete);
      }

      Map<ACL.RoleAssignmentPermission, List<ACL>> roleAssignmentToACLMapping =
          aclsWithThisResourceGroup.stream().collect(Collectors.groupingBy(ACL::roleAssignmentPermission));

      // insert new ACLs for all new resource selectors
      List<ACL> aclsToCreate = new ArrayList<>();
      roleAssignmentToACLMapping.forEach(
          (roleAssignmentId, aclList) -> resourceSelectorsToAdd.forEach(resourceSelectorToAdd -> {
            ACL aclToCreate = ACL.copyOf(aclList.get(0));
            aclToCreate.setResourceSelector(resourceSelectorToAdd);
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
      log.info("None of the relevant fields have changed for resource group: {}", id);
    }
    return 0;
  }

  @Override
  public long consumeDeleteEvent(String id) {
    log.info("Received resource group deletion event for id: {}", id);
    return 0;
  }

  @Override
  public long consumeCreateEvent(String id, ResourceGroupDBO accessControlEntity) {
    log.info("Received resource group creation event for id: {}", id);
    return 0;
  }
}
