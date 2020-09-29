package io.harness.ngpipeline.repository;

import io.harness.annotation.HarnessRepo;
import io.harness.cdng.pipeline.beans.entities.CDPipelineEntity;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;

import java.util.Optional;

@HarnessRepo
public interface PipelineRepository extends PagingAndSortingRepository<CDPipelineEntity, String>,
                                            Repository<CDPipelineEntity, String>, CustomPipelineRepository {
  <S extends CDPipelineEntity> S save(S cdPipelineEntity);
  Optional<CDPipelineEntity> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier, boolean notDeleted);
}
