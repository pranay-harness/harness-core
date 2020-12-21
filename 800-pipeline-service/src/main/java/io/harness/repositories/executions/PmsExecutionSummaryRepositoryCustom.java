package io.harness.repositories.executions;

import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.entity.PipelineExecutionSummaryEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public interface PmsExecutionSummaryRepositoryCustom {
  PipelineExecutionSummaryEntity update(Query query, Update update);
  Page<PipelineExecutionSummaryEntity> findAll(Criteria criteria, Pageable pageable);
}
