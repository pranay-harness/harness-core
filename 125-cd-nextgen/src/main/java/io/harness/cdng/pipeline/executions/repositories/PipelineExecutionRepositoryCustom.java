package io.harness.cdng.pipeline.executions.repositories;

import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

public interface PipelineExecutionRepositoryCustom {
  Page<PipelineExecutionSummary> findAll(Criteria criteria, Pageable pageable);
  void findAndUpdate(String planExecutionId, Update update);
}
