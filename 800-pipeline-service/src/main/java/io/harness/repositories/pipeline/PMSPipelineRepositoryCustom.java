package io.harness.repositories.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.pipeline.PipelineConfig;
import io.harness.pms.pipeline.PipelineEntity;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(PIPELINE)
public interface PMSPipelineRepositoryCustom {
  Page<PipelineEntity> findAll(
      Criteria criteria, Pageable pageable, String accountIdentifier, String orgIdentifier, String projectIdentifier);

  PipelineEntity save(PipelineEntity pipelineToSave, PipelineConfig yamlDTO);

  Optional<PipelineEntity> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, boolean notDeleted);

  PipelineEntity updatePipelineYaml(PipelineEntity pipelineToUpdate, PipelineConfig yamlDTO);

  PipelineEntity updatePipelineMetadata(
      String accountId, String orgIdentifier, String projectIdentifier, Criteria criteria, Update update);

  PipelineEntity deletePipeline(PipelineEntity pipelineToUpdate, PipelineConfig yamlDTO);
}
