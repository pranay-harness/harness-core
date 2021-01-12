package io.harness.pms.plan.execution.service;

import io.harness.dto.OrchestrationGraphDTO;
import io.harness.pms.plan.execution.PlanExecutionInterruptType;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.plan.execution.beans.dto.InterruptDTO;
import io.harness.pms.plan.execution.beans.dto.PipelineExecutionFilterPropertiesDTO;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface PMSExecutionService {
  String getInputsetYaml(String accountId, String orgId, String projectId, String planExecutionId);

  Page<PipelineExecutionSummaryEntity> getPipelineExecutionSummaryEntity(Criteria criteria, Pageable pageable);

  PipelineExecutionSummaryEntity getPipelineExecutionSummaryEntity(
      String accountId, String orgId, String projectId, String planExecutionId);

  OrchestrationGraphDTO getOrchestrationGraph(String stageNodeId, String planExecutionId);

  InterruptDTO registerInterrupt(PlanExecutionInterruptType executionInterruptType, String planExecutionId);

  Criteria formCriteria(String accountId, String orgId, String projectId, String pipelineIdentifier,
      String filterIdentifier, PipelineExecutionFilterPropertiesDTO filterProperties);
}
