/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.filter.entity.Filter;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

@HarnessRepo
@OwnedBy(DX)
public class FilterCustomRepositoryImpl implements FilterCustomRepository {
  private final MongoTemplate mongoTemplate;

  @Autowired
  public FilterCustomRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Page<Filter> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    query.collation(Collation.of("en").strength(Collation.ComparisonLevel.secondary()));
    List<Filter> filters = mongoTemplate.find(query, Filter.class);
    return PageableExecutionUtils.getPage(
        filters, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Filter.class));
  }
}
