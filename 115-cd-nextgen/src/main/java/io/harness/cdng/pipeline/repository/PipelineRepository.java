package io.harness.cdng.pipeline.repository;

import io.harness.annotation.HarnessRepo;
import io.harness.cdng.pipeline.beans.entities.CDPipelineEntity;
import org.springframework.data.repository.Repository;

import java.util.Optional;

@HarnessRepo
public interface PipelineRepository extends Repository<CDPipelineEntity, String>, CustomPipelineRepository {
  <S extends CDPipelineEntity> S save(S cdPipelineEntity);
  Optional<CDPipelineEntity> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier, boolean notDeleted);
}
