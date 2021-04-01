package io.harness.accesscontrol.roleassignments.persistence.repositories;

import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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
  public long deleteMulti(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.remove(query, RoleAssignmentDBO.class).getDeletedCount();
  }

  @Override
  public List<RoleAssignmentDBO> insertAllIgnoringDuplicates(List<RoleAssignmentDBO> roleAssignmentDBOList) {
    List<RoleAssignmentDBO> upsertedList = new ArrayList<>();
    for (RoleAssignmentDBO roleAssignmentDBO : roleAssignmentDBOList) {
      try {
        mongoTemplate.insert(roleAssignmentDBO);
        upsertedList.add(roleAssignmentDBO);
      } catch (DuplicateKeyException duplicateKeyException) {
        // ignore duplicates
      } catch (Exception exception) {
        log.error("Could not create role assignment due to unexpected error", exception);
      }
    }
    return upsertedList;
  }
}
