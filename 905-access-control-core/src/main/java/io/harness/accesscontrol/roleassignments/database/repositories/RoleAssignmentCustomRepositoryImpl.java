package io.harness.accesscontrol.roleassignments.database.repositories;

import io.harness.accesscontrol.roleassignments.database.RoleAssignment;

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
  public Page<RoleAssignment> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<RoleAssignment> assignments = mongoTemplate.find(query, RoleAssignment.class);
    return PageableExecutionUtils.getPage(
        assignments, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), RoleAssignment.class));
  }
}
