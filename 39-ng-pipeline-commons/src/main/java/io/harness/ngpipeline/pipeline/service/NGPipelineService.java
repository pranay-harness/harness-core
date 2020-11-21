package io.harness.ngpipeline.pipeline.service;

import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface NGPipelineService {
  NgPipelineEntity create(NgPipelineEntity ngPipeline);

  Optional<NgPipelineEntity> get(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier, boolean deleted);

  NgPipelineEntity update(NgPipelineEntity ngPipeline);

  Page<NgPipelineEntity> list(Criteria criteria, Pageable pageable);

  boolean delete(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, Long version);

  Map<String, String> getPipelineIdentifierToName(
      String accountId, String orgId, String projectId, @NotNull List<String> pipelineIdentifiers);
  NgPipelineEntity getPipeline(String uuid);

  NgPipelineEntity getPipeline(String pipelineId, String accountId, String orgId, String projectId);

  Page<NgPipelineEntity> listPipelines(
      String accountId, String orgId, String projectId, Criteria criteria, Pageable pageable, String searchTerm);
}
