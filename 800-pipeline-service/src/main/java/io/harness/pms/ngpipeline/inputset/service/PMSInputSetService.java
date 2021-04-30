package io.harness.pms.ngpipeline.inputset.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.pipeline.PipelineEntity;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PIPELINE)
public interface PMSInputSetService {
  InputSetEntity create(InputSetEntity inputSetEntity);

  Optional<InputSetEntity> get(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String identifier, boolean deleted);

  InputSetEntity update(InputSetEntity inputSetEntity);

  boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier,
      String identifier, Long version);

  Page<InputSetEntity> list(Criteria criteria, Pageable pageable);

  void deleteInputSetsOnPipelineDeletion(PipelineEntity pipelineEntity);
}
