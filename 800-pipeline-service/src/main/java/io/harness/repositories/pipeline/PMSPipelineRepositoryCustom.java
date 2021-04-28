package io.harness.repositories.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.pipeline.PipelineConfig;
import io.harness.pms.pipeline.PipelineEntity;

import com.mongodb.client.result.UpdateResult;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(PIPELINE)
public interface PMSPipelineRepositoryCustom {
  PipelineEntity update(Criteria criteria, PipelineEntity pipelineEntity);

  PipelineEntity update(Criteria criteria, Update update);

  UpdateResult delete(Criteria criteria);

  Page<PipelineEntity> findAll(Criteria criteria, Pageable pageable);

  Optional<PipelineEntity> incrementRunSequence(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, boolean deleted);

  PipelineEntity save(PipelineEntity pipelineToSave, PipelineConfig yamlDTO);

  Optional<PipelineEntity> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, boolean notDeleted);
}
