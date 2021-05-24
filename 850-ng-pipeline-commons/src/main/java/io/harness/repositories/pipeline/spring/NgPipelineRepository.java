package io.harness.repositories.pipeline.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.ToBeDeleted;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.repositories.pipeline.custom.NgPipelineRepositoryCustom;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@ToBeDeleted
@Deprecated
@HarnessRepo
public interface NgPipelineRepository
    extends PagingAndSortingRepository<NgPipelineEntity, String>, NgPipelineRepositoryCustom {
  Optional<NgPipelineEntity> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier, boolean notDeleted);
}
