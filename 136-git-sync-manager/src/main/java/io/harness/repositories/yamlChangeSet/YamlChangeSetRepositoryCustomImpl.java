/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.yamlChangeSet;

import static io.harness.annotations.dev.HarnessTeam.DX;

import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.beans.YamlChangeSet.YamlChangeSetKeys;
import io.harness.gitsync.common.beans.YamlChangeSetStatus;

import com.google.inject.Inject;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class YamlChangeSetRepositoryCustomImpl implements YamlChangeSetRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public UpdateResult updateYamlChangeSetStatus(YamlChangeSetStatus status, String yamlChangeSetId) {
    Query query = new Query().addCriteria(Criteria.where(YamlChangeSetKeys.uuid).is(yamlChangeSetId));
    Update update = new Update().set(YamlChangeSetKeys.status, status);
    return mongoTemplate.updateFirst(query, update, YamlChangeSet.class);
  }

  @Override
  public UpdateResult updateYamlChangeSetStatusAndCutoffTime(
      YamlChangeSetStatus status, String yamlChangeSetId, long cutOffTime) {
    Query query = new Query().addCriteria(Criteria.where(YamlChangeSetKeys.uuid).is(yamlChangeSetId));
    Update update = new Update().set(YamlChangeSetKeys.status, status);
    update.set(YamlChangeSetKeys.cutOffTime, cutOffTime);
    return mongoTemplate.updateFirst(query, update, YamlChangeSet.class);
  }

  @Override
  public UpdateResult updateYamlChangeSetsStatus(
      YamlChangeSetStatus oldStatus, YamlChangeSetStatus newStatus, String accountId) {
    Query query = new Query().addCriteria(
        new Criteria().and(YamlChangeSetKeys.accountId).is(accountId).and(YamlChangeSetKeys.status).is(oldStatus));
    Update update = new Update().set(YamlChangeSetKeys.status, newStatus);
    return mongoTemplate.updateMulti(query, update, YamlChangeSet.class);
  }

  @Override
  public UpdateResult updateYamlChangeSetsToNewStatusWithMessageCodeAndQueuedAtLessThan(
      YamlChangeSetStatus oldStatus, YamlChangeSetStatus newStatus, long cutOffCreatedAt, String message) {
    Query query = new Query().addCriteria(
        new Criteria().and(YamlChangeSetKeys.status).is(oldStatus).and(YamlChangeSetKeys.queuedOn).lt(cutOffCreatedAt));
    Update update = new Update().set(YamlChangeSetKeys.status, newStatus).set(YamlChangeSetKeys.messageCode, message);
    return mongoTemplate.updateMulti(query, update, YamlChangeSet.class);
  }

  @Override
  public UpdateResult update(Query query, Update update) {
    return mongoTemplate.updateMulti(query, update, YamlChangeSet.class);
  }

  @Override
  public Page<YamlChangeSet> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);

    int finalPageSize = pageable.getPageSize();
    List<YamlChangeSet> yamlChangeSets = mongoTemplate.find(query, YamlChangeSet.class);
    return PageableExecutionUtils.getPage(yamlChangeSets, pageable,
        () -> mongoTemplate.count(Query.of(query).limit(finalPageSize).skip(-1), YamlChangeSet.class));
  }

  @Override
  public <C> AggregationResults aggregate(Aggregation aggregation, Class<C> castClass) {
    return mongoTemplate.aggregate(aggregation, YamlChangeSet.class, castClass);
  }

  @Override
  public List<String> findDistinctAccountIdByStatusIn(List<YamlChangeSetStatus> status) {
    Criteria criteria = Criteria.where(YamlChangeSetKeys.status).in(status);
    Query query = query(criteria);
    return mongoTemplate.findDistinct(query, YamlChangeSetKeys.accountId, YamlChangeSet.class, String.class);
  }
}
