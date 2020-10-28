package io.harness.cdng.pipeline.executions.repositories;

import io.harness.cdng.pipeline.executions.PipelineExecutionHelper;
import io.harness.ngpipeline.pipeline.executions.beans.CDStageExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface PipelineExecutionRepositoryCustom {
  Page<PipelineExecutionSummary> findAll(Criteria criteria, Pageable pageable);
  void findAndUpdate(String planExecutionId, CDStageExecutionSummary cdStageExecutionSummary,
      PipelineExecutionHelper.StageIndex stageIndex);
}
