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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}
