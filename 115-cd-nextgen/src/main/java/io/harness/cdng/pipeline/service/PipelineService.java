package io.harness.cdng.pipeline.service;
import io.harness.cdng.pipeline.beans.dto.CDPipelineDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface PipelineService {
  String createPipeline(String yaml, String accountId, String orgId, String projectId);
  String updatePipeline(String yaml, String accountId, String orgId, String projectId);
  CDPipelineDTO createPipelineReturnYaml(String yaml, String accountId, String orgId, String projectId);
  CDPipelineDTO getPipeline(String pipelineId, String accountId, String orgId, String projectId);
  Page<CDPipelineDTO> getPipelines(
      String accountId, String orgId, String projectId, Criteria criteria, Pageable pageable);
}
