package io.harness.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.filter.entity.Filter;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

@HarnessRepo
public class FilterCustomRepositoryImpl implements FilterCustomRepository {
  private final MongoTemplate mongoTemplate;

  @Autowired
  public FilterCustomRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Page<Filter> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<Filter> filters = mongoTemplate.find(query, Filter.class);
    return PageableExecutionUtils.getPage(
        filters, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Filter.class));
  }
}