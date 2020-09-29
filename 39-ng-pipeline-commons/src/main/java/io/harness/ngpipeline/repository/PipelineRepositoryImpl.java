package io.harness.ngpipeline.repository;

import com.google.inject.Inject;

import io.harness.cdng.pipeline.beans.entities.CDPipelineEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class PipelineRepositoryImpl implements CustomPipelineRepository {
  MongoTemplate mongoTemplate;

  // Created Dummy method for examples
  @Override
  public Optional<CDPipelineEntity> getPipelineByIdExample(String accountId, String pipelineId) {
    Criteria criteria = new Criteria()
                            .and(CDPipelineEntity.PipelineNGKeys.accountId)
                            .is(accountId)
                            .and(CDPipelineEntity.PipelineNGKeys.identifier)
                            .is(pipelineId);
    Query query = new Query(criteria);
    return Optional.ofNullable(mongoTemplate.findOne(query, CDPipelineEntity.class));
  }

  @Override
  public Page<CDPipelineEntity> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<CDPipelineEntity> pipelineEntities = mongoTemplate.find(query, CDPipelineEntity.class);
    return PageableExecutionUtils.getPage(pipelineEntities, pageable,
        () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), CDPipelineEntity.class));
  }

  @Override
  public List<CDPipelineEntity> findAllWithCriteria(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.find(query, CDPipelineEntity.class);
  }
}
