package io.harness.repositories.filters;

import io.harness.annotation.HarnessRepo;
import io.harness.connector.entities.ConnectorFilter;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

@HarnessRepo
public class ConnectorFilterCustomRepositoryImpl implements ConnectorFilterCustomRepository {
  private final MongoTemplate mongoTemplate;

  @Autowired
  public ConnectorFilterCustomRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Page<ConnectorFilter> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<ConnectorFilter> connectorFilters = mongoTemplate.find(query, ConnectorFilter.class);
    return PageableExecutionUtils.getPage(connectorFilters, pageable,
        () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), ConnectorFilter.class));
  }
}