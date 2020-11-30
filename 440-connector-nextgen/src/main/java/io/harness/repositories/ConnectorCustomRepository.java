package io.harness.repositories;

import io.harness.connector.entities.Connector;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public interface ConnectorCustomRepository {
  Page<Connector> findAll(Criteria criteria, Pageable pageable);
  Connector update(Query query, Update update);
  <T> AggregationResults<T> aggregate(Aggregation aggregation, Class<T> classToFillResultIn);
}
