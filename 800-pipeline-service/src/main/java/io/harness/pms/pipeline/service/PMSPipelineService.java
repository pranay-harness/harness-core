package io.harness.pms.pipeline.service;

import io.harness.dto.OrchestrationGraphDTO;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.StepCategory;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface PMSPipelineService {
  PipelineEntity create(PipelineEntity pipelineEntity);

  Optional<PipelineEntity> get(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier, boolean deleted);

  PipelineEntity update(PipelineEntity pipelineEntity);

  boolean delete(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, Long version);

  Page<PipelineEntity> list(Criteria criteria, Pageable pageable);

  StepCategory getSteps(String module, String category);
}
