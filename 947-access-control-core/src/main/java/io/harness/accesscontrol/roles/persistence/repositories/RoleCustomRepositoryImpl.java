/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.roles.persistence.repositories;

import io.harness.accesscontrol.roles.persistence.RoleDBO;
import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.mongodb.client.result.UpdateResult;
import java.util.List;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

@OwnedBy(HarnessTeam.PL)
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

  @Override
  public Optional<RoleDBO> find(Criteria criteria) {
    Query query = new Query(criteria);
    return Optional.ofNullable(mongoTemplate.findOne(query, RoleDBO.class));
  }

  @Override
  public UpdateResult updateMulti(Criteria criteria, Update update) {
    Query query = new Query(criteria);
    return mongoTemplate.updateMulti(query, update, RoleDBO.class);
  }

  @Override
  public long deleteMulti(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.remove(query, RoleDBO.class).getDeletedCount();
  }
}
