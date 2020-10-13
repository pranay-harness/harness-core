package io.harness.cdng.pipeline.service;

import io.harness.cdng.pipeline.beans.dto.NGPipelineResponseDTO;
import io.harness.cdng.pipeline.beans.dto.NGPipelineSummaryResponseDTO;
import io.harness.cdng.pipeline.beans.entities.NgPipelineEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.NotNull;

public interface NGPipelineService {
  String createPipeline(String yaml, String accountId, String orgId, String projectId);
  String updatePipeline(String yaml, String accountId, String orgId, String projectId, String pipelineId);
  Optional<NGPipelineResponseDTO> getPipelineResponseDTO(
      String pipelineId, String accountId, String orgId, String projectId);
  Page<NGPipelineSummaryResponseDTO> getPipelinesSummary(
      String accountId, String orgId, String projectId, Criteria criteria, Pageable pageable, String searchTerm);
  boolean deletePipeline(String accountId, String orgId, String projectId, String pipelineId);
  Map<String, String> getPipelineIdentifierToName(
      String accountId, String orgId, String projectId, @NotNull List<String> pipelineIdentifiers);
  NgPipelineEntity getPipeline(String uuid);
  NgPipelineEntity getPipeline(String pipelineId, String accountId, String orgId, String projectId);
  Page<NgPipelineEntity> listPipelines(
      String accountId, String orgId, String projectId, Criteria criteria, Pageable pageable, String searchTerm);
}
