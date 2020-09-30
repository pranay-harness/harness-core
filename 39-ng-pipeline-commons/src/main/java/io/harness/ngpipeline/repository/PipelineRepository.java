package io.harness.ngpipeline.repository;

import io.harness.annotation.HarnessRepo;
import io.harness.cdng.pipeline.beans.entities.NgPipelineEntity;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;

import java.util.Optional;

@HarnessRepo
public interface PipelineRepository extends PagingAndSortingRepository<NgPipelineEntity, String>,
                                            Repository<NgPipelineEntity, String>, CustomPipelineRepository {
  <S extends NgPipelineEntity> S save(S pipelineEntity);
  Optional<NgPipelineEntity> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier, boolean notDeleted);
}
