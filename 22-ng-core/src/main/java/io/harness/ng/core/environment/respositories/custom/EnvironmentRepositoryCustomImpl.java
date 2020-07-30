package io.harness.ng.core.environment.respositories.custom;

import com.google.inject.Inject;

import com.mongodb.client.result.UpdateResult;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.mappers.EnvironmentFilterHelper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class EnvironmentRepositoryCustomImpl implements EnvironmentRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public Page<Environment> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<Environment> projects = mongoTemplate.find(query, Environment.class);
    return PageableExecutionUtils.getPage(
        projects, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Environment.class));
  }

  @Override
  public UpdateResult upsert(Criteria criteria, Environment environment) {
    Query query = new Query(criteria);
    return mongoTemplate.upsert(query, EnvironmentFilterHelper.getUpdateOperations(environment), Environment.class);
  }

  @Override
  public UpdateResult update(Criteria criteria, Environment environment) {
    Query query = new Query(criteria);
    return mongoTemplate.updateFirst(
        query, EnvironmentFilterHelper.getUpdateOperations(environment), Environment.class);
  }
}
