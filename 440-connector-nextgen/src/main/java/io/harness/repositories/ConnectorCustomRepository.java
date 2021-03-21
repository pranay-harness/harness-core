package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.Connector;

import com.mongodb.client.result.UpdateResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(DX)
public interface ConnectorCustomRepository {
  Page<Connector> findAll(Criteria criteria, Pageable pageable);
  Connector update(Query query, Update update);
  UpdateResult updateMultiple(Query query, Update update);
  <T> AggregationResults<T> aggregate(Aggregation aggregation, Class<T> classToFillResultIn);
}
