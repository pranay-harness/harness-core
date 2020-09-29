package io.harness.cdng.pipeline.executions.repositories;

import com.google.inject.Inject;

import io.harness.cdng.pipeline.executions.beans.PipelineExecutionSummary;
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
public class PipelineExecutionRepositoryImpl implements PipelineExecutionRepositoryCustom {
  private MongoTemplate mongoTemplate;

  public Page<PipelineExecutionSummary> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<PipelineExecutionSummary> pipelineExecutionSummaries =
        mongoTemplate.find(query, PipelineExecutionSummary.class);
    return PageableExecutionUtils.getPage(pipelineExecutionSummaries, pageable,
        () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), PipelineExecutionSummary.class));
  }
}
