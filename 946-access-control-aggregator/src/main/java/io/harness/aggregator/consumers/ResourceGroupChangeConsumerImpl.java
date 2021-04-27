package io.harness.aggregator.consumers;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.services.ACLService;
import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDBO;
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
public class ResourceGroupChangeConsumerImpl implements ChangeConsumer<ResourceGroupDBO> {
  private final ACLService aclService;

  @Override
  public long consumeUpdateEvent(String id, ResourceGroupDBO updatedEntity) {
    if (Optional.ofNullable(updatedEntity.getResourceSelectors()).filter(x -> !x.isEmpty()).isPresent()) {
      List<ACL> aclsWithThisResourceGroup = aclService.getByResourceGroup(updatedEntity.getScopeIdentifier(),
          updatedEntity.getIdentifier(), Boolean.TRUE.equals(updatedEntity.getManaged()));

      Set<String> currentResourceSelectors = updatedEntity.getResourceSelectors();
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

      Map<ACL.RoleAssignmentPermissionPrincipal, List<ACL>> roleAssignmentToACLMapping =
          aclsWithThisResourceGroup.stream().collect(Collectors.groupingBy(ACL::roleAssignmentPermissionPrincipal));

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
  public long consumeCreateEvent(String id, ResourceGroupDBO createdEntity) {
    return 0;
  }
}
