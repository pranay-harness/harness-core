package io.harness.aggregator.consumers;

import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.accesscontrol.principals.PrincipalType.USER_GROUP;
import static io.harness.aggregator.ACLUtils.buildACL;
import static io.harness.annotations.dev.HarnessTeam.PL;

import static java.lang.Runtime.getRuntime;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.repository.ACLRepository;
import io.harness.accesscontrol.principals.usergroups.persistence.UserGroupDBO;
import io.harness.accesscontrol.principals.usergroups.persistence.UserGroupRepository;
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
public class UserGroupChangeConsumerImpl implements ChangeConsumer<UserGroupDBO> {
  private final ACLRepository aclRepository;
  private final RoleAssignmentRepository roleAssignmentRepository;
  private final UserGroupRepository userGroupRepository;

  @Override
  public void consumeUpdateEvent(String id, UserGroupDBO updatedUserGroup) {
    Optional<UserGroupDBO> userGroup = userGroupRepository.findById(id);
    if (!userGroup.isPresent()) {
      return;
    }

    Criteria criteria = Criteria.where(RoleAssignmentDBOKeys.principalType)
                            .is(USER_GROUP)
                            .and(RoleAssignmentDBOKeys.principalIdentifier)
                            .is(userGroup.get().getIdentifier())
                            .and(RoleAssignmentDBOKeys.scopeIdentifier)
                            .is(userGroup.get().getScopeIdentifier());
    List<ReProcessRoleAssignmentOnUserGroupUpdateTask> tasksToExecute =
        roleAssignmentRepository.findAll(criteria, Pageable.unpaged())
            .stream()
            .map(
                (RoleAssignmentDBO roleAssignment)
                    -> new ReProcessRoleAssignmentOnUserGroupUpdateTask(aclRepository, roleAssignment, userGroup.get()))
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
  public long consumeCreateEvent(String id, UserGroupDBO createdEntity) {
    return 0;
  }

  private static class ReProcessRoleAssignmentOnUserGroupUpdateTask implements Callable<Result> {
    private final ACLRepository aclRepository;
    private final RoleAssignmentDBO roleAssignmentDBO;
    private final UserGroupDBO updatedUserGroup;

    private ReProcessRoleAssignmentOnUserGroupUpdateTask(
        ACLRepository aclRepository, RoleAssignmentDBO roleAssignment, UserGroupDBO updatedUserGroup) {
      this.aclRepository = aclRepository;
      this.roleAssignmentDBO = roleAssignment;
      this.updatedUserGroup = updatedUserGroup;
    }

    @Override
    public Result call() {
      Set<String> existingPrincipals = Sets.newHashSet(
          Sets.newHashSet(aclRepository.getDistinctPrincipalsInACLsForRoleAssignment(roleAssignmentDBO.getId())));
      Set<String> principalsAddedToUserGroup = Sets.difference(updatedUserGroup.getUsers(), existingPrincipals);
      Set<String> principalRemovedFromUserGroup = Sets.difference(existingPrincipals, updatedUserGroup.getUsers());

      long numberOfACLsDeleted =
          aclRepository.deleteByRoleAssignmentIdAndPrincipals(roleAssignmentDBO.getId(), principalRemovedFromUserGroup);

      Set<String> existingResourceSelectors =
          Sets.newHashSet(aclRepository.getDistinctResourceSelectorsInACLs(roleAssignmentDBO.getId()));
      Set<String> existingPermissions =
          Sets.newHashSet(aclRepository.getDistinctPermissionsInACLsForRoleAssignment(roleAssignmentDBO.getId()));

      long numberOfACLsCreated = 0;
      List<ACL> aclsToCreate = new ArrayList<>();
      for (String permissionIdentifier : existingPermissions) {
        for (String principalIdentifier : principalsAddedToUserGroup) {
          for (String resourceSelector : existingResourceSelectors) {
            aclsToCreate.add(buildACL(
                permissionIdentifier, Principal.of(USER, principalIdentifier), roleAssignmentDBO, resourceSelector));
          }
        }
      }
      numberOfACLsCreated += aclRepository.insertAllIgnoringDuplicates(aclsToCreate);

      return new Result(numberOfACLsCreated, numberOfACLsDeleted);
    }
  }
}
