package io.harness.connector.repositories.base;

import io.harness.annotation.HarnessRepo;
import io.harness.connector.entities.Connector;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

@HarnessRepo
public class ConnectorCustomRepositoryImpl implements ConnectorCustomRepository {
  private final MongoTemplate mongoTemplate;

  @Autowired
  public ConnectorCustomRepositoryImpl(@Qualifier("connectorMongoTemplate") MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Page<Connector> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<Connector> connectors = mongoTemplate.find(query, Connector.class);
    return PageableExecutionUtils.getPage(
        connectors, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Connector.class));
  }
}
