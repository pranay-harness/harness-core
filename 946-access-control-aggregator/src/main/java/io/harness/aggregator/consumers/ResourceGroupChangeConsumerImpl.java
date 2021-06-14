package io.harness.aggregator.consumers;

import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.accesscontrol.principals.PrincipalType.USER_GROUP;
import static io.harness.aggregator.ACLUtils.buildACL;
import static io.harness.annotations.dev.HarnessTeam.PL;

import static java.lang.Runtime.getRuntime;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.repository.ACLRepository;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDBO;
import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupRepository;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO.RoleAssignmentDBOKeys;
import io.harness.accesscontrol.roleassignments.persistence.repositories.RoleAssignmentRepository;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.GeneralException;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class ResourceGroupChangeConsumerImpl implements ChangeConsumer<ResourceGroupDBO> {
  private final ACLRepository aclRepository;
  private final RoleAssignmentRepository roleAssignmentRepository;
  private final ResourceGroupRepository resourceGroupRepository;

  @Override
  public void consumeUpdateEvent(String id, ResourceGroupDBO updatedResourceGroup) {
    Optional<ResourceGroupDBO> resourceGroup = resourceGroupRepository.findById(id);
    if (!resourceGroup.isPresent()) {
      return;
    }

    // skip processing if the resource group is has full scope selected (_all_resources)
    if (resourceGroup.get().isFullScopeSelected()) {
      return;
    }

    Criteria criteria = Criteria.where(RoleAssignmentDBOKeys.resourceGroupIdentifier)
                            .is(resourceGroup.get().getIdentifier())
                            .and(RoleAssignmentDBOKeys.scopeIdentifier)
                            .is(resourceGroup.get().getScopeIdentifier());
    List<ReProcessRoleAssignmentOnRoleUpdateTask> tasksToExecute =
        roleAssignmentRepository.findAll(criteria, Pageable.unpaged())
            .stream()
            .map((RoleAssignmentDBO roleAssignment)
                     -> new ReProcessRoleAssignmentOnRoleUpdateTask(aclRepository, roleAssignment, resourceGroup.get()))
            .collect(Collectors.toList());

    long numberOfACLsCreated = 0;
    long numberOfACLsDeleted = 0;

    ExecutorService executorService = Executors.newFixedThreadPool(getRuntime().availableProcessors() * 2);
    try {
      for (Future<Result> future : executorService.invokeAll(tasksToExecute)) {
        Result result = future.get();
        numberOfACLsCreated += result.getNumberOfACLsCreated();
        numberOfACLsDeleted += result.getNumberOfACLsDeleted();
      }
    } catch (ExecutionException ex) {
      throw new GeneralException("", ex.getCause());
    } catch (InterruptedException ex) {
      // Should never happen though
      Thread.currentThread().interrupt();
    }

    log.info("Number of ACLs created: {}", numberOfACLsCreated);
    log.info("Number of ACLs deleted: {}", numberOfACLsDeleted);
  }

  @Override
  public void consumeDeleteEvent(String id) {
    // No need to process separately. Would be processed indirectly when associated role bindings will be deleted
  }

  @Override
  public long consumeCreateEvent(String id, ResourceGroupDBO createdEntity) {
    return 0;
  }

  private static class ReProcessRoleAssignmentOnRoleUpdateTask implements Callable<Result> {
    private final ACLRepository aclRepository;
    private final RoleAssignmentDBO roleAssignmentDBO;
    private final ResourceGroupDBO updatedResourceGroup;

    private ReProcessRoleAssignmentOnRoleUpdateTask(
        ACLRepository aclRepository, RoleAssignmentDBO roleAssignment, ResourceGroupDBO updatedResourceGroup) {
      this.aclRepository = aclRepository;
      this.roleAssignmentDBO = roleAssignment;
      this.updatedResourceGroup = updatedResourceGroup;
    }

    @Override
    public Result call() {
      Set<String> existingResourceSelectors =
          Sets.newHashSet(aclRepository.getDistinctResourceSelectorsInACLs(roleAssignmentDBO.getId()));
      Set<String> resourceSelectorsRemovedFromResourceGroup =
          Sets.difference(existingResourceSelectors, updatedResourceGroup.getResourceSelectors());
      Set<String> resourceSelectorsAddedToResourceGroup =
          Sets.difference(updatedResourceGroup.getResourceSelectors(), existingResourceSelectors);

      long numberOfACLsDeleted = aclRepository.deleteByRoleAssignmentIdAndResourceSelectors(
          roleAssignmentDBO.getId(), resourceSelectorsRemovedFromResourceGroup);

      Set<String> existingPermissions =
          Sets.newHashSet(aclRepository.getDistinctPermissionsInACLsForRoleAssignment(roleAssignmentDBO.getId()));
      Set<String> existingPrincipals =
          Sets.newHashSet(aclRepository.getDistinctPrincipalsInACLsForRoleAssignment(roleAssignmentDBO.getId()));
      PrincipalType principalType =
          USER_GROUP.equals(roleAssignmentDBO.getPrincipalType()) ? USER : roleAssignmentDBO.getPrincipalType();

      long numberOfACLsCreated = 0;
      List<ACL> aclsToCreate = new ArrayList<>();
      for (String resourceSelector : resourceSelectorsAddedToResourceGroup) {
        for (String principalIdentifier : existingPrincipals) {
          for (String permissionIdentifier : existingPermissions) {
            aclsToCreate.add(buildACL(permissionIdentifier, Principal.of(principalType, principalIdentifier),
                roleAssignmentDBO, resourceSelector));
          }
        }
      }
      numberOfACLsCreated += aclRepository.insertAllIgnoringDuplicates(aclsToCreate);

      return new Result(numberOfACLsCreated, numberOfACLsDeleted);
    }
  }
}
