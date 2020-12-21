package io.harness.pms.exception.service;

import io.harness.dto.OrchestrationGraphDTO;
import io.harness.pms.pipeline.entity.PipelineExecutionSummaryEntity;
import io.harness.pms.pipeline.resource.PipelineExecutionSummaryDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface PMSExecutionService {
  String getInputsetYaml(String accountId, String orgId, String projectId, String planExecutionId);

  Page<PipelineExecutionSummaryEntity> getPipelineExecutionSummaryEntity(Criteria criteria, Pageable pageable);

  PipelineExecutionSummaryEntity getPipelineExecutionSummaryEntity(
      String accountId, String orgId, String projectId, String planExecutionId);

  OrchestrationGraphDTO getOrchestrationGraph(String stageIdentifier, String planExecutionId);
}
