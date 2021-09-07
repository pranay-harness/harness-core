package io.harness.accesscontrol.roleassignments.persistence.repositories;

import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO.RoleAssignmentDBOKeys;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.mongodb.client.result.UpdateResult;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class RoleAssignmentCustomRepositoryImpl implements RoleAssignmentCustomRepository {
  private final MongoTemplate mongoTemplate;
  @Autowired
  public RoleAssignmentCustomRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Page<RoleAssignmentDBO> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<RoleAssignmentDBO> assignments = mongoTemplate.find(query, RoleAssignmentDBO.class);
    return PageableExecutionUtils.getPage(
        assignments, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), RoleAssignmentDBO.class));
  }

  @Override
  public boolean updateById(String id, Update updateOperation) {
    Criteria criteria = Criteria.where(RoleAssignmentDBOKeys.id).is(id);
    Query query = new Query(criteria);
    UpdateResult updateResult = mongoTemplate.updateFirst(query, updateOperation, RoleAssignmentDBO.class);
    return updateResult.getModifiedCount() == updateResult.getMatchedCount();
  }

  @Override
  public long deleteMulti(Criteria criteria) {
    Query query = new Query(criteria);
    log.info("The current query for deleting multiple role assignment is: {}", query.toString());
    return mongoTemplate.remove(query, RoleAssignmentDBO.class).getDeletedCount();
  }
}
