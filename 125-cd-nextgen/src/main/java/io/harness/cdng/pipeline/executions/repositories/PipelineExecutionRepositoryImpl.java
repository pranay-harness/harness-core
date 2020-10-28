package io.harness.cdng.pipeline.executions.repositories;

import static io.harness.ngpipeline.pipeline.executions.beans.CDStageExecutionSummary.CDStageExecutionSummaryKeys;
import static io.harness.ngpipeline.pipeline.executions.beans.ParallelStageExecutionSummary.ParallelStageExecutionSummaryKeys;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import com.google.inject.Inject;

import io.harness.cdng.pipeline.executions.PipelineExecutionHelper;
import io.harness.ngpipeline.pipeline.executions.beans.CDStageExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummary.PipelineExecutionSummaryKeys;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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

  @Override
  public void findAndUpdate(String planExecutionId, CDStageExecutionSummary cdStageExecutionSummary,
      PipelineExecutionHelper.StageIndex stageIndex) {
    Criteria parallelCriteria = where(PipelineExecutionSummaryKeys.planExecutionId).is(planExecutionId);

    String key = stageIndex.getSecondLevelIndex() != -1
        ? String.format("%s.%s.%s.%s", PipelineExecutionSummaryKeys.stageExecutionSummarySummaryElements,
              stageIndex.getFirstLevelIndex(), ParallelStageExecutionSummaryKeys.stageExecutionSummaries,
              stageIndex.getSecondLevelIndex())
        : String.format("%s.%s", PipelineExecutionSummaryKeys.stageExecutionSummarySummaryElements,
              stageIndex.getFirstLevelIndex());

    mongoTemplate.updateFirst(new Query(parallelCriteria),
        new Update()
            .set(key + "." + CDStageExecutionSummaryKeys.executionStatus, cdStageExecutionSummary.getExecutionStatus())
            .set(key + "." + CDStageExecutionSummaryKeys.endedAt, cdStageExecutionSummary.getEndedAt())
            .set(key + "." + CDStageExecutionSummaryKeys.startedAt, cdStageExecutionSummary.getStartedAt())
            .set(key + "." + CDStageExecutionSummaryKeys.errorInfo, cdStageExecutionSummary.getErrorInfo()),

        PipelineExecutionSummary.class);
  }
}
