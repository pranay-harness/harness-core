package io.harness.accesscontrol.roleassignments.persistence.repositories;

import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

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
  public long deleteMulti(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.remove(query, RoleAssignmentDBO.class).getDeletedCount();
  }
}
