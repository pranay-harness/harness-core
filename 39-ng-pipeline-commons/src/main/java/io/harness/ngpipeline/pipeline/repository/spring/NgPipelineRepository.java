package io.harness.ngpipeline.pipeline.repository.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.pipeline.repository.custom.NgPipelineRepositoryCustom;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface NgPipelineRepository
    extends PagingAndSortingRepository<NgPipelineEntity, String>, NgPipelineRepositoryCustom {
  Optional<NgPipelineEntity> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier, boolean notDeleted);
}
