package io.harness.repositories.inputset.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.ToBeDeleted;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;
import io.harness.repositories.inputset.custom.InputSetRepositoryCustom;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.repository.PagingAndSortingRepository;

@ToBeDeleted
@Deprecated
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
