package io.harness.cdng.inputset.repository.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.cdng.inputset.repository.custom.InputSetRepositoryCustom;
import io.harness.ngpipeline.BaseInputSetEntity;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@HarnessRepo
public interface InputSetRepository
    extends PagingAndSortingRepository<BaseInputSetEntity, String>, InputSetRepositoryCustom {
  Optional<BaseInputSetEntity>
  findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPipelineIdentifierAndIdentifierAndDeletedNot(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineIdentifier,
      String inputSetIdentifier, boolean notDeleted);
}
