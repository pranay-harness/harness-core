package io.harness.accesscontrol.roles.persistence.repositories;

import io.harness.accesscontrol.roles.persistence.RoleDBO;
import io.harness.annotation.HarnessRepo;

import java.util.List;
import javax.validation.executable.ValidateOnExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

@HarnessRepo
@ValidateOnExecution
public class RoleCustomRepositoryImpl implements RoleCustomRepository {
  private final MongoTemplate mongoTemplate;

  @Autowired
  public RoleCustomRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Page<RoleDBO> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<RoleDBO> roleDBOS = mongoTemplate.find(query, RoleDBO.class);
    return PageableExecutionUtils.getPage(
        roleDBOS, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), RoleDBO.class));
  }
}
