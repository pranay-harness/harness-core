package io.harness.ngpipeline.inputset.repository.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ngpipeline.inputset.repository.custom.InputSetRepositoryCustom;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@HarnessRepo
public interface InputSetRepository
    extends PagingAndSortingRepository<BaseInputSetEntity, String>, InputSetRepositoryCustom {
  Optional<BaseInputSetEntity>
  findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPipelineIdentifierAndIdentifierAndDeletedNot(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineIdentifier,
      String inputSetIdentifier, boolean notDeleted);

  List<BaseInputSetEntity>
  findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPipelineIdentifierAndDeletedNotAndIdentifierIn(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineIdentifier,
      boolean notDeleted, Set<String> inputSetIdentifiersList);
}
