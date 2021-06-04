package io.harness.aggregator.consumers;

import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.accesscontrol.principals.PrincipalType.USER_GROUP;
import static io.harness.aggregator.ACLUtils.getACL;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.repository.ACLRepository;
import io.harness.accesscontrol.principals.usergroups.persistence.UserGroupDBO;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO.RoleAssignmentDBOKeys;
import io.harness.accesscontrol.roleassignments.persistence.repositories.RoleAssignmentRepository;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class UserGroupChangeConsumerImpl implements ChangeConsumer<UserGroupDBO> {
  private final ACLRepository aclRepository;
  private final RoleAssignmentRepository roleAssignmentRepository;
  private final ChangeConsumer<RoleAssignmentDBO> roleAssignmentChangeConsumer;
  // Number of threads = Number of Available Cores * (1 + (Wait time / Service time) )
  private final ExecutorService executorService =
      Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

  private long processRoleAssignmentInternal(RoleAssignmentDBO roleAssignmentDBO, UserGroupDBO updatedUserGroup) {
    long createdCount = 0;
    Set<String> existingPrincipals =
        Sets.newHashSet(aclRepository.getDistinctPrincipalsInACLsForRoleAssignment(roleAssignmentDBO.getId()));

    if (existingPrincipals.isEmpty()) {
      createdCount += roleAssignmentChangeConsumer.consumeCreateEvent(roleAssignmentDBO.getId(), roleAssignmentDBO);
    } else {
      Set<String> principalsToDelete = Sets.difference(existingPrincipals, updatedUserGroup.getUsers());
      Set<String> principalsToAdd = Sets.difference(updatedUserGroup.getUsers(), existingPrincipals);
      Set<String> existingResourceSelectors =
          Sets.newHashSet(aclRepository.getDistinctResourceSelectorsInACLs(roleAssignmentDBO.getId()));
      Set<String> existingPermissions =
          Sets.newHashSet(aclRepository.getDistinctPermissionsInACLsForRoleAssignment(roleAssignmentDBO.getId()));

      long deletedCount =
          aclRepository.deleteByRoleAssignmentIdAndPrincipals(roleAssignmentDBO.getId(), principalsToDelete);
      log.info("ACLs deleted: {}", deletedCount);

      List<ACL> aclsToCreate = new ArrayList<>();
      existingResourceSelectors.forEach(resourceSelector
          -> existingPermissions.forEach(permissionIdentifier
              -> principalsToAdd.forEach(principalIdentifier
                  -> aclsToCreate.add(getACL(permissionIdentifier, Principal.of(USER, principalIdentifier),
                      roleAssignmentDBO, resourceSelector)))));
      if (!aclsToCreate.isEmpty()) {
        createdCount += aclRepository.insertAllIgnoringDuplicates(aclsToCreate);
        log.info("ACLs created: {}", createdCount);
      }
    }
    return createdCount;
  }

  private long processRoleAssignments(List<RoleAssignmentDBO> roleAssignments, UserGroupDBO updatedUserGroup) {
    List<CompletableFuture<Long>> futures = new ArrayList<>();
    for (RoleAssignmentDBO roleAssignmentDBO : roleAssignments) {
      futures.add(CompletableFuture.supplyAsync(
          () -> processRoleAssignmentInternal(roleAssignmentDBO, updatedUserGroup), executorService));
    }

    return futures.stream().mapToLong(CompletableFuture::join).sum();
  }

  @Override
  public long consumeUpdateEvent(String id, UserGroupDBO updatedUserGroup) {
    long createdCount = 0;
    int pageIdx = 0;
    int pageSize = 1000;
    int iterations = 100;
    Page<RoleAssignmentDBO> roleAssignmentsPage;

    do {
      roleAssignmentsPage = roleAssignmentRepository.findAll(Criteria.where(RoleAssignmentDBOKeys.principalType)
                                                                 .is(USER_GROUP)
                                                                 .and(RoleAssignmentDBOKeys.principalIdentifier)
                                                                 .is(updatedUserGroup.getIdentifier())
                                                                 .and(RoleAssignmentDBOKeys.scopeIdentifier)
                                                                 .is(updatedUserGroup.getScopeIdentifier()),
          getPageRequest(PageRequest.builder().pageIndex(pageIdx).pageSize(pageSize).build()));

      createdCount += processRoleAssignments(roleAssignmentsPage.getContent(), updatedUserGroup);
      pageIdx++;
      iterations--;
    } while (iterations > 0 && !roleAssignmentsPage.getContent().isEmpty());

    return createdCount;
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
