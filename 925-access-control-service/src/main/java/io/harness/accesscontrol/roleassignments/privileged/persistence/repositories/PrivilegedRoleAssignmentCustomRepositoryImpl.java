/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.roleassignments.privileged.persistence.repositories;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static org.springframework.data.mongodb.util.MongoDbErrorCodes.isDuplicateKeyCode;

import io.harness.accesscontrol.roleassignments.privileged.persistence.PrivilegedRoleAssignmentDBO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mongodb.MongoBulkWriteException;
import java.util.List;
import javax.validation.executable.ValidateOnExecution;
import org.springframework.data.mongodb.BulkOperationException;
import org.springframework.data.mongodb.core.BulkOperations.BulkMode;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.PL)
@ValidateOnExecution
@Singleton
public class PrivilegedRoleAssignmentCustomRepositoryImpl implements PrivilegedRoleAssignmentCustomRepository {
  private final MongoTemplate mongoTemplate;

  @Inject
  public PrivilegedRoleAssignmentCustomRepositoryImpl(@Named("mongoTemplate") MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public long insertAllIgnoringDuplicates(List<PrivilegedRoleAssignmentDBO> assignments) {
    try {
      if (isEmpty(assignments)) {
        return 0;
      }
      return mongoTemplate.bulkOps(BulkMode.UNORDERED, PrivilegedRoleAssignmentDBO.class)
          .insert(assignments)
          .execute()
          .getInsertedCount();
    } catch (BulkOperationException ex) {
      if (ex.getErrors().stream().allMatch(bulkWriteError -> isDuplicateKeyCode(bulkWriteError.getCode()))) {
        return ex.getResult().getInsertedCount();
      }
      throw ex;
    } catch (Exception ex) {
      if (ex.getCause() instanceof MongoBulkWriteException) {
        MongoBulkWriteException bulkWriteException = (MongoBulkWriteException) ex.getCause();
        if (bulkWriteException.getWriteErrors().stream().allMatch(
                writeError -> isDuplicateKeyCode(writeError.getCode()))) {
          return bulkWriteException.getWriteResult().getInsertedCount();
        }
      }
      throw ex;
    }
  }

  @Override
  public List<PrivilegedRoleAssignmentDBO> get(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.find(query, PrivilegedRoleAssignmentDBO.class);
  }

  @Override
  public long remove(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.remove(query, PrivilegedRoleAssignmentDBO.class).getDeletedCount();
  }
}
