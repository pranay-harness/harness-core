package io.harness.app.dao.repositories;

import com.google.inject.Inject;

import io.harness.beans.CIPipeline;
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
public class CIPipelineRepositoryCustomImpl implements CIPipelineRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public Page<CIPipeline> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<CIPipeline> projects = mongoTemplate.find(query, CIPipeline.class);
    return PageableExecutionUtils.getPage(
        projects, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), CIPipeline.class));
  }

  @Override
  public List<CIPipeline> findAllWithCriteria(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.find(query, CIPipeline.class);
  }
}
