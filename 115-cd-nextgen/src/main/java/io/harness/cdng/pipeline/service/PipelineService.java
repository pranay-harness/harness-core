package io.harness.cdng.pipeline.service;

import io.harness.cdng.pipeline.beans.dto.CDPipelineResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Optional;

public interface PipelineService {
  String createPipeline(String yaml, String accountId, String orgId, String projectId);
  String updatePipeline(String yaml, String accountId, String orgId, String projectId, String pipelineId);
  Optional<CDPipelineResponseDTO> getPipeline(String pipelineId, String accountId, String orgId, String projectId);
  Page<CDPipelineResponseDTO> getPipelines(
      String accountId, String orgId, String projectId, Criteria criteria, Pageable pageable);
}
