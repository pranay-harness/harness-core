/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.permissions.persistence.repositories;

import io.harness.accesscontrol.permissions.persistence.PermissionDBO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.PL)
public class PermissionCustomRepositoryImpl implements PermissionCustomRepository {
  private final MongoTemplate mongoTemplate;

  @Autowired
  public PermissionCustomRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public List<PermissionDBO> findAll(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.find(query, PermissionDBO.class);
  }
}
