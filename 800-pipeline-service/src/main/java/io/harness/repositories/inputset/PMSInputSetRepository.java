package io.harness.repositories.inputset;

import io.harness.annotation.HarnessRepo;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface PMSInputSetRepository
    extends PagingAndSortingRepository<InputSetEntity, String>, PMSInputSetRepositoryCustom {
  Optional<InputSetEntity>
  findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPipelineIdentifierAndIdentifierAndDeletedNot(String accountId,
      String orgIdentifier, String projectIdentifier, String pipelineIdentifier, String identifier, boolean notDeleted);
}
