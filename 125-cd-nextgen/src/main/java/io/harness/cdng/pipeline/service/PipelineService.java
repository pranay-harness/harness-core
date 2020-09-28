package io.harness.cdng.pipeline.service;

import io.harness.beans.ExecutionStrategyType;
import io.harness.cdng.pipeline.StepCategory;
import io.harness.cdng.pipeline.beans.CDPipelineValidationInfo;
import io.harness.cdng.pipeline.beans.dto.CDPipelineResponseDTO;
import io.harness.cdng.pipeline.beans.dto.CDPipelineSummaryResponseDTO;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PipelineService {
  String createPipeline(String yaml, String accountId, String orgId, String projectId);
  String updatePipeline(String yaml, String accountId, String orgId, String projectId, String pipelineId);
  Optional<CDPipelineResponseDTO> getPipeline(String pipelineId, String accountId, String orgId, String projectId);
  Page<CDPipelineSummaryResponseDTO> getPipelines(
      String accountId, String orgId, String projectId, Criteria criteria, Pageable pageable, String searchTerm);
  Map<ServiceDefinitionType, List<ExecutionStrategyType>> getExecutionStrategyList();
  String getExecutionStrategyYaml(
      ServiceDefinitionType serviceDefinitionType, ExecutionStrategyType executionStrategyType) throws IOException;
  List<ServiceDefinitionType> getServiceDefinitionTypes();
  StepCategory getSteps(ServiceDefinitionType serviceDefinitionType);
  boolean deletePipeline(String accountId, String orgId, String projectId, String pipelineId);
  Optional<CDPipelineValidationInfo> validatePipeline(
      String pipelineId, String accountId, String orgId, String projectId);
}
