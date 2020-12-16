package io.harness.repositories.executions;

import io.harness.pms.pipeline.entity.PipelineExecutionSummaryEntity;

import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public interface PmsExecutionSummaryRepositoryCustom {
  PipelineExecutionSummaryEntity update(Query query, Update update);
}
