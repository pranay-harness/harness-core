package io.harness.gitsync.core.dao.api.repositories.YamlChangeSet;

import com.google.inject.Inject;

import com.mongodb.client.result.UpdateResult;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.beans.YamlChangeSet.YamlChangeSetKeys;
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

import java.util.List;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class YamlChangeSetRepositoryCustomImpl implements YamlChangeSetRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public UpdateResult updateYamlChangeSetStatus(YamlChangeSet.Status status, String yamlChangeSetId) {
    Query query = new Query().addCriteria(Criteria.where(YamlChangeSetKeys.uuid).is(yamlChangeSetId));
    Update update = new Update().set(YamlChangeSetKeys.status, status);
    return mongoTemplate.updateFirst(query, update, YamlChangeSet.class);
  }

  @Override
  public UpdateResult updateYamlChangeSetsStatus(
      YamlChangeSet.Status oldStatus, YamlChangeSet.Status newStatus, String accountId) {
    Query query = new Query().addCriteria(
        new Criteria().and(YamlChangeSetKeys.accountId).is(accountId).and(YamlChangeSetKeys.status).is(oldStatus));
    Update update = new Update().set(YamlChangeSetKeys.status, newStatus);
    return mongoTemplate.updateMulti(query, update, YamlChangeSet.class);
  }

  @Override
  public UpdateResult updateYamlChangeSetsToNewStatusWithMessageCodeAndCreatedAtLessThan(
      YamlChangeSet.Status oldStatus, YamlChangeSet.Status newStatus, long cutOffCreatedAt, String message) {
    Query query = new Query().addCriteria(new Criteria()
                                              .and(YamlChangeSetKeys.status)
                                              .is(oldStatus)
                                              .and(YamlChangeSetKeys.createdAt)
                                              .lt(cutOffCreatedAt));
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
}
