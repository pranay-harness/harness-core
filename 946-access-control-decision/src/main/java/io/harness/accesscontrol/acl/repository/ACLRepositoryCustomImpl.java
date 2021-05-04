package io.harness.accesscontrol.acl.repository;

import static io.harness.accesscontrol.acl.models.ACL.RESOURCE_GROUP_IDENTIFIER_KEY;
import static io.harness.accesscontrol.acl.models.ACL.ROLE_IDENTIFIER_KEY;
import static io.harness.accesscontrol.acl.models.ACL.USER_GROUP_IDENTIFIER_KEY;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.models.ACL.ACLKeys;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.BulkOperationException;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(PL)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class ACLRepositoryCustomImpl implements ACLRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  private boolean isDuplicateKeyException(BulkOperationException ex) {
    int duplicateKeyErrorCode = 11000;
    return ex.getErrors().stream().anyMatch(bwe -> bwe.getCode() == duplicateKeyErrorCode);
  }

  public long insertAllIgnoringDuplicates(List<ACL> acls) {
    try {
      return mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ACL.class)
          .insert(acls)
          .execute()
          .getInsertedCount();
    } catch (DuplicateKeyException duplicateKeyException) {
      return 0;
    } catch (BulkOperationException bulkOperationException) {
      if (!isDuplicateKeyException(bulkOperationException)) {
        throw bulkOperationException;
      }
      return 0;
    }
  }

  @Override
  public List<ACL> findByUserGroup(String scopeIdentifier, String userGroupIdentifier) {
    Criteria criteria = Criteria.where(USER_GROUP_IDENTIFIER_KEY).is(userGroupIdentifier);
    criteria.and(ACLKeys.scopeIdentifier).is(scopeIdentifier);
    return mongoTemplate.find(new Query(criteria), ACL.class);
  }

  @Override
  public List<ACL> findByRole(String scopeIdentifier, String identifier, boolean managed) {
    Criteria criteria = Criteria.where(ROLE_IDENTIFIER_KEY).is(identifier);
    if (!managed) {
      criteria.and(ACLKeys.scopeIdentifier).is(scopeIdentifier);
    }
    return mongoTemplate.find(new Query(criteria), ACL.class);
  }

  @Override
  public List<ACL> findByResourceGroup(String scopeIdentifier, String identifier, boolean managed) {
    Criteria criteria = Criteria.where(RESOURCE_GROUP_IDENTIFIER_KEY).is(identifier);
    if (!managed) {
      criteria.and(ACLKeys.scopeIdentifier).is(scopeIdentifier);
    }
    return mongoTemplate.find(new Query(criteria), ACL.class);
  }

  @Override
  public List<ACL> getByRoleAssignmentId(String id) {
    return mongoTemplate.find(new Query(Criteria.where(ACLKeys.roleAssignmentId).is(id)), ACL.class);
  }

  @Override
  public long deleteByRoleAssignmentId(String id) {
    return mongoTemplate.remove(new Query(Criteria.where(ACLKeys.roleAssignmentId).is(id)), ACL.class)
        .getDeletedCount();
  }

  @Override
  public List<String> getDistinctResourceSelectorsInACLs(String roleAssignmentId) {
    Criteria criteria = Criteria.where(ACLKeys.roleAssignmentId).is(roleAssignmentId);
    Query query = new Query();
    query.addCriteria(criteria);
    return mongoTemplate.findDistinct(query, ACLKeys.resourceSelector, ACL.class, String.class);
  }

  @Override
  public long deleteByRoleAssignmentIdAndResourceSelectors(
      String roleAssignmentId, Set<String> resourceSelectorsToDelete) {
    return mongoTemplate
        .remove(new Query(Criteria.where(ACLKeys.roleAssignmentId)
                              .is(roleAssignmentId)
                              .and(ACLKeys.resourceSelector)
                              .in(resourceSelectorsToDelete)),
            ACL.class)
        .getDeletedCount();
  }

  @Override
  public long deleteByRoleAssignmentIdAndPermissions(String roleAssignmentId, Set<String> permissions) {
    return mongoTemplate
        .remove(new Query(Criteria.where(ACLKeys.roleAssignmentId)
                              .is(roleAssignmentId)
                              .and(ACLKeys.permissionIdentifier)
                              .in(permissions)),
            ACL.class)
        .getDeletedCount();
  }

  @Override
  public long deleteByRoleAssignmentIdAndPrincipals(String roleAssignmentId, Set<String> principals) {
    return mongoTemplate
        .remove(new Query(Criteria.where(ACLKeys.roleAssignmentId)
                              .is(roleAssignmentId)
                              .and(ACLKeys.principalIdentifier)
                              .in(principals)),
            ACL.class)
        .getDeletedCount();
  }

  @Override
  public List<String> getDistinctPermissionsInACLsForRoleAssignment(String roleAssignmentId) {
    Criteria criteria = Criteria.where(ACLKeys.roleAssignmentId).is(roleAssignmentId);
    Query query = new Query();
    query.addCriteria(criteria);
    return mongoTemplate.findDistinct(query, ACLKeys.permissionIdentifier, ACL.class, String.class);
  }

  @Override
  public List<String> getDistinctPrincipalsInACLsForRoleAssignment(String roleAssignmentId) {
    Criteria criteria = Criteria.where(ACLKeys.roleAssignmentId).is(roleAssignmentId);
    Query query = new Query();
    query.addCriteria(criteria);
    return mongoTemplate.findDistinct(query, ACLKeys.principalIdentifier, ACL.class, String.class);
  }
}
