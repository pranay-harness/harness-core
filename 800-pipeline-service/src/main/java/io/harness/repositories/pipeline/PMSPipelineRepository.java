package io.harness.repositories.pipeline;

import io.harness.annotation.HarnessRepo;
import io.harness.pms.pipeline.PipelineEntity;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface PMSPipelineRepository
    extends PagingAndSortingRepository<PipelineEntity, String>, PMSPipelineRepositoryCustom {
  Optional<PipelineEntity> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier, boolean notDeleted);
}
