package io.harness.aggregator.consumers;

import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.accesscontrol.principals.PrincipalType.USER_GROUP;
import static io.harness.aggregator.ACLUtils.getACL;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.repository.ACLRepository;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO.RoleAssignmentDBOKeys;
import io.harness.accesscontrol.roleassignments.persistence.repositories.RoleAssignmentRepository;
import io.harness.accesscontrol.roles.persistence.RoleDBO;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class RoleChangeConsumerImpl implements ChangeConsumer<RoleDBO> {
  private final ACLRepository aclRepository;
  private final RoleAssignmentRepository roleAssignmentRepository;
  private final ChangeConsumer<RoleAssignmentDBO> roleAssignmentChangeConsumer;
  // Number of threads = Number of Available Cores * (1 + (Wait time / Service time) )
  private final ExecutorService executorService =
      Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

  private long processRoleAssignmentInternal(RoleAssignmentDBO roleAssignmentDBO, RoleDBO updatedRole) {
    long createdCount = 0;
    Set<String> existingPermissions =
        Sets.newHashSet(aclRepository.getDistinctPermissionsInACLsForRoleAssignment(roleAssignmentDBO.getId()));
    if (existingPermissions.isEmpty()) {
      createdCount += roleAssignmentChangeConsumer.consumeCreateEvent(roleAssignmentDBO.getId(), roleAssignmentDBO);
    } else {
      Set<String> permissionsToAdd = Sets.difference(updatedRole.getPermissions(), existingPermissions);
      Set<String> permissionsToDelete = Sets.difference(existingPermissions, updatedRole.getPermissions());
      Set<String> existingResourceSelectors =
          Sets.newHashSet(aclRepository.getDistinctResourceSelectorsInACLs(roleAssignmentDBO.getId()));
      Set<String> existingPrincipals =
          Sets.newHashSet(aclRepository.getDistinctPrincipalsInACLsForRoleAssignment(roleAssignmentDBO.getId()));
      PrincipalType principalType =
          USER_GROUP.equals(roleAssignmentDBO.getPrincipalType()) ? USER : roleAssignmentDBO.getPrincipalType();

      long deletedCount =
          aclRepository.deleteByRoleAssignmentIdAndPermissions(roleAssignmentDBO.getId(), permissionsToDelete);
      log.info("ACLs deleted: {}", deletedCount);

      List<ACL> aclsToCreate = new ArrayList<>();
      existingResourceSelectors.forEach(resourceSelector
          -> permissionsToAdd.forEach(permissionIdentifier
              -> existingPrincipals.forEach(principalIdentifier
                  -> aclsToCreate.add(getACL(permissionIdentifier, Principal.of(principalType, principalIdentifier),
                      roleAssignmentDBO, resourceSelector)))));
      if (!aclsToCreate.isEmpty()) {
        createdCount += aclRepository.insertAllIgnoringDuplicates(aclsToCreate);
        log.info("ACLs created: {}", createdCount);
      }
    }
    return createdCount;
  }

  private long processRoleAssignments(List<RoleAssignmentDBO> roleAssignments, RoleDBO updatedRole) {
    List<CompletableFuture<Long>> futures = new ArrayList<>();
    for (RoleAssignmentDBO roleAssignmentDBO : roleAssignments) {
      futures.add(CompletableFuture.supplyAsync(
          () -> processRoleAssignmentInternal(roleAssignmentDBO, updatedRole), executorService));
    }
    return futures.stream().mapToLong(CompletableFuture::join).sum();
  }

  @Override
  public long consumeUpdateEvent(String id, RoleDBO updatedRole) {
    long createdCount = 0;
    int pageIdx = 0;
    int pageSize = 1000;
    int iterations = 100;
    Page<RoleAssignmentDBO> roleAssignmentsPage;

    do {
      Criteria criteria = Criteria.where(RoleAssignmentDBOKeys.roleIdentifier).is(updatedRole.getIdentifier());
      if (!StringUtils.isEmpty(updatedRole.getScopeIdentifier())) {
        criteria.and(RoleAssignmentDBOKeys.scopeIdentifier).is(updatedRole.getScopeIdentifier());
      }
      roleAssignmentsPage = roleAssignmentRepository.findAll(
          criteria, getPageRequest(PageRequest.builder().pageIndex(pageIdx).pageSize(pageSize).build()));

      createdCount += processRoleAssignments(roleAssignmentsPage.getContent(), updatedRole);

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
  public long consumeCreateEvent(String id, RoleDBO createdEntity) {
    return 0;
  }
}
