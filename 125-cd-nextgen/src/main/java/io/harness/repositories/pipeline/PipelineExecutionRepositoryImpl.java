package io.harness.repositories.pipeline;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummary.PipelineExecutionSummaryKeys;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

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

  @Override
  public void findAndUpdate(String planExecutionId, Update update) {
    Criteria parallelCriteria = where(PipelineExecutionSummaryKeys.planExecutionId).is(planExecutionId);
    mongoTemplate.updateFirst(new Query(parallelCriteria), update, PipelineExecutionSummary.class);
  }
}
