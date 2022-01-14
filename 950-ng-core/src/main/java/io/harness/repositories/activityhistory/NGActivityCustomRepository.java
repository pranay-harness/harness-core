package io.harness.repositories.activityhistory;

import io.harness.ng.core.activityhistory.entity.NGActivity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;

public interface NGActivityCustomRepository {
  Page<NGActivity> findAll(Criteria criteria, Pageable pageable);
  <T> AggregationResults<T> aggregate(Aggregation aggregation, Class<T> classToFillResultIn);
}
