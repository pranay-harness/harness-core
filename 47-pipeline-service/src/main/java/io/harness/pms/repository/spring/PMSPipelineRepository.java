package io.harness.pms.repository.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.pms.beans.entities.PipelineEntity;
import io.harness.pms.repository.custom.PMSPipelineRepositoryCustom;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface PMSPipelineRepository
    extends PagingAndSortingRepository<PipelineEntity, String>, PMSPipelineRepositoryCustom {
  Optional<PipelineEntity> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier, boolean notDeleted);
}
