package io.harness.repositories;

import static io.harness.connector.entities.Connector.CONNECTOR_COLLECTION_NAME;

import io.harness.annotation.HarnessRepo;
import io.harness.connector.entities.Connector;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

@HarnessRepo
public class ConnectorCustomRepositoryImpl implements ConnectorCustomRepository {
  private final MongoTemplate mongoTemplate;

  @Autowired
  public ConnectorCustomRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Page<Connector> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<Connector> connectors = mongoTemplate.find(query, Connector.class);
    return PageableExecutionUtils.getPage(
        connectors, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Connector.class));
  }

  @Override
  public Connector update(Query query, Update update) {
    return mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true), Connector.class);
  }

  @Override
  public <T> AggregationResults<T> aggregate(Aggregation aggregation, Class<T> classToFillResultIn) {
    return mongoTemplate.aggregate(aggregation, CONNECTOR_COLLECTION_NAME, classToFillResultIn);
  }
}
