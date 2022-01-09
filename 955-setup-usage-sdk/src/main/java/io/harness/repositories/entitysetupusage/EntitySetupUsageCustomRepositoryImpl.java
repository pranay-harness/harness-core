/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.repositories.entitysetupusage;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;

import com.google.inject.Inject;
import com.mongodb.client.result.DeleteResult;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

@HarnessRepo
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class EntitySetupUsageCustomRepositoryImpl implements EntitySetupUsageCustomRepository {
  private final MongoTemplate mongoTemplate;

  public Page<EntitySetupUsage> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<EntitySetupUsage> connectors = mongoTemplate.find(query, EntitySetupUsage.class);
    return PageableExecutionUtils.getPage(
        connectors, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), EntitySetupUsage.class));
  }

  @Override
  public long countAll(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.count(query, EntitySetupUsage.class);
  }

  @Override
  public Boolean exists(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.exists(query, EntitySetupUsage.class);
  }

  @Override
  public long delete(Criteria criteria) {
    Query query = new Query(criteria);
    DeleteResult removeResult = mongoTemplate.remove(query, EntitySetupUsage.class);
    return removeResult.getDeletedCount();
  }
}
